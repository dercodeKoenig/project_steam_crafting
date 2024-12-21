package ProjectSteamCrafting.SpinningWheel;

import ARLib.network.INetworkTagReceiver;
import ARLib.utils.BlockEntityItemStackHandler;
import ARLib.utils.InventoryUtils;
import ARLib.utils.MachineRecipe;
import ARLib.utils.recipePart;
import ProjectSteam.Core.AbstractMechanicalBlock;
import ProjectSteam.Core.IMechanicalBlockProvider;
import ProjectSteam.Static;
import ProjectSteamCrafting.Sieve.EntitySieve;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

import static ProjectSteamCrafting.Registry.ENTITY_SPINNING_WHEEL;

public class EntitySpinningWheel extends BlockEntity implements INetworkTagReceiver, IMechanicalBlockProvider {

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
            return myForce;
        }

        @Override
        public double getRotationMultiplierToInside(@org.jetbrains.annotations.Nullable Direction receivingFace) {
            return 1;
        }
    };

    public EntitySpinningWheel(BlockPos pos, BlockState blockState) {
        super(ENTITY_SPINNING_WHEEL.get(), pos, blockState);

        inventoryInput = new BlockEntityItemStackHandler(4,this);
        inventoryOutput = new BlockEntityItemStackHandler(4,this);

        itemHandlerInputs.add(inventoryInput);
        itemHandlerOutputs.add(inventoryOutput);
    }

    @Override
    public void onLoad(){
        super.onLoad();
        myMechanicalBlock.mechanicalOnload();
    }

    public void resetRecipe() {
        currentRecipe = null;
        currentProgress = 0;
    }

    public void scanFornewRecipe() {
        for (SpinningWheelConfig.MachineRecipe r : config.recipes) {
            if(InventoryUtils.hasInputs(itemHandlerInputs, fluidHandlerInputs, List.of(new recipePart(r.inputItem.id,r.inputItem.amount,1)))){
                currentRecipe = r;
                break;
            }
        }
    }

    public InteractionResult use(Player player) {
        return InteractionResult.PASS;
    }

    public void completeCurrentRecipe() {
        for (int i = 0; i < currentRecipe.inputItem.amount; i++) {
            if (level.random.nextFloat() < currentRecipe.inputItem.p) {
                InventoryUtils.consumeElements(fluidHandlerInputs, itemHandlerInputs, currentRecipe.inputItem.id, 1, false);
            }
        }
        for (SpinningWheelConfig.MachineRecipe.Item output : currentRecipe.outputItems) {
            for (int i = 0; i < output.amount; i++) {
                if (level.random.nextFloat() < output.p) {
                    InventoryUtils.createElements(fluidHandlerOutputs, itemHandlerOutputs, output.id, 1);
                }
            }
        }
        resetRecipe();
    }

    public void tick() {
        myMechanicalBlock.mechanicalTick();

        if (currentRecipe == null) {
            scanFornewRecipe();
        }
        if (InventoryUtils.hasInputs(itemHandlerInputs, fluidHandlerInputs, List.of(new recipePart(currentRecipe.inputItem.id, currentRecipe.inputItem.amount, 1)))) {
            double progressMade = Math.abs((float) (Static.rad_to_degree(myMechanicalBlock.internalVelocity) / 360f / Static.TPS));
            currentProgress += progressMade;
            if (currentProgress >= currentRecipe.timeRequired) {
                if(InventoryUtils.canFitElements(itemHandlerOutputs,fluidHandlerOutputs,))
                completeCurrentRecipe();
            }
        } else {
            resetRecipe();
        }
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntitySpinningWheel) t).tick();
    }

    @Override
    public void readServer(CompoundTag compoundTag) {
        myMechanicalBlock.mechanicalReadServer(compoundTag);
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        myMechanicalBlock.mechanicalReadClient(compoundTag);
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
    public AbstractMechanicalBlock getMechanicalBlock(Direction direction) {
        if(direction == getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite()){
            return myMechanicalBlock;
        }
            return null;
    }

    @Override
    public BlockEntity getBlockEntity() {
        return this;
    }
}