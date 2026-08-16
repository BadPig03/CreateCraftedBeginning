package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightForgingPressSerialization {
    private static final String COMPOUND_KEY_CORE = "Core";
    private static final String COMPOUND_KEY_FILTER = "Filter";
    private static final String COMPOUND_KEY_INPUT_ITEMS = "InputItems";
    private static final String COMPOUND_KEY_OPERATING = "Operating";
    private static final String COMPOUND_KEY_OPERATING_TICKS = "OperatingTicks";
    private static final String COMPOUND_KEY_OUTPUT_ITEMS = "OutputItems";
    private static final String COMPOUND_KEY_PRESS_HEAD_ITEMS = "PressHeadItems";
    private static final String COMPOUND_KEY_PROCESSING_ITEMS = "ProcessingItems";

    private final AirtightForgingPressBlockEntity press;
    private final AirtightForgingPressController controller;

    public AirtightForgingPressSerialization(AirtightForgingPressBlockEntity press, AirtightForgingPressController controller) {
        this.press = press;
        this.controller = controller;
    }

    public void write(CompoundTag tag, Provider provider) {
        tag.put(COMPOUND_KEY_CORE, press.getCore().write());
        tag.put(COMPOUND_KEY_PRESS_HEAD_ITEMS, press.getPressHeadInventory().serializeNBT(provider));
        tag.put(COMPOUND_KEY_PROCESSING_ITEMS, press.getAdditionInventory().serializeNBT(provider));
        tag.put(COMPOUND_KEY_FILTER, press.getRecipeFilter().saveOptional(provider));
        tag.put(COMPOUND_KEY_INPUT_ITEMS, press.getInputInventory().serializeNBT(provider));
        tag.put(COMPOUND_KEY_OUTPUT_ITEMS, press.getOutputInventory().serializeNBT(provider));
        tag.putFloat(COMPOUND_KEY_OPERATING_TICKS, controller.getOperatingTicks());
        tag.putBoolean(COMPOUND_KEY_OPERATING, controller.isOperating());
    }

    public void read(CompoundTag tag, Provider provider, boolean clientPacket) {
        if (tag.contains(COMPOUND_KEY_CORE)) {
            press.getCore().read(tag.getCompound(COMPOUND_KEY_CORE));
        }
        if (tag.contains(COMPOUND_KEY_PRESS_HEAD_ITEMS)) {
            press.getPressHeadInventory().deserializeNBT(provider, tag.getCompound(COMPOUND_KEY_PRESS_HEAD_ITEMS));
        }
        if (tag.contains(COMPOUND_KEY_PROCESSING_ITEMS)) {
            press.getAdditionInventory().deserializeNBT(provider, tag.getCompound(COMPOUND_KEY_PROCESSING_ITEMS));
        }
        press.loadRecipeFilter(tag.contains(COMPOUND_KEY_FILTER) ? ItemStack.parseOptional(provider, tag.getCompound(COMPOUND_KEY_FILTER)) : ItemStack.EMPTY);
        if (tag.contains(COMPOUND_KEY_INPUT_ITEMS)) {
            press.getInputInventory().deserializeNBT(provider, tag.getCompound(COMPOUND_KEY_INPUT_ITEMS));
        }
        if (tag.contains(COMPOUND_KEY_OUTPUT_ITEMS)) {
            press.getOutputInventory().deserializeNBT(provider, tag.getCompound(COMPOUND_KEY_OUTPUT_ITEMS));
        }

        float operatingTicks = tag.contains(COMPOUND_KEY_OPERATING_TICKS) ? tag.getFloat(COMPOUND_KEY_OPERATING_TICKS) : controller.getOperatingTicks();
        boolean operating = tag.contains(COMPOUND_KEY_OPERATING) ? tag.getBoolean(COMPOUND_KEY_OPERATING) : controller.isOperating();
        controller.loadOperationState(operating, operatingTicks, clientPacket);
    }
}
