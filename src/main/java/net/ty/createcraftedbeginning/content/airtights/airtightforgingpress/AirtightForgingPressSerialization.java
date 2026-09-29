package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.foundation.NbtValues;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightForgingPressSerialization {
    private static final String COMPOUND_KEY_CORE = "Core";
    private static final String COMPOUND_KEY_FILTER = "Filter";
    private static final String COMPOUND_KEY_INPUT_ITEMS = "InputItems";
    private static final String COMPOUND_KEY_OPERATING = "Operating";
    private static final String COMPOUND_KEY_OPERATING_TICKS = "OperatingTicks";
    private static final String COMPOUND_KEY_PRESSURE_SPEED_MULTIPLIER = "PressureSpeedMultiplier";
    private static final String COMPOUND_KEY_OPERATION_SPEED = "OperationSpeed";
    private static final String COMPOUND_KEY_OPERATION_CYCLE = "OperationCycle";
    private static final String COMPOUND_KEY_OUTPUT_ITEMS = "OutputItems";
    private static final String COMPOUND_KEY_PRESS_HEAD_ITEMS = "PressHeadItems";
    private static final String COMPOUND_KEY_PROCESSING_ITEMS = "ProcessingItems";

    private final AirtightForgingPressBlockEntity press;
    private final AirtightForgingPressController controller;

    AirtightForgingPressSerialization(AirtightForgingPressBlockEntity press, AirtightForgingPressController controller) {
        this.press = press;
        this.controller = controller;
    }

    void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        compoundTag.put(COMPOUND_KEY_CORE, press.getCore().write());
        compoundTag.put(COMPOUND_KEY_PRESS_HEAD_ITEMS, press.getPressHeadInventory().serializeNBT(provider));
        compoundTag.put(COMPOUND_KEY_PROCESSING_ITEMS, press.getAdditionInventory().serializeNBT(provider));
        compoundTag.put(COMPOUND_KEY_FILTER, press.getRecipeFilter().saveOptional(provider));
        compoundTag.put(COMPOUND_KEY_INPUT_ITEMS, press.getInputInventory().serializeNBT(provider));
        compoundTag.put(COMPOUND_KEY_OUTPUT_ITEMS, press.getOutputInventory().serializeNBT(provider));
        if (!clientPacket) {
            return;
        }

        compoundTag.putFloat(COMPOUND_KEY_OPERATING_TICKS, controller.getOperatingTicks());
        compoundTag.putFloat(COMPOUND_KEY_PRESSURE_SPEED_MULTIPLIER, controller.getPressureSpeedMultiplier());
        compoundTag.putFloat(COMPOUND_KEY_OPERATION_SPEED, controller.getOperationSpeed());
        compoundTag.putInt(COMPOUND_KEY_OPERATION_CYCLE, controller.getOperationCycle());
        compoundTag.putBoolean(COMPOUND_KEY_OPERATING, controller.isOperating());
    }

    void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        if (compoundTag.contains(COMPOUND_KEY_CORE)) {
            press.getCore().read(compoundTag.getCompound(COMPOUND_KEY_CORE));
        }
        if (compoundTag.contains(COMPOUND_KEY_PRESS_HEAD_ITEMS)) {
            press.getPressHeadInventory().deserializeNBT(provider, compoundTag.getCompound(COMPOUND_KEY_PRESS_HEAD_ITEMS));
        }
        if (compoundTag.contains(COMPOUND_KEY_PROCESSING_ITEMS)) {
            press.getAdditionInventory().deserializeNBT(provider, compoundTag.getCompound(COMPOUND_KEY_PROCESSING_ITEMS));
        }
        press.loadRecipeFilter(compoundTag.contains(COMPOUND_KEY_FILTER) ? ItemStack.parseOptional(provider, compoundTag.getCompound(COMPOUND_KEY_FILTER)) : ItemStack.EMPTY);
        if (compoundTag.contains(COMPOUND_KEY_INPUT_ITEMS)) {
            press.getInputInventory().deserializeNBT(provider, compoundTag.getCompound(COMPOUND_KEY_INPUT_ITEMS));
        }
        if (compoundTag.contains(COMPOUND_KEY_OUTPUT_ITEMS)) {
            press.getOutputInventory().deserializeNBT(provider, compoundTag.getCompound(COMPOUND_KEY_OUTPUT_ITEMS));
        }

        float operatingTicks = NbtValues.getFloatOrDefault(compoundTag, COMPOUND_KEY_OPERATING_TICKS, controller.getOperatingTicks());
        float pressureSpeedMultiplier = NbtValues.getFloatOrDefault(compoundTag, COMPOUND_KEY_PRESSURE_SPEED_MULTIPLIER, controller.getPressureSpeedMultiplier());
        float operationSpeed = NbtValues.getFloatOrDefault(compoundTag, COMPOUND_KEY_OPERATION_SPEED, controller.getOperationSpeed());
        int operationCycle = NbtValues.getIntOrDefault(compoundTag, COMPOUND_KEY_OPERATION_CYCLE, controller.getOperationCycle());
        boolean operating = NbtValues.getBooleanOrDefault(compoundTag, COMPOUND_KEY_OPERATING, controller.isOperating());
        controller.loadOperationState(operating, operatingTicks, pressureSpeedMultiplier, operationSpeed, operationCycle, clientPacket);
    }
}
