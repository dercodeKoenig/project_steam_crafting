package ProjectSteamCrafting.WoodMill;

import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketBlockEntity;
import ARLib.utils.ItemUtils;
import ProjectSteam.Blocks.Mechanics.CrankShaft.BlockCrankShaftBase;
import ProjectSteam.Blocks.Mechanics.CrankShaft.EntityCrankShaftBase;
import ProjectSteam.Blocks.Mechanics.CrankShaft.ICrankShaftConnector;
import ProjectSteam.Core.AbstractMechanicalBlock;
import ProjectSteam.Static;
import ProjectSteamCrafting.Sieve.BlockSieve;
import ProjectSteamCrafting.Sieve.IMesh;
import ProjectSteamCrafting.Sieve.Items.ItemSieveUpgrade;
import ProjectSteamCrafting.Sieve.SieveConfig;
import ProjectSteamCrafting.Sieve.SieveConfigLoader;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.UUID;

import static ProjectSteamCrafting.Registry.*;

public class EntityWoodMill extends BlockEntity implements ProjectSteam.Core.IMechanicalBlockProvider, INetworkTagReceiver, ICrankShaftConnector {


    double myFriction = 10;
    double myInertia = 1;
    double maxStress = 300;

    public AbstractMechanicalBlock myMechanicalBlock = new AbstractMechanicalBlock(0, this) {
        @Override
        public double getMaxStress() {
            return maxStress;
        }

        @Override
        public double getInertia(Direction face) {
            return myInertia;
        }

        @Override
        public double getTorqueResistance(Direction face) {
            return myFriction;
        }

        @Override
        public double getTorqueProduced(Direction face) {
           return 0;
        }

        @Override
        public double getRotationMultiplierToInside(@org.jetbrains.annotations.Nullable Direction receivingFace) {
            return 1;
        }
    };

    @Override
    public BlockEntity getBlockEntity() {
        return this;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        myMechanicalBlock.mechanicalOnload();
        if (level.isClientSide) {
            CompoundTag myOnloadTag = new CompoundTag();
            myOnloadTag.putUUID("ClientWoodMillOnload", Minecraft.getInstance().player.getUUID());
            PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(this, myOnloadTag));
        }
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderSystem.recordRenderCall(() -> {

            });
        }
    }

    @Override
    public void setRemoved() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderSystem.recordRenderCall(() -> {

            });
        }
        super.setRemoved();
    }

    @Override
    public void readClient(CompoundTag tag) {
        myMechanicalBlock.mechanicalReadClient(tag);
    }

    @Override
    public void readServer(CompoundTag tag) {
        myMechanicalBlock.mechanicalReadServer(tag);
        if (tag.contains("ClientWoodMillOnload")) {
            UUID from = tag.getUUID("ClientWoodMillOnload");
            ServerPlayer pfrom = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(from);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        myMechanicalBlock.mechanicalLoadAdditional(tag, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        myMechanicalBlock.mechanicalSaveAdditional(tag, registries);
    }

    @Override
    public AbstractMechanicalBlock getMechanicalBlock(Direction side) {
        BlockState myState = getBlockState();
        if (myState.getBlock() instanceof BlockWoodMill) {
            if (side == Direction.DOWN) {
                BlockEntity t = level.getBlockEntity(getBlockPos().relative(side));
                if (t instanceof EntityCrankShaftBase cs) {
                    if (cs.getBlockState().getValue(BlockCrankShaftBase.ROTATION_AXIS) != getBlockState().getValue(BlockWoodMill.FACING).getAxis()) {
                        return myMechanicalBlock;
                    }
                }
            }
        }
        return null;
    }

    public EntityWoodMill(BlockPos pos, BlockState blockState) {
        super(ENTITY_WOODMILL.get(), pos, blockState);
    }

    void broadcastChangeOfInventoryAndSetChanged() {
        if (!level.isClientSide) {
            //CompoundTag meshInfo = ;
            //PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(getBlockPos()), PacketBlockEntity.getBlockEntityPacket(this, meshInfo));
            //setChanged();
        }
    }

    /*
    SieveConfig.MachineRecipe getRecipeForInputs(ItemStack inputs) {
        for (SieveConfig.MachineRecipe i : config.recipes) {
            if (ItemUtils.matches(i.requiredMesh, myMesh)) {
                SieveConfig.MachineRecipe.Item input = i.inputItems.getFirst();
                if (ItemUtils.matches(input.id, inputs)) {
                    return i;
                }
            }
        }
        return null;
    }
     */

    /*
    void completeRecipe() {
        if (currentRecipe != null) {
            for (SieveConfig.MachineRecipe.Item item : currentRecipe.outputItems) {
                int actual_num = 0;
                for (int i = 0; i < item.amount; ++i) {
                    if (item.p >= 1.0F || Math.random() < (double) item.p) {
                        ++actual_num;
                    }
                }
                ItemStack output = ItemUtils.getItemStackFromId(item.id, actual_num);
                for (Direction i : Direction.values()) {
                    if(i==Direction.UP)continue;
                    IItemHandler inv = level.getCapability(Capabilities.ItemHandler.BLOCK, getBlockPos().relative(i), i.getOpposite());
                    if (inv instanceof IItemHandler) {
                        for (int j = 0; j < inv.getSlots(); j++) {
                            output = inv.insertItem(j, output, false);
                            if (output.isEmpty()) break;
                        }
                    }
                }
                if (!output.isEmpty()) {
                    ItemEntity ie = new ItemEntity(level, getBlockPos().getX(), getBlockPos().getY() + 1, getBlockPos().getZ(), output);
                    level.addFreshEntity(ie);
                }
                myInputs.shrink(1);
                broadcastChangeOfInventoryAndSetChanged();
            }
            currentRecipe = null;
        }
        myFriction = config.baseResistance;
    }
     */
/*
    boolean tryAddElementToSieveInventory(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (myInputs.isEmpty()) {
            if (getRecipeForInputs(stack) != null) {
                myInputs = stack.copyWithCount(1);
                stack.shrink(1);
                broadcastChangeOfInventoryAndSetChanged();
                return true;
            }
        } else {
            if (ItemStack.isSameItemSameComponents(stack, myInputs)) {
                int maxStackSize = Math.min(myInputs.getMaxStackSize(), maxStackSizeForSieve);
                int toAdd = Math.min(maxStackSize - myInputs.getCount(), 1);
                myInputs.grow(toAdd);
                stack.shrink(toAdd);
                broadcastChangeOfInventoryAndSetChanged();
                return true;
            }
        }
        return false;
    }
*/


    public void tick() {
        myMechanicalBlock.mechanicalTick();

        if (!level.isClientSide) {
           /*
            if (currentRecipe != null) {
                currentProgress += Math.abs((float) (Static.rad_to_degree(myMechanicalBlock.internalVelocity) / 360f / Static.TPS));
                if (currentProgress >= currentRecipe.timeRequired) {
                    completeRecipe();
                    currentProgress = 0;
                }
            } else {
                if (!myInputs.isEmpty() && !myMesh.isEmpty()) {
                    currentRecipe = getRecipeForInputs(myInputs);
                    if (currentRecipe != null) {
                        myFriction = config.baseResistance + currentRecipe.additionalResistance;
                        CompoundTag timeRequiredTag = new CompoundTag();
                        timeRequiredTag.putDouble("timeRequired", currentRecipe.timeRequired);
                        PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(getBlockPos()), PacketBlockEntity.getBlockEntityPacket(this, timeRequiredTag));
                    }
                }
            }
            */
        } else {
            /*
            if (!myInputs.isEmpty()) {
                currentProgress += Math.abs((float) (Static.rad_to_degree(myMechanicalBlock.internalVelocity) / 360f / Static.TPS));
                if (currentProgress > client_syncedCurrentRecipeTime) {

                }
            } else currentProgress = 0;
             */
        }
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityWoodMill) t).tick();
    }
}
