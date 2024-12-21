package ProjectSteamCrafting.SpinningWheel;

import ARLib.utils.BlockEntityItemStackHandler;
import ARLib.utils.InventoryUtils;
import ARLib.utils.MachineRecipe;
import ARLib.utils.recipePart;
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

    SpinningWheelConfig config = SpinningWheelConfigLoader.loadConfig();

    SpinningWheelConfig.MachineRecipe currentRecipe = null;
    double currentProgress;

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

    public void scanFornewRecipe() {
        for (SpinningWheelConfig.MachineRecipe r : config.recipes) {
            if(InventoryUtils.hasInputs(itemHandlerInputs, fluidHandlerInputs, List.of(new recipePart(r.inputItem.id,r.inputItem.amount,1)))){
                currentRecipe = r;
                currentProgress = 0;
                break;
            }
        }
    }

    public InteractionResult use(Player player) {
        return InteractionResult.PASS;
    }

    public void tick(){
        if (currentRecipe == null) {
            scanFornewRecipe();
        }
        if (master.hasinputs(currentRecipe.inputs) && master.canFitOutputs(currentRecipe.outputs)) {
            if (master.getTotalEnergyStored() >= currentRecipe.energyPerTick) {
                progress += 1;
                master.consumeEnergy(currentRecipe.energyPerTick);
                if (progress == currentRecipe.ticksRequired) {
                    master.consumeInput(currentRecipe.inputs, false);
                    master.produceOutput(currentRecipe.outputs);
                    reset();
                }
                return true;
            }
        } else {
            reset();
        }
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntitySpinningWheel) t).tick();
    }
}