package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.foundation.NbtValues;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightReactorKettleSerialization {
    private static final String COMPOUND_KEY_CORE = "Core";
    private static final String COMPOUND_KEY_FILTER = "Filter";
    private static final String COMPOUND_KEY_INPUT_ITEMS = "InputItems";
    private static final String COMPOUND_KEY_OPEN_STATE = "OpenState";
    private static final String COMPOUND_KEY_OPERATING = "Operating";
    private static final String COMPOUND_KEY_OPERATING_TICKS = "OperatingTicks";
    private static final String COMPOUND_KEY_OUTPUT_ITEMS = "OutputItems";
    private static final String COMPOUND_KEY_PROCESSING_TICKS = "ProcessingTicks";
    private static final String COMPOUND_KEY_OPERATION_KINETIC_SPEED = "OperationKineticSpeed";
    private static final String COMPOUND_KEY_PROCESSING_KINETIC_SPEED = "ProcessingKineticSpeed";
    private static final String COMPOUND_KEY_PRESSURE_SPEED_MULTIPLIER = "PressureSpeedMultiplier";
    private static final String COMPOUND_KEY_OPERATION_CYCLE = "OperationCycle";
    private static final String COMPOUND_KEY_PROCESSING_CYCLE = "ProcessingCycle";

    private final AirtightReactorKettleBlockEntity kettle;
    private final AirtightReactorKettleController controller;

    AirtightReactorKettleSerialization(AirtightReactorKettleBlockEntity kettle, AirtightReactorKettleController controller) {
        this.kettle = kettle;
        this.controller = controller;
    }

    void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        compoundTag.put(COMPOUND_KEY_CORE, kettle.getCore().write());
        compoundTag.put(COMPOUND_KEY_FILTER, kettle.getRecipeFilter().saveOptional(provider));
        compoundTag.put(COMPOUND_KEY_INPUT_ITEMS, kettle.getInputInventory().serializeNBT(provider));
        compoundTag.put(COMPOUND_KEY_OUTPUT_ITEMS, kettle.getOutputInventory().serializeNBT(provider));
        if (!clientPacket) {
            return;
        }

        compoundTag.putInt(COMPOUND_KEY_OPERATING_TICKS, controller.getOperatingTicks());
        compoundTag.putInt(COMPOUND_KEY_PROCESSING_TICKS, controller.getProcessingTicks());
        compoundTag.putFloat(COMPOUND_KEY_OPERATION_KINETIC_SPEED, controller.getOperationKineticSpeed());
        compoundTag.putFloat(COMPOUND_KEY_PROCESSING_KINETIC_SPEED, controller.getProcessingKineticSpeed());
        compoundTag.putFloat(COMPOUND_KEY_PRESSURE_SPEED_MULTIPLIER, controller.getPressureSpeedMultiplier());
        compoundTag.putInt(COMPOUND_KEY_OPERATION_CYCLE, controller.getOperationCycle());
        compoundTag.putInt(COMPOUND_KEY_PROCESSING_CYCLE, controller.getProcessingCycle());
        compoundTag.putBoolean(COMPOUND_KEY_OPERATING, controller.isOperating());
        compoundTag.putBoolean(COMPOUND_KEY_OPEN_STATE, controller.getWindowsOpenState());
    }

    void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        if (compoundTag.contains(COMPOUND_KEY_CORE)) {
            kettle.getCore().read(compoundTag.getCompound(COMPOUND_KEY_CORE));
        }
        boolean hasSerializedFilter = compoundTag.contains(COMPOUND_KEY_FILTER);
        kettle.loadRecipeFilter(hasSerializedFilter ? ItemStack.parseOptional(provider, compoundTag.getCompound(COMPOUND_KEY_FILTER)) : ItemStack.EMPTY, hasSerializedFilter);
        if (compoundTag.contains(COMPOUND_KEY_INPUT_ITEMS)) {
            kettle.getInputInventory().deserializeNBT(provider, compoundTag.getCompound(COMPOUND_KEY_INPUT_ITEMS));
        }
        if (compoundTag.contains(COMPOUND_KEY_OUTPUT_ITEMS)) {
            kettle.getOutputInventory().deserializeNBT(provider, compoundTag.getCompound(COMPOUND_KEY_OUTPUT_ITEMS));
        }

        int operatingTicks = NbtValues.getIntOrDefault(compoundTag, COMPOUND_KEY_OPERATING_TICKS, controller.getOperatingTicks());
        int processingTicks = NbtValues.getIntOrDefault(compoundTag, COMPOUND_KEY_PROCESSING_TICKS, controller.getProcessingTicks());
        float operationKineticSpeed = NbtValues.getFloatOrDefault(compoundTag, COMPOUND_KEY_OPERATION_KINETIC_SPEED, controller.getOperationKineticSpeed());
        float processingKineticSpeed = NbtValues.getFloatOrDefault(compoundTag, COMPOUND_KEY_PROCESSING_KINETIC_SPEED, controller.getProcessingKineticSpeed());
        float pressureSpeedMultiplier = NbtValues.getFloatOrDefault(compoundTag, COMPOUND_KEY_PRESSURE_SPEED_MULTIPLIER, controller.getPressureSpeedMultiplier());
        int operationCycle = NbtValues.getIntOrDefault(compoundTag, COMPOUND_KEY_OPERATION_CYCLE, controller.getOperationCycle());
        int processingCycle = NbtValues.getIntOrDefault(compoundTag, COMPOUND_KEY_PROCESSING_CYCLE, controller.getProcessingCycle());
        boolean isOperating = NbtValues.getBooleanOrDefault(compoundTag, COMPOUND_KEY_OPERATING, controller.isOperating());
        boolean windowsOpen = NbtValues.getBooleanOrDefault(compoundTag, COMPOUND_KEY_OPEN_STATE, controller.getWindowsOpenState());
        controller.loadOperationState(isOperating, operatingTicks, processingTicks, windowsOpen, operationKineticSpeed, processingKineticSpeed, pressureSpeedMultiplier, operationCycle, processingCycle, clientPacket);
    }
}
