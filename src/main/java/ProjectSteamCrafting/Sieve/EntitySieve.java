package ProjectSteamCrafting.Sieve;

import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketBlockEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import ProjectSteam.Core.AbstractMechanicalBlock;
import ProjectSteam.Blocks.Mechanics.CrankShaft.ICrankShaftConnector;
import ProjectSteam.Blocks.Mechanics.CrankShaft.BlockCrankShaftBase;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.UUID;

import static ProjectSteamCrafting.Registry.ENTITY_SIEVE;

public class EntitySieve extends BlockEntity implements ProjectSteam.Core.IMechanicalBlockProvider, INetworkTagReceiver, ICrankShaftConnector {

    VertexBuffer vertexBuffer;
    MeshData mesh;
    VertexBuffer vertexBuffer2;
    MeshData mesh2;
    VertexBuffer vertexBuffer3;
    MeshData mesh3;
    int lastLight = 0;


    ItemStack myMesh = ItemStack.EMPTY;

    public static double MAX_FORCE = 100;
    public static double MAX_SPEED = 20;
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
            double actualForce = myForce * Math.max(0, (1 - Math.abs(internalVelocity) / MAX_SPEED));
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
    }

    @Override
    public void setRemoved() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderSystem.recordRenderCall(() -> {
                vertexBuffer.close();
                vertexBuffer2.close();
                vertexBuffer3.close();
            });
        }
        removeMyMesh();
        super.setRemoved();
    }

    @Override
    public void readClient(CompoundTag tag) {
        myMechanicalBlock.mechanicalReadClient(tag);
        if(tag.contains("hasMesh")){
            if(!tag.getBoolean("hasMesh")){
                myMesh = ItemStack.EMPTY;
            }
        }
        if(tag.contains("mesh")){
            myMesh = ItemStack.parse(level.registryAccess(),tag.getCompound("mesh")).get();
        }
    }

    @Override
    public void readServer(CompoundTag tag) {
        myMechanicalBlock.mechanicalReadServer(tag);
        if(tag.contains("ClientSieveOnload")){
            UUID from = tag.getUUID("ClientSieveOnload");
            ServerPlayer pfrom = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(from);
            CompoundTag meshInfo = getMeshUpdateTag();
            PacketDistributor.sendToPlayer(pfrom,PacketBlockEntity.getBlockEntityPacket(this,meshInfo));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        myMechanicalBlock.mechanicalLoadAdditional(tag, registries);
        if(tag.contains("mesh")){
            CompoundTag meshTag = tag.getCompound("mesh");
            myMesh = ItemStack.parse(registries,meshTag).get();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        myMechanicalBlock.mechanicalSaveAdditional(tag, registries);
        if(!myMesh.isEmpty()){
            Tag meshTag = myMesh.save(registries);
            tag.put("mesh", meshTag);
        }
    }

    @Override
    public AbstractMechanicalBlock getMechanicalBlock(Direction side) {
        BlockState myState = getBlockState();
        if (myState.getBlock() instanceof BlockSieve) {
            if (side == myState.getValue(BlockSieve.FACING)) {
                BlockEntity t = level.getBlockEntity(getBlockPos().relative(side));
                if (t instanceof ProjectSteam.Blocks.Mechanics.CrankShaft.EntityCrankShaftBase cs) {
                    if(cs.getBlockState().getValue(BlockCrankShaftBase.ROTATION_AXIS) != getBlockState().getValue(BlockSieve.FACING).getAxis()) {
                        return myMechanicalBlock;
                    }
                }
            }
        }
        return null;
    }

    public EntitySieve(BlockPos pos, BlockState blockState) {
        super(ENTITY_SIEVE.get(), pos, blockState);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            RenderSystem.recordRenderCall(() -> {
                vertexBuffer = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                vertexBuffer2 = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
                vertexBuffer3 = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
            });
        }

    }
CompoundTag getMeshUpdateTag(){
    CompoundTag meshInfo = new CompoundTag();
    meshInfo.putBoolean("hasMesh", !myMesh.isEmpty());
    if (!myMesh.isEmpty()) {
        Tag meshTag = myMesh.save(level.registryAccess());
        meshInfo.put("mesh", meshTag);
    }
    return meshInfo;
}
void sendMeshInfoToClient() {
    if (!level.isClientSide) {
        CompoundTag meshInfo = getMeshUpdateTag();
        PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) level, new ChunkPos(getBlockPos()), PacketBlockEntity.getBlockEntityPacket(this, meshInfo));
    }
}
    void removeMyMesh(){
        if(!myMesh.isEmpty()){
            ItemEntity i = new ItemEntity(level,getBlockPos().getX(),getBlockPos().getY(),getBlockPos().getZ(),myMesh);
            i.setDeltaMovement(0,0.2,0);
            level.addFreshEntity(i);
            myMesh = ItemStack.EMPTY;
        }
    }
    public InteractionResult use(Player player) {
        if (!player.isShiftKeyDown()) {
            if (player.getMainHandItem().getItem() instanceof IMesh) {
                removeMyMesh();
                myMesh = player.getMainHandItem().copyWithCount(1);
                player.getMainHandItem().shrink(1);
                sendMeshInfoToClient();
                setChanged();
                return InteractionResult.SUCCESS;
            } else {
                if (ticksRemainingForForce < 5 && getMechanicalBlock(getBlockState().getValue(BlockSieve.FACING)) == null) {
                    ticksRemainingForForce += 5;
                    player.causeFoodExhaustion(0.5f);
                    return InteractionResult.SUCCESS_NO_ITEM_USED;
                }
            }
        } else {
            removeMyMesh();
            sendMeshInfoToClient();
            setChanged();
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public void tick() {
        myMechanicalBlock.mechanicalTick();

        if(!level.isClientSide) {
            if (ticksRemainingForForce > 0 && getMechanicalBlock(getBlockState().getValue(BlockSieve.FACING)) == null) {
                ticksRemainingForForce--;
                myForce = MAX_FORCE - 1 * myMechanicalBlock.internalVelocity;
            } else {
                myForce = 0;
                ticksRemainingForForce =0;
            }
        }

    }


    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntitySieve) t).tick();
    }
}
