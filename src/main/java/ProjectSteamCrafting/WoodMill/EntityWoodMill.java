package ProjectSteamCrafting.WoodMill;

import ARLib.multiblockCore.BlockMultiblockMaster;
import ARLib.multiblockCore.EntityMultiblockMaster;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static ProjectSteam.Registry.CASING;
import static ProjectSteam.Registry.CASING_SLAB;
import static ProjectSteamCrafting.Registry.*;

public class EntityWoodMill extends EntityMultiblockMaster implements ProjectSteam.Core.IMechanicalBlockProvider, INetworkTagReceiver, ICrankShaftConnector {

    ItemStack currentInput = ItemStack.EMPTY;

    WoodMillConfig config = WoodMillConfigLoader.loadConfig();

    double myFriction = config.baseResistance;
    double myInertia = 1;
    double maxStress = 500;

    double timeRequired = 50;
    double currentProgress = 0;

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

    public static Object[][][] structure = {
            {{'C'},{'c'}, {'C'}}
    };
    public static HashMap<Character, List<Block>> charMapping = new HashMap<>();

    static {
        List<Block> c = new ArrayList<>();
        c.add(WOODMILL.get());
        charMapping.put('c', c);

        List<Block> C = new ArrayList<>();
        C.add(CASING_SLAB.get());
        charMapping.put('C', C);
    }
    @Override
    public Object[][][] getStructure() {
        return structure;
    }

    @Override
    public HashMap<Character, List<Block>> getCharMapping() {
        return charMapping;
    }

    @Override
    public void onLoad() {
        super.onLoad();

        structure = new Object[][][]{
                {{'C'}, {'c'}, {'C'}}
        };

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
        if(tag.contains("hasInput")){
            if(!tag.getBoolean("hasInput")){
                currentInput = ItemStack.EMPTY;
            }
        }
        if(tag.contains("currentInput")){
            currentInput = ItemStack.parse(level.registryAccess(),tag.getCompound("currentInput")).get();
            currentProgress = 0;
        }
        super.readClient(tag);
    }

    @Override
    public void readServer(CompoundTag tag) {
        myMechanicalBlock.mechanicalReadServer(tag);
        if (tag.contains("ClientWoodMillOnload")) {
            UUID from = tag.getUUID("ClientWoodMillOnload");
            ServerPlayer pfrom = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(from);
            CompoundTag info = getClientSyncUpdateTag();
            PacketDistributor.sendToPlayer(pfrom, PacketBlockEntity.getBlockEntityPacket(this, info));
        }
        super.readServer(tag);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        myMechanicalBlock.mechanicalLoadAdditional(tag, registries);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        myMechanicalBlock.mechanicalSaveAdditional(tag, registries);
    }

    @Override
    public AbstractMechanicalBlock getMechanicalBlock(Direction side) {
        if(!getBlockState().getValue(BlockMultiblockMaster.STATE_MULTIBLOCK_FORMED))return null;
        BlockState myState = getBlockState();
        if (myState.getBlock() instanceof BlockWoodMill) {
            if (side == Direction.DOWN) {
                BlockEntity t = level.getBlockEntity(getBlockPos().relative(side));
                if (t instanceof EntityCrankShaftBase cs&& cs.myType == CrankShaftType.LARGE) {
                    if (cs.getBlockState().getValue(BlockCrankShaftBase.ROTATION_AXIS) != getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis()) {
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

    CompoundTag getClientSyncUpdateTag(){
        CompoundTag info = new CompoundTag();
        info.putBoolean("hasInput", !currentInput.isEmpty());
        if(!currentInput.isEmpty()){
            info.put("currentInput", currentInput.save(level.registryAccess()));
        }
        return info;
    }

    void broadcastChangeOfInventoryAndSetChanged() {
        if (!level.isClientSide) {
            CompoundTag info = getClientSyncUpdateTag();
            PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(getBlockPos()), PacketBlockEntity.getBlockEntityPacket(this, info));
            setChanged();
        }
    }


    WoodMillConfig.MachineRecipe getRecipeForInputs(ItemStack inputs) {
        for (WoodMillConfig.MachineRecipe i : config.recipes) {
                WoodMillConfig.MachineRecipe.Item input = i.inputItem;
                if (ItemUtils.matches(input.id, inputs)) {
                    return i;
                }
        }
        return null;
    }

void removeCurrentInputStack(){
    if (!currentInput.isEmpty()) {
        BlockPos op =  getBlockPos();
        ItemEntity ie = new ItemEntity(level,op.getX(),op.getY()+3,op.getZ(), currentInput);
        level.addFreshEntity(ie);
    }
    currentInput = ItemStack.EMPTY;
    broadcastChangeOfInventoryAndSetChanged();
}

    void completeRecipe() {
        if (currentInput != ItemStack.EMPTY) {
            for (WoodMillConfig.MachineRecipe recipe : config.recipes) {
                if (ItemUtils.matches(recipe.inputItem.id, currentInput)) {
                    ItemStack output = ItemUtils.getItemStackFromId(recipe.outputItem.id, recipe.outputItem.amount);

                /*
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
                 */
                    if (!output.isEmpty()) {
                        BlockPos op =  getBlockPos().relative(getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite()).relative(Direction.UP);
                        ItemEntity ie = new ItemEntity(level,op.getX(),op.getY(),op.getZ(), output);
                        level.addFreshEntity(ie);
                    }
                    currentInput = ItemStack.EMPTY;
                    broadcastChangeOfInventoryAndSetChanged();
                    break;
                }
            }
        }
        myFriction = config.baseResistance;
    }

    boolean trySetCurrentInput(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!currentInput.isEmpty()) return false;
        WoodMillConfig.MachineRecipe r = getRecipeForInputs(stack);
        if (r != null) {
            if(!level.isClientSide) {
                currentInput = stack.copyWithCount(1);
                stack.shrink(1);
                myFriction = config.baseResistance + r.additionalResistance;
                broadcastChangeOfInventoryAndSetChanged();
            }
            return true;
        }
        return false;
    }

@Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hitResult) {
    if (!player.isShiftKeyDown()) {
        if (trySetCurrentInput(player.getMainHandItem()))
            return InteractionResult.SUCCESS;
        else
            return InteractionResult.SUCCESS_NO_ITEM_USED;
    }
    return InteractionResult.PASS;
}

    public void tick() {
        myMechanicalBlock.mechanicalTick();

        if (getBlockState().getValue(BlockMultiblockMaster.STATE_MULTIBLOCK_FORMED)) {
            if (!level.isClientSide) {
                if (!currentInput.isEmpty()) {
                    currentProgress += Math.abs((float) (Static.rad_to_degree(myMechanicalBlock.internalVelocity) / 360f / Static.TPS)) * config.speedMultiplier;
                    if (currentProgress >= timeRequired) {
                        completeRecipe();
                        currentProgress = 0;
                    }
                }
            } else {
                if (!currentInput.isEmpty()) {
                    currentProgress += Math.abs((float) (Static.rad_to_degree(myMechanicalBlock.internalVelocity) / 360f / Static.TPS)) * config.speedMultiplier;
                    if (currentProgress >= timeRequired) {

                    }
                }
            }
        }
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityWoodMill) t).tick();
    }


    static List<CrankShaftType> allowedCrankshaftTypes = new ArrayList();
    static{
        allowedCrankshaftTypes.add(CrankShaftType.LARGE);
    }
    @Override
    public List<CrankShaftType> getConnectableCrankshafts() {
        return allowedCrankshaftTypes;
    }
}
