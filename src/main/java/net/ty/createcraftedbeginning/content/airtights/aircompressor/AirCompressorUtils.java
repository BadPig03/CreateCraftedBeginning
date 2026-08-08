package net.ty.createcraftedbeginning.content.airtights.aircompressor;

import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.coolantshandlers.AirtightCoolantHandler;
import net.ty.createcraftedbeginning.api.coolantshandlers.AirtightCoolantHandlerUtils;
import net.ty.createcraftedbeginning.api.coolantshandlers.CoolantEfficiency;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmountUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightcheckvalve.AirtightCheckValveBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlock;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.content.airtights.smartairtightpipe.SmartAirtightPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.transaction.MachineResourceSnapshots;
import net.ty.createcraftedbeginning.core.transaction.ResourceTransaction;
import net.ty.createcraftedbeginning.recipe.PressurizationRecipe;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirCompressorUtils {
    public static final int LAZY_TICK_RATE = 5;

    private static final String COMPOUND_KEY_STORED_HEAT = "StoredHeat";
    private static final String COMPOUND_KEY_COOLANT_EFFICIENCY = "CoolantEfficiency";
    private static final String COMPOUND_KEY_OVERHEAT_STATE = "OverheatState";

    private AirCompressorUtils() {
    }

    public static int getNextOverheatThreshold() {
        return Mth.clamp(CCBConfig.server().airtights.nextOverheatThreshold.get(), 1, Integer.MAX_VALUE / OverheatState.MELTDOWN.ordinal());
    }

    public static long getMaxCapacity() {
        return CCBConfig.server().airtights.maxAirCompressorCapacity.get() * GasAmountUtils.MILLIBUCKETS_PER_BUCKET;
    }

    public static int getMaxStoredHeat() {
        return getNextOverheatThreshold() * OverheatState.MELTDOWN.ordinal();
    }

    public static int clampStoredHeat(int storedHeat) {
        return Math.clamp(storedHeat, 0, getMaxStoredHeat());
    }

    public static int getNextStateHeat(OverheatState state) {
        if (state == OverheatState.MELTDOWN) {
            return getMaxStoredHeat();
        }
        return clampStoredHeat((state.ordinal() + 1) * getNextOverheatThreshold());
    }

    public static OverheatState getOverheatState(int storedHeat) {
        return OverheatState.fromStoredHeat(storedHeat, getNextOverheatThreshold());
    }

    public static BlockState getStateForBasicPlacement(BlockPlaceContext context, BlockState state) {
        Direction oppositeFacing = context.getHorizontalDirection().getOpposite();
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            return state.setValue(AirCompressorBlock.HORIZONTAL_FACING, oppositeFacing);
        }

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction clickedSide = context.getClickedFace().getOpposite();
        BlockState neighborState = level.getBlockState(pos.relative(clickedSide));
        Block neighborBlock = neighborState.getBlock();
        return switch (neighborBlock) {
            case AirtightPumpBlock ignored -> getStateForPumpPlacement(state, neighborState, oppositeFacing);
            case AirtightPipeBlock ignored -> getStateForPipePlacement(state, neighborState, clickedSide, oppositeFacing);
            case SmartAirtightPipeBlock ignored -> getStateForPipePlacement(state, neighborState, clickedSide, oppositeFacing);
            case AirtightCheckValveBlock ignored -> getStateForCheckValvePlacement(state, neighborState, oppositeFacing);
            default -> state.setValue(AirCompressorBlock.HORIZONTAL_FACING, oppositeFacing);
        };
    }

    @Nullable
    public static CompressionPlan createCompressionPlan(Level level, GasStack input) {
        if (input.isEmpty()) {
            return null;
        }

        PressurizationRecipe recipe = PressurizationRecipe.findRecipe(level, input).orElse(null);
        if (recipe == null) {
            return null;
        }

        long inputPerBatch = recipe.getGasIngredient().amount();
        GasStack outputPerBatch = recipe.getGasResult().copy();
        if (inputPerBatch <= 0 || outputPerBatch.isEmpty() || outputPerBatch.getAmount() <= 0) {
            return null;
        }
        return new CompressionPlan(recipe, inputPerBatch, outputPerBatch);
    }

    public static boolean canOperate(@Nullable CompressionPlan plan, boolean overStressed, float speed, OverheatState overheatState, SmartGasTankBehaviour inputTankBehaviour, SmartGasTankBehaviour outputTankBehaviour) {
        if (plan == null || overStressed) {
            return false;
        }

        if (Mth.abs(speed) < SpeedLevel.MEDIUM.getSpeedValue()) {
            return false;
        }

        if (overheatState == OverheatState.MELTDOWN) {
            return false;
        }

        GasStack input = inputTankBehaviour.getPrimaryHandler().getGasStack();
        if (input.getAmount() < plan.inputPerBatch()) {
            return false;
        }

        GasStack output = outputTankBehaviour.getPrimaryHandler().getGasStack();
        boolean hasCompatibleOutput = output.isEmpty() || GasStack.isSameGasSameComponents(output, plan.outputPerBatch());
        return hasCompatibleOutput && outputTankBehaviour.getPrimaryHandler().getSpace() >= plan.outputPerBatch().getAmount();
    }

    public static WorkState accumulateWork(WorkState workState, CompressionPlan plan, float speed, OverheatState overheatState) {
        long accumulatedWork = workState.recipe() == plan.recipe() ? workState.accumulatedWork() : 0;
        float scaledWork = Mth.abs(speed) * getPressurizationRateMultiplier() * overheatState.getEfficiencyPercent();
        long addedWork = Math.max(0, Mth.floor(scaledWork));
        long updatedWork = addedWork >= Long.MAX_VALUE - accumulatedWork ? Long.MAX_VALUE : accumulatedWork + addedWork;
        return new WorkState(plan.recipe(), updatedWork);
    }

    public static WorkState pressurize(WorkState workState, CompressionPlan plan, SmartGasTankBehaviour inputTankBehaviour, SmartGasTankBehaviour outputTankBehaviour) {
        long accumulatedWork = workState.recipe() == plan.recipe() ? workState.accumulatedWork() : 0;
        long batchesByWork = accumulatedWork / 100 / plan.inputPerBatch();
        long batchesByInput = inputTankBehaviour.getPrimaryHandler().getGasAmount() / plan.inputPerBatch();
        long batchesByOutput = outputTankBehaviour.getPrimaryHandler().getSpace() / plan.outputPerBatch().getAmount();
        long batches = Math.min(batchesByWork, Math.min(batchesByInput, batchesByOutput));
        if (batches <= 0) {
            return new WorkState(plan.recipe(), accumulatedWork);
        }

        long totalInput = batches * plan.inputPerBatch();
        long totalOutput = batches * plan.outputPerBatch().getAmount();
        GasStack outputStack = plan.outputPerBatch().copyWithAmount(totalOutput);
        ResourceTransaction transaction = new ResourceTransaction().add(ResourceTransaction.participant(() -> {
            GasStack simulatedDrain = inputTankBehaviour.getInternalGasHandler().forceDrain(totalInput, GasAction.SIMULATE);
            long simulatedFill = outputTankBehaviour.getInternalGasHandler().forceFill(outputStack, GasAction.SIMULATE);
            return simulatedDrain.getAmount() == totalInput && plan.recipe().getGasIngredient().ingredient().test(simulatedDrain) && simulatedFill == totalOutput;
        }, () -> MachineResourceSnapshots.snapshotGasContents(inputTankBehaviour, outputTankBehaviour), () -> {
            GasStack drained = inputTankBehaviour.getInternalGasHandler().forceDrain(totalInput, GasAction.EXECUTE);
            return drained.getAmount() == totalInput && plan.recipe().getGasIngredient().ingredient().test(drained) && outputTankBehaviour.getInternalGasHandler().forceFill(outputStack, GasAction.EXECUTE) == totalOutput;
        }, snapshot -> MachineResourceSnapshots.restoreGasContents(snapshot, inputTankBehaviour, outputTankBehaviour)));
        if (!transaction.commit()) {
            return new WorkState(plan.recipe(), accumulatedWork);
        }

        long remainingWork = accumulatedWork - totalInput * 100;
        return new WorkState(plan.recipe(), remainingWork);
    }

    public static int updateStoredHeat(int storedHeat, float speed, boolean operating, CoolantEfficiency coolantEfficiency, Level level) {
        int netHeat = getHeatAdded(speed, operating) - coolantEfficiency.getHeatReduced(level);
        long updatedHeat = (long) storedHeat + netHeat;
        return (int) Math.max(0, Math.min(updatedHeat, getMaxStoredHeat()));
    }

    public static int readStoredHeat(ItemStack compressor) {
        if (!compressor.has(CCBDataComponents.COMPRESSOR_STORED_HEAT)) {
            return inferStoredHeat(OverheatState.fromItem(compressor));
        }
        return clampStoredHeat(compressor.getOrDefault(CCBDataComponents.COMPRESSOR_STORED_HEAT, 0));
    }

    public static void saveToItem(ItemStack compressor, OverheatState overheatState, int storedHeat) {
        compressor.set(CCBDataComponents.COMPRESSOR_OVERHEAT_STATE, overheatState.getSerializedName());
        compressor.set(CCBDataComponents.COMPRESSOR_STORED_HEAT, clampStoredHeat(storedHeat));
    }

    public static void writeData(CompoundTag tag, OverheatState overheatState, int storedHeat, CoolantEfficiency coolantEfficiency, boolean clientPacket) {
        tag.putString(COMPOUND_KEY_OVERHEAT_STATE, overheatState.getSerializedName());
        if (clientPacket) {
            return;
        }

        tag.putInt(COMPOUND_KEY_STORED_HEAT, clampStoredHeat(storedHeat));
        tag.putString(COMPOUND_KEY_COOLANT_EFFICIENCY, coolantEfficiency.getSerializedName());
    }

    public static OverheatState readOverheatState(CompoundTag tag) {
        if (!tag.contains(COMPOUND_KEY_OVERHEAT_STATE)) {
            return OverheatState.NORMAL;
        }
        return OverheatState.fromName(tag.getString(COMPOUND_KEY_OVERHEAT_STATE));
    }

    public static int readStoredHeat(CompoundTag tag, OverheatState savedState) {
        if (!tag.contains(COMPOUND_KEY_STORED_HEAT)) {
            return inferStoredHeat(savedState);
        }
        return clampStoredHeat(tag.getInt(COMPOUND_KEY_STORED_HEAT));
    }

    public static CoolantEfficiency readCoolantEfficiency(CompoundTag tag) {
        if (!tag.contains(COMPOUND_KEY_COOLANT_EFFICIENCY)) {
            return CoolantEfficiency.NONE;
        }
        return CoolantEfficiency.fromName(tag.getString(COMPOUND_KEY_COOLANT_EFFICIENCY));
    }

    public static CoolantEfficiency getCoolantEfficiency(Level level, BlockPos coolantPos) {
        BlockState coolantState = level.getBlockState(coolantPos);
        AirtightCoolantHandler coolantHandler = AirtightCoolantHandlerUtils.of(coolantState.getBlock());
        return coolantHandler.getCoolantEfficiency(level, coolantPos, coolantState);
    }

    public static CoolantEfficiency tickCoolant(ServerLevel level, BlockPos coolantPos, boolean shouldConsume, RandomSource random) {
        BlockState coolantState = level.getBlockState(coolantPos);
        AirtightCoolantHandler coolantHandler = AirtightCoolantHandlerUtils.of(coolantState.getBlock());
        CoolantEfficiency efficiency = coolantHandler.getCoolantEfficiency(level, coolantPos, coolantState);
        float consumeChance = Mth.clamp(CCBConfig.server().airtights.coolantConsumptionChance.getF(), 0, 1);
        if (efficiency == CoolantEfficiency.NONE || !shouldConsume || random.nextFloat() >= consumeChance) {
            return efficiency;
        }

        BlockState meltedState = coolantHandler.getMeltBlockState(level, coolantPos, coolantState);
        if (meltedState == null || meltedState.equals(coolantState)) {
            return getCoolantEfficiency(level, coolantPos);
        }

        if (meltedState.isAir()) {
            level.removeBlock(coolantPos, false);
        }
        else {
            level.setBlockAndUpdate(coolantPos, meltedState);
        }
        return getCoolantEfficiency(level, coolantPos);
    }

    public static void updateOperatingBlockState(Level level, BlockPos pos, BlockState state, boolean operating) {
        if (level.isClientSide || !state.hasProperty(AirCompressorBlock.ACTIVE) || state.getValue(AirCompressorBlock.ACTIVE) == operating) {
            return;
        }

        level.setBlock(pos, state.setValue(AirCompressorBlock.ACTIVE, operating), Block.UPDATE_CLIENTS);
    }

    private static BlockState getStateForPumpPlacement(BlockState state, BlockState pumpState, Direction oppositeFacing) {
        Direction facing = pumpState.getValue(AirtightPumpBlock.FACING);
        if (facing.getAxis() == Axis.Y) {
            return state.setValue(AirCompressorBlock.HORIZONTAL_FACING, oppositeFacing);
        }
        return state.setValue(AirCompressorBlock.HORIZONTAL_FACING, facing.getClockWise());
    }

    private static BlockState getStateForPipePlacement(BlockState state, BlockState pipeState, Direction clickedSide, Direction oppositeFacing) {
        Axis axis = pipeState.getValue(AirCompressorBlock.AXIS);
        boolean reverse = clickedSide.getAxisDirection() == AxisDirection.NEGATIVE;
        return switch (axis) {
            case X -> state.setValue(AirCompressorBlock.HORIZONTAL_FACING, reverse ? Direction.SOUTH : Direction.NORTH);
            case Y -> state.setValue(AirCompressorBlock.HORIZONTAL_FACING, oppositeFacing);
            case Z -> state.setValue(AirCompressorBlock.HORIZONTAL_FACING, reverse ? Direction.EAST : Direction.WEST);
        };
    }

    private static BlockState getStateForCheckValvePlacement(BlockState state, BlockState valveState, Direction oppositeFacing) {
        boolean inverted = valveState.getValue(AirtightCheckValveBlock.INVERTED);
        return switch (valveState.getValue(AirtightCheckValveBlock.AXIS)) {
            case X -> state.setValue(AirCompressorBlock.HORIZONTAL_FACING, inverted ? Direction.NORTH : Direction.SOUTH);
            case Y -> state.setValue(AirCompressorBlock.HORIZONTAL_FACING, oppositeFacing);
            case Z -> state.setValue(AirCompressorBlock.HORIZONTAL_FACING, inverted ? Direction.WEST : Direction.EAST);
        };
    }

    private static float getPressurizationRateMultiplier() {
        return CCBConfig.server().airtights.pressurizationRateMultiplier.getF();
    }

    private static int getHeatAdded(float speed, boolean operating) {
        if (!operating) {
            return 0;
        }

        float mediumSpeed = Math.max(1, SpeedLevel.MEDIUM.getSpeedValue());
        float maximumSpeed = Math.max(mediumSpeed, AllConfigs.server().kinetics.maxRotationSpeed.get());
        float progress = maximumSpeed == mediumSpeed ? 1 : Mth.clamp((Mth.abs(speed) - mediumSpeed) / (maximumSpeed - mediumSpeed), 0, 1);
        return Mth.floor(Mth.lerp(progress, 3, 5) + 0.5f);
    }

    private static int inferStoredHeat(OverheatState savedState) {
        if (savedState == OverheatState.NORMAL) {
            return 0;
        }

        if (savedState == OverheatState.MELTDOWN) {
            return getMaxStoredHeat();
        }

        int threshold = getNextOverheatThreshold();
        return clampStoredHeat(savedState.ordinal() * threshold + threshold / 2);
    }

    public record CompressionPlan(PressurizationRecipe recipe, long inputPerBatch, GasStack outputPerBatch) {}

    public record WorkState(@Nullable PressurizationRecipe recipe, long accumulatedWork) {
        public static final WorkState EMPTY = new WorkState(null, 0);

        public WorkState {
            accumulatedWork = Math.max(0, accumulatedWork);
        }
    }
}
