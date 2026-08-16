package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipe;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasInjectionChamberOperationState {
    public static final String COMPOUND_KEY_PROCESSING_TICKS = "ProcessingTicks";

    private static final String COMPOUND_KEY_OPERATION_TYPE = "OperationType";
    private static final String COMPOUND_KEY_OPERATION_GAS = "OperationGas";
    private static final String COMPOUND_KEY_OPERATION_FAN_PROCESSING_TYPE = "OperationFanProcessingType";
    private static final String COMPOUND_KEY_OPERATION_INPUT = "OperationInput";
    private static final String COMPOUND_KEY_OPERATION_RESULTS = "OperationResults";
    private static final String COMPOUND_KEY_OPERATION_FLUID_INPUTS = "OperationFluidInputs";
    private static final String COMPOUND_KEY_OPERATION_FLUID_RESULT = "OperationFluidResult";
    private static final String COMPOUND_KEY_OPERATION_RESULT_PREPARED = "OperationResultPrepared";
    private static final String COMPOUND_KEY_OPERATION_EXECUTED = "OperationExecuted";

    public final List<ItemStack> results = new ArrayList<>();
    public final List<FluidStack> fluidInputs = new ArrayList<>();
    public GasStack gas = GasStack.EMPTY;
    public FluidStack fluidResult = FluidStack.EMPTY;
    @Nullable public ResourceLocation fanProcessingTypeId;
    public ItemStack input = ItemStack.EMPTY;
    public boolean resultPrepared;
    public boolean executed;
    @Nullable public GasInjectionRecipe recipe;
    public OperationType type = OperationType.NONE;

    private int processingTicks = -1;
    private int previousProcessingTicks = -1;

    public int getProcessingTicks() {
        return processingTicks;
    }

    public void setProcessingTicks(int ticks) {
        processingTicks = ticks;
    }

    public int getPreviousProcessingTicks() {
        return previousProcessingTicks;
    }

    public void synchronizeProcessingTicks(int synchronizedTicks, boolean clientPacket) {
        if (clientPacket && processingTicks >= 0 && synchronizedTicks >= 0) {
            return;
        }

        processingTicks = synchronizedTicks;
        previousProcessingTicks = synchronizedTicks;
    }

    public void capturePreviousProcessingTicks() {
        previousProcessingTicks = processingTicks;
    }

    public void decrementProcessingTicks() {
        --processingTicks;
    }

    public boolean isRunning() {
        return processingTicks >= 0;
    }

    public boolean isGasLocked() {
        return type.usesGas && processingTicks >= 0 && !executed;
    }

    public void setOperation(OperationType type, ItemStack input, int inputCount, GasStack gas, long requiredAmount, @Nullable GasInjectionRecipe recipe, @Nullable ResourceLocation fanProcessingTypeId) {
        this.type = type;
        this.input = input.copyWithCount(inputCount);
        this.gas = gas.isEmpty() ? GasStack.EMPTY : gas.copyWithAmount(requiredAmount);
        fluidInputs.clear();
        fluidResult = FluidStack.EMPTY;
        this.fanProcessingTypeId = fanProcessingTypeId;
        this.recipe = recipe;
        results.clear();
        resultPrepared = false;
        executed = false;
    }

    public void setBasinOperation(GasStack gas, long requiredAmount, List<FluidStack> fluidInputs, FluidStack result) {
        type = OperationType.BASIN_RECIPE;
        input = ItemStack.EMPTY;
        this.gas = gas.copyWithAmount(requiredAmount);
        this.fluidInputs.clear();
        fluidInputs.forEach(fluidInput -> this.fluidInputs.add(fluidInput.copy()));
        fluidResult = result.copy();
        fanProcessingTypeId = null;
        recipe = null;
        results.clear();
        resultPrepared = true;
        executed = false;
    }

    public void clear() {
        type = OperationType.NONE;
        gas = GasStack.EMPTY;
        fluidInputs.clear();
        fluidResult = FluidStack.EMPTY;
        fanProcessingTypeId = null;
        input = ItemStack.EMPTY;
        results.clear();
        resultPrepared = false;
        recipe = null;
        executed = false;
    }

    public void writeOperation(CompoundTag tag, Provider provider) {
        if (type == OperationType.NONE) {
            return;
        }

        tag.putString(COMPOUND_KEY_OPERATION_TYPE, type.serializedName);
        if (!gas.isEmpty()) {
            tag.put(COMPOUND_KEY_OPERATION_GAS, gas.saveOptional(provider));
        }
        if (fanProcessingTypeId != null) {
            tag.putString(COMPOUND_KEY_OPERATION_FAN_PROCESSING_TYPE, fanProcessingTypeId.toString());
        }
        tag.put(COMPOUND_KEY_OPERATION_INPUT, input.saveOptional(provider));
        if (!fluidInputs.isEmpty()) {
            ListTag inputs = new ListTag();
            for (FluidStack fluidInput : fluidInputs) {
                inputs.add(fluidInput.saveOptional(provider));
            }
            tag.put(COMPOUND_KEY_OPERATION_FLUID_INPUTS, inputs);
        }
        if (!fluidResult.isEmpty()) {
            tag.put(COMPOUND_KEY_OPERATION_FLUID_RESULT, fluidResult.saveOptional(provider));
        }
        tag.putBoolean(COMPOUND_KEY_OPERATION_RESULT_PREPARED, resultPrepared);
        if (resultPrepared) {
            ListTag serializedResults = new ListTag();
            for (ItemStack result : results) {
                serializedResults.add(result.saveOptional(provider));
            }
            tag.put(COMPOUND_KEY_OPERATION_RESULTS, serializedResults);
        }
        tag.putBoolean(COMPOUND_KEY_OPERATION_EXECUTED, executed);
    }

    public boolean readOperation(CompoundTag tag, Provider provider, Predicate<ResourceLocation> fanOperationValidator) {
        recipe = null;
        type = OperationType.byName(tag.getString(COMPOUND_KEY_OPERATION_TYPE));
        gas = tag.contains(COMPOUND_KEY_OPERATION_GAS) ? GasStack.parseOptional(provider, tag.getCompound(COMPOUND_KEY_OPERATION_GAS)) : GasStack.EMPTY;
        fanProcessingTypeId = tag.contains(COMPOUND_KEY_OPERATION_FAN_PROCESSING_TYPE) ? ResourceLocation.tryParse(tag.getString(COMPOUND_KEY_OPERATION_FAN_PROCESSING_TYPE)) : null;
        input = tag.contains(COMPOUND_KEY_OPERATION_INPUT) ? ItemStack.parseOptional(provider, tag.getCompound(COMPOUND_KEY_OPERATION_INPUT)) : ItemStack.EMPTY;

        fluidInputs.clear();
        if (tag.contains(COMPOUND_KEY_OPERATION_FLUID_INPUTS, Tag.TAG_LIST)) {
            ListTag inputs = tag.getList(COMPOUND_KEY_OPERATION_FLUID_INPUTS, Tag.TAG_COMPOUND);
            for (int i = 0; i < inputs.size(); i++) {
                FluidStack fluidInput = FluidStack.parseOptional(provider, inputs.getCompound(i));
                if (!fluidInput.isEmpty()) {
                    fluidInputs.add(fluidInput);
                }
            }
        }

        fluidResult = tag.contains(COMPOUND_KEY_OPERATION_FLUID_RESULT) ? FluidStack.parseOptional(provider, tag.getCompound(COMPOUND_KEY_OPERATION_FLUID_RESULT)) : FluidStack.EMPTY;
        resultPrepared = tag.getBoolean(COMPOUND_KEY_OPERATION_RESULT_PREPARED);
        results.clear();
        if (resultPrepared && tag.contains(COMPOUND_KEY_OPERATION_RESULTS, Tag.TAG_LIST)) {
            ListTag serializedResults = tag.getList(COMPOUND_KEY_OPERATION_RESULTS, Tag.TAG_COMPOUND);
            for (int i = 0; i < serializedResults.size(); i++) {
                ItemStack result = ItemStack.parseOptional(provider, serializedResults.getCompound(i));
                if (!result.isEmpty()) {
                    results.add(result);
                }
            }
        }
        executed = tag.getBoolean(COMPOUND_KEY_OPERATION_EXECUTED);
        return isLoadedOperationValid(fanOperationValidator);
    }

    private boolean isLoadedOperationValid(Predicate<ResourceLocation> fanOperationValidator) {
        if (type == OperationType.NONE) {
            return false;
        }

        if (type == OperationType.BASIN_RECIPE) {
            return !gas.isEmpty() && !fluidInputs.isEmpty() && !fluidResult.isEmpty();
        }

        if (input.isEmpty()) {
            return false;
        }

        if (type != OperationType.FAN_PROCESSING) {
            return !type.usesGas || !gas.isEmpty();
        }
        return !gas.isEmpty() && fanProcessingTypeId != null && GasInjectionChamberUtils.getFanProcessingType(fanProcessingTypeId).isPresent() && fanOperationValidator.test(fanProcessingTypeId);
    }

    public enum OperationType {
        NONE("none", false),
        ITEM_RECIPE("recipe", true),
        BASIN_RECIPE("basin_recipe", true),
        CANISTER("canister", true),
        FAN_PROCESSING("fan_processing", true);

        public final String serializedName;
        public final boolean usesGas;

        OperationType(String serializedName, boolean usesGas) {
            this.serializedName = serializedName;
            this.usesGas = usesGas;
        }

        private static OperationType byName(String name) {
            for (OperationType type : values()) {
                if (type.serializedName.equals(name)) {
                    return type;
                }
            }
            return NONE;
        }
    }
}
