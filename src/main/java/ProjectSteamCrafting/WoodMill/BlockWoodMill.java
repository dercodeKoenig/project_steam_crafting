package ProjectSteamCrafting.WoodMill;

import ARLib.multiblockCore.BlockMultiblockMaster;
import ARLib.multiblockCore.EntityMultiblockMaster;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

import static ProjectSteamCrafting.Registry.ENTITY_WOODMILL;

public class BlockWoodMill extends BlockMultiblockMaster implements EntityBlock {

    public BlockWoodMill() {
        super(Properties.of().noOcclusion().strength(1.0f).noOcclusion());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
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
                level.setBlock(pos, state.setValue(BlockStateProperties.HORIZONTAL_FACING, placer.getDirection()), 3);
            else
                level.setBlock(pos, state.setValue(BlockStateProperties.HORIZONTAL_FACING, placer.getDirection().getOpposite()), 3);
        }

        super.setPlacedBy(level, pos, state, placer, stack); // Call the super method for any additional behavior
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        BlockEntity me = level.getBlockEntity(pos);
        if(me instanceof EntityMultiblockMaster mm){
            mm.scanStructure();
        }
            return state;
    }

    /* managed by BlockMultiblockMaster
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockEntity b = level.getBlockEntity(pos);
        if(b instanceof EntityWoodMill h)
            return h.use(player);
        return InteractionResult.PASS;
    }
     */

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if(blockEntity instanceof EntityWoodMill s){
            s.removeCurrentInputStacks();
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return EntityWoodMill::tick;
    }
}
