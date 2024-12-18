package ProjectSteamCrafting.WoodMill;

import ProjectSteamCrafting.Sieve.EntitySieve;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import static ProjectSteamCrafting.Registry.ENTITY_WOODMILL;

public class BlockWoodMill extends Block implements EntityBlock {
    public static EnumProperty<Direction> FACING = EnumProperty.create("facing", Direction.class);

    public BlockWoodMill() {
        super(Properties.of().noOcclusion().strength(1.0f));
        BlockState state = this.stateDefinition.any();
        state = state.setValue(FACING, Direction.SOUTH);
        this.registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return ENTITY_WOODMILL.get().create(blockPos,blockState);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (placer != null) {
            if(placer.isShiftKeyDown())
                level.setBlock(pos, state.setValue(FACING, placer.getDirection()), 3);
            else
                level.setBlock(pos, state.setValue(FACING, placer.getDirection().getOpposite()), 3);
        }

        super.setPlacedBy(level, pos, state, placer, stack); // Call the super method for any additional behavior
    }

    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if(state.getValue(FACING).getAxis() == Direction.Axis.X) {
            return Shapes.create((double) -1F, (double) 0F, (double) 0F, (double) 2F, (double) 0.5F, (double) 1F);
        }
        if(state.getValue(FACING).getAxis() == Direction.Axis.Z) {
            return Shapes.create((double) 0F, (double) 0F, (double) -1F, (double) 1F, (double) 0.5F, (double) 2F);
        }
        return Shapes.create((double) 0F, (double) 0F, (double) 0F, (double) 1F, (double) 1F, (double) 1F);
    }
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityWoodMill::tick;
    }
}
