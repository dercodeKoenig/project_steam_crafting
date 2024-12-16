package ProjectSteamCrafting.Sieve;

import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketBlockEntity;
import ARLib.utils.ItemUtils;
import ProjectSteam.Static;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import ProjectSteam.Core.AbstractMechanicalBlock;
import ProjectSteam.Blocks.Mechanics.CrankShaft.ICrankShaftConnector;
import ProjectSteam.Blocks.Mechanics.CrankShaft.BlockCrankShaftBase;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.UUID;

import static ProjectSteamCrafting.Registry.ENTITY_SIEVE;

public class EntitySieve extends BlockEntity implements ProjectSteam.Core.IMechanicalBlockProvider, INetworkTagReceiver, ICrankShaftConnector {

    static SieveConfig config = SieveConfigLoader.loadConfig();

    VertexBuffer myInputRendererBuffer;
    ItemStack lastInputStackForRender= ItemStack.EMPTY;
    ResourceLocation inputStackTexture = ResourceLocation.withDefaultNamespace("textures/block/air");

    ItemStack myMesh = ItemStack.EMPTY;
    ItemStack myInputs = ItemStack.EMPTY;

    SieveConfig.MachineRecipe currentRecipe = null;

    static double click_force = config.clickForce;
     static double k = config.k;
     static double max_speed = 20;
    int ticksRemainingForForce = 0;


    double myFriction = 30;
    double myInertia = 1;
    double maxStress = 100;
    double myForce;

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
            double actualForce = myForce * Math.max(0, (1 - Math.abs(internalVelocity) / max_speed));
            return actualForce;
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
            myOnloadTag.putUUID("ClientSieveOnload", Minecraft.getInstance().player.getUUID());
            PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(this, myOnloadTag));
        }
        if(FMLEnvironment.dist == Dist.CLIENT){
            RenderSystem.recordRenderCall(() -> {
                myInputRendererBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
            });
        }
    }

    @Override
    public void setRemoved() {
        removeMyMesh();
        if(FMLEnvironment.dist == Dist.CLIENT){
            RenderSystem.recordRenderCall(() -> {
                myInputRendererBuffer.close();
            });
        }
        super.setRemoved();
    }

    @Override
    public void readClient(CompoundTag tag) {
        myMechanicalBlock.mechanicalReadClient(tag);
        if (tag.contains("hasMesh")) {
            if (!tag.getBoolean("hasMesh")) {
                myMesh = ItemStack.EMPTY;
            }
        }
        if (tag.contains("mesh")) {
            myMesh = ItemStack.parse(level.registryAccess(), tag.getCompound("mesh")).get();
        }

        if (tag.contains("hasInput")) {
            if (!tag.getBoolean("hasInput")) {
                myInputs = ItemStack.EMPTY;
            }
        }
        if (tag.contains("inputs")) {
            myInputs = ItemStack.parse(level.registryAccess(), tag.getCompound("inputs")).get();
        }
    }

    @Override
    public void readServer(CompoundTag tag) {
        myMechanicalBlock.mechanicalReadServer(tag);
        if (tag.contains("ClientSieveOnload")) {
            UUID from = tag.getUUID("ClientSieveOnload");
            ServerPlayer pfrom = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(from);
            CompoundTag meshInfo = getMeshUpdateTag();
            PacketDistributor.sendToPlayer(pfrom, PacketBlockEntity.getBlockEntityPacket(this, meshInfo));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        myMechanicalBlock.mechanicalLoadAdditional(tag, registries);
        if (tag.contains("mesh")) {
            CompoundTag meshTag = tag.getCompound("mesh");
            myMesh = ItemStack.parse(registries, meshTag).get();
        }
        if (tag.contains("inputs")) {
            CompoundTag inputsTag = tag.getCompound("inputs");
            myInputs = ItemStack.parse(registries, inputsTag).get();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        myMechanicalBlock.mechanicalSaveAdditional(tag, registries);
        if (!myMesh.isEmpty()) {
            Tag meshTag = myMesh.save(registries);
            tag.put("mesh", meshTag);
        }
        if (!myInputs.isEmpty()) {
            Tag inputsTag = myInputs.save(registries);
            tag.put("inputs", inputsTag);
        }
    }

    @Override
    public AbstractMechanicalBlock getMechanicalBlock(Direction side) {
        BlockState myState = getBlockState();
        if (myState.getBlock() instanceof BlockSieve) {
            if (side == myState.getValue(BlockSieve.FACING)) {
                BlockEntity t = level.getBlockEntity(getBlockPos().relative(side));
                if (t instanceof ProjectSteam.Blocks.Mechanics.CrankShaft.EntityCrankShaftBase cs) {
                    if (cs.getBlockState().getValue(BlockCrankShaftBase.ROTATION_AXIS) != getBlockState().getValue(BlockSieve.FACING).getAxis()) {
                        return myMechanicalBlock;
                    }
                }
            }
        }
        return null;
    }

    public EntitySieve(BlockPos pos, BlockState blockState) {
        super(ENTITY_SIEVE.get(), pos, blockState);
    }

    CompoundTag getMeshUpdateTag() {
        CompoundTag meshInfo = new CompoundTag();
        meshInfo.putBoolean("hasMesh", !myMesh.isEmpty());
        if (!myMesh.isEmpty()) {
            Tag meshTag = myMesh.save(level.registryAccess());
            meshInfo.put("mesh", meshTag);
        }

        meshInfo.putBoolean("hasInput", !myMesh.isEmpty());
        if (!myInputs.isEmpty()) {
            Tag inputsTag = myInputs.save(level.registryAccess());
            meshInfo.put("inputs", inputsTag);
        }

        return meshInfo;
    }

    void broadcastChangeOfInventoryAndSetChanged() {
        if (!level.isClientSide) {
            CompoundTag meshInfo = getMeshUpdateTag();
            PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(getBlockPos()), PacketBlockEntity.getBlockEntityPacket(this, meshInfo));
            setChanged();
        }
    }

    void removeMyMesh() {
        if (!myMesh.isEmpty()) {
            ItemEntity i = new ItemEntity(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), myMesh);
            i.setDeltaMovement(0, 0.2, 0);
            level.addFreshEntity(i);
            myMesh = ItemStack.EMPTY;
            broadcastChangeOfInventoryAndSetChanged();
        }
    }

    SieveConfig.MachineRecipe getRecipeForInputs(ItemStack inputs){
        for (SieveConfig.MachineRecipe i : config.recipes) {
            SieveConfig.MachineRecipe.Item input = i.inputItems.getFirst();
            System.out.println(input.id+":"+ inputs);
            if (ItemUtils.matches(input.id, inputs)) {
                return i;
            }
        }
        return null;
    }
    void completeRecipe(){
        if(currentRecipe != null){

            for(SieveConfig.MachineRecipe.Item item:currentRecipe.outputItems){
                int actual_num = 0;
                for(int i = 0; i < item.amount; ++i) {
                    if (item.p >= 1.0F || Math.random() < (double)item.p) {
                        ++actual_num;
                    }
                }
                ItemStack output = ItemUtils.getItemStackFromId(item.id, actual_num);
                ItemEntity ie = new ItemEntity(level, getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ(), output);
                level.addFreshEntity(ie);

                myInputs.shrink(1);
                broadcastChangeOfInventoryAndSetChanged();
            }

            currentRecipe = null;
        }
    }

    boolean tryAddElementToInventory(ItemStack stack){
        if(stack.isEmpty())return false;

        if(myInputs.isEmpty()) {
         if(getRecipeForInputs(stack)!=null){
             myInputs = stack.copyWithCount(1);
             stack.shrink(1);
             broadcastChangeOfInventoryAndSetChanged();
             return true;
         }
        }else{
            if(ItemStack.isSameItemSameComponents(stack, myInputs)){
                int maxStackSize = myInputs.getMaxStackSize();
                int toAdd = Math.min(maxStackSize - myInputs.getCount(), 1);
                myInputs.grow(toAdd);
                stack.shrink(toAdd);
                broadcastChangeOfInventoryAndSetChanged();
                return true;
            }
        }
        return  false;
    }

    public InteractionResult use(Player player) {
        if (!player.isShiftKeyDown()) {
            if (player.getMainHandItem().getItem() instanceof IMesh) {
                removeMyMesh();
                myMesh = player.getMainHandItem().copyWithCount(1);
                player.getMainHandItem().shrink(1);
                broadcastChangeOfInventoryAndSetChanged();
                return InteractionResult.SUCCESS;
            }
            if(tryAddElementToInventory(player.getMainHandItem()))return InteractionResult.SUCCESS;
            if(tryAddElementToInventory(player.getOffhandItem()))return InteractionResult.SUCCESS;


            if(player.getMainHandItem().isEmpty()){
                if (ticksRemainingForForce < 5 && getMechanicalBlock(getBlockState().getValue(BlockSieve.FACING)) == null) {
                    ticksRemainingForForce += 5;
                    player.causeFoodExhaustion(0.5f);
                    return InteractionResult.SUCCESS_NO_ITEM_USED;
                }
            }
        } else {
            removeMyMesh();
            broadcastChangeOfInventoryAndSetChanged();
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public void tick() {
        myMechanicalBlock.mechanicalTick();

        if (!level.isClientSide) {
            if (ticksRemainingForForce > 0 && getMechanicalBlock(getBlockState().getValue(BlockSieve.FACING)) == null) {
                ticksRemainingForForce--;
                myForce = click_force - k * myMechanicalBlock.internalVelocity;
            } else {
                myForce = 0;
                ticksRemainingForForce = 0;
            }

            if (currentRecipe != null) {
                currentRecipe.currentProgress += Math.abs((float) (Static.rad_to_degree(myMechanicalBlock.internalVelocity) / 360f));
                System.out.println(currentRecipe.currentProgress);
                if (currentRecipe.currentProgress >= currentRecipe.timeRequired) {
                    completeRecipe();
                    System.out.println("completed recipe");
                }
            } else {
                if (!myInputs.isEmpty()) {
                    currentRecipe = getRecipeForInputs(myInputs);
                }
            }
        }
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntitySieve) t).tick();
    }
}
