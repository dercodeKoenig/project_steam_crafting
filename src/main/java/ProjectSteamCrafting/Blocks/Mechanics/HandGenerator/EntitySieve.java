package ProjectSteamCrafting.Blocks.Mechanics.HandGenerator;

import ARLib.network.INetworkTagReceiver;
import ProjectSteamCrafting.Registry;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import ProjectSteam.Core.AbstractMechanicalBlock;
import ProjectSteam.Blocks.Mechanics.CrankShaft.ICrankShaftConnector;
import ProjectSteam.Blocks.Mechanics.CrankShaft.EntityCrankShaftBase;
import ProjectSteam.Blocks.Mechanics.CrankShaft.BlockCrankShaftBase;

import static ProjectSteamCrafting.Registry.ENTITY_SIEVE;

public class EntitySieve extends BlockEntity implements ProjectSteam.Core.IMechanicalBlockProvider, INetworkTagReceiver, ICrankShaftConnector {

    VertexBuffer vertexBuffer;
    MeshData mesh;
    VertexBuffer vertexBuffer2;
    MeshData mesh2;
    VertexBuffer vertexBuffer3;
    MeshData mesh3;

    public static double MAX_FORCE = 100;
    public static double MAX_SPEED = 20;

    int lastLight = 0;

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
        super.setRemoved();
    }

    @Override
    public void readClient(CompoundTag tag) {
        myMechanicalBlock.mechanicalReadClient(tag);
    }

    @Override
    public void readServer(CompoundTag tag) {
        myMechanicalBlock.mechanicalReadServer(tag);
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


    public boolean onPlayerClicked(boolean isShift) {
        if (!isShift) {
            if (ticksRemainingForForce < 5) {
                ticksRemainingForForce += 5;
                return true;
            }
        }
        return false;
    }

    public void tick() {
        myMechanicalBlock.mechanicalTick();

        if(!level.isClientSide) {
            if (ticksRemainingForForce > 0) {
                ticksRemainingForForce--;
                myForce = MAX_FORCE - 1 * myMechanicalBlock.internalVelocity;
            } else {
                myForce = 0;
            }
        }

    }


    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntitySieve) t).tick();
    }
}
