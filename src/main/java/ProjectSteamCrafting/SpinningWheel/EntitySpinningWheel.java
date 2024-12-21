package ProjectSteamCrafting.SpinningWheel;

import ARLib.utils.BlockEntityItemStackHandler;
import ProjectSteamCrafting.Sieve.EntitySieve;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

import static ProjectSteamCrafting.Registry.ENTITY_SPINNING_WHEEL;

public class EntitySpinningWheel extends BlockEntity {

    BlockEntityItemStackHandler inventoryOutput;
    BlockEntityItemStackHandler inventoryInput;

    List<IItemHandler> itemHandlerInputs = new ArrayList<>();
    List<IItemHandler> itemHandlerOutputs = new ArrayList<>();

    // not used but required by the utils lib
    List<IFluidHandler> fluidHandlerInputs = new ArrayList<>();
    List<IFluidHandler> fluidHandlerOutputs = new ArrayList<>();

    public EntitySpinningWheel(BlockPos pos, BlockState blockState) {
        super(ENTITY_SPINNING_WHEEL.get(), pos, blockState);

        inventoryInput = new BlockEntityItemStackHandler(4,this);
        inventoryOutput = new BlockEntityItemStackHandler(4,this);

        itemHandlerInputs.add(inventoryInput);
        itemHandlerOutputs.add(inventoryOutput);
    }

    public InteractionResult use(Player player) {
        return InteractionResult.PASS;
    }

    public void tick(){

    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntitySpinningWheel) t).tick();
    }
}