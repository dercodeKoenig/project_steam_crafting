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
import net.minecraft.core.particles.*;
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
import ProjectSteam.Core.AbstractMechanicalBlock;
import ProjectSteam.Blocks.Mechanics.CrankShaft.ICrankShaftConnector;
import ProjectSteam.Blocks.Mechanics.CrankShaft.BlockCrankShaftBase;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.List;
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
    double currentProgress;
    double client_syncedCurrentRecipeTime;

     double click_force = config.clickForce;
     double k = config.k;
     double max_speed = 20;
    int maxStackSizeForSieve = config.inventorySize;

    int ticksRemainingForForce = 0;
    double myFriction = config.baseResistance;
    double myInertia = 1;
    double maxStress = 100;
    double myForce = 0;

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
        if (tag.contains("timeRequired")) {
            client_syncedCurrentRecipeTime = tag.getDouble("timeRequired");
        }
        if (tag.contains("syncMaxStackSize")) {
            maxStackSizeForSieve = tag.getInt("syncMaxStackSize");
        }
    }

    @Override
    public void readServer(CompoundTag tag) {
        myMechanicalBlock.mechanicalReadServer(tag);
        if (tag.contains("ClientSieveOnload")) {
            UUID from = tag.getUUID("ClientSieveOnload");
            ServerPlayer pfrom = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(from);
            CompoundTag info = getMeshUpdateTag();
            info.putInt("syncMaxStackSize", maxStackSizeForSieve);
            PacketDistributor.sendToPlayer(pfrom, PacketBlockEntity.getBlockEntityPacket(this, info));
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

        meshInfo.putBoolean("hasInput", !myInputs.isEmpty());
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
            ItemEntity i = new ItemEntity(level, getBlockPos().getX(), getBlockPos().getY()+1, getBlockPos().getZ(), myMesh);
            i.setDeltaMovement(0, 0.2, 0);
            level.addFreshEntity(i);
            myMesh = ItemStack.EMPTY;

            ItemEntity i2 = new ItemEntity(level, getBlockPos().getX(), getBlockPos().getY()+1, getBlockPos().getZ(), myInputs);
            i2.setDeltaMovement(0, 0.2, 0);
            level.addFreshEntity(i2);
            myInputs = ItemStack.EMPTY;

            broadcastChangeOfInventoryAndSetChanged();
        }
    }

    SieveConfig.MachineRecipe getRecipeForInputs(ItemStack inputs){
        for (SieveConfig.MachineRecipe i : config.recipes) {
            if( ItemUtils.matches(i.requiredMesh, myMesh)) {
                SieveConfig.MachineRecipe.Item input = i.inputItems.getFirst();
                if (ItemUtils.matches(input.id, inputs)) {
                    return i;
                }
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
                ItemEntity ie = new ItemEntity(level, getBlockPos().getX(), getBlockPos().getY()+1, getBlockPos().getZ(), output);
                level.addFreshEntity(ie);

                myInputs.shrink(1);
                broadcastChangeOfInventoryAndSetChanged();
            }
            currentRecipe = null;
        }
        myFriction = config.baseResistance;
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
                int maxStackSize = Math.min(myInputs.getMaxStackSize(), maxStackSizeForSieve);
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
        }
        if (!level.isClientSide) {
            if(myInputs.getCount() < maxStackSizeForSieve){
                double minX = getBlockPos().getX();
                double minY = getBlockPos().getY();
                double minZ = getBlockPos().getZ();
                double maxX = minX + 1;
                double maxY = minY + 1.5;
                double maxZ = minZ + 1;

                // Get all entities of class ItemEntity within the bounding box
                List<ItemEntity> itemEntities = level.getEntitiesOfClass(
                        ItemEntity.class,
                        new AABB(minX, minY, minZ, maxX, maxY, maxZ)
                );
                for(ItemEntity i : itemEntities){
                    tryAddElementToInventory(i.getItem());
                }
            }
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
        } else {
            if (!myInputs.isEmpty()) {
                currentProgress += Math.abs((float) (Static.rad_to_degree(myMechanicalBlock.internalVelocity) / 360f / Static.TPS));
                if (currentProgress > client_syncedCurrentRecipeTime) {
                    currentProgress = 0;
                    myInputs.shrink(1);
                }

                if (myInputs.getItem() instanceof BlockItem b) {
                    BlockState blockState = b.getBlock().defaultBlockState();
                    double dps = Math.abs(Static.rad_to_degree(myMechanicalBlock.internalVelocity));
                    int o = 100;
                    int n = (int) Math.min(7, dps / o);
                    if (level.random.nextFloat()*o < (int) dps % o) n += 1;
                    for (int i = 0; i < n; i++) { // Spawn multiple particles for better effect
                        double x = getBlockPos().getX() + 0.5 + (Math.random() - 0.5)*0.5;
                        double y = getBlockPos().getY() + 0.5;
                        double z = getBlockPos().getZ() + 0.5 + (Math.random() - 0.5)*0.5;

                        level.addParticle(
                                new DustParticleOptions(Vec3.fromRGB24(b.getBlock().defaultMapColor().col).toVector3f() , 0.4f),
                                x, y, z,
                                0.0, 0.0, 0.0 // Velocity: this shit does not work
                        );
                    }
                }
            }
        }
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntitySieve) t).tick();
    }
}
