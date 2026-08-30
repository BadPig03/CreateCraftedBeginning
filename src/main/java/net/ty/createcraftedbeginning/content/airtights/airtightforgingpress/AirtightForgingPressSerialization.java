package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.foundation.CCBNbtUtils;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightForgingPressSerialization {
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

    AirtightForgingPressSerialization(AirtightForgingPressBlockEntity press, AirtightForgingPressController controller) {
        this.press = press;
        this.controller = controller;
    }

    void write(CompoundTag compoundTag, Provider provider) {
        CCBNbtUtils.putTag(compoundTag, COMPOUND_KEY_CORE, press.getCore().write());
        CCBNbtUtils.putTag(compoundTag, COMPOUND_KEY_PRESS_HEAD_ITEMS, press.getPressHeadInventory().serializeNBT(provider));
        CCBNbtUtils.putTag(compoundTag, COMPOUND_KEY_PROCESSING_ITEMS, press.getAdditionInventory().serializeNBT(provider));
        CCBNbtUtils.putTag(compoundTag, COMPOUND_KEY_FILTER, press.getRecipeFilter().saveOptional(provider));
        CCBNbtUtils.putTag(compoundTag, COMPOUND_KEY_INPUT_ITEMS, press.getInputInventory().serializeNBT(provider));
        CCBNbtUtils.putTag(compoundTag, COMPOUND_KEY_OUTPUT_ITEMS, press.getOutputInventory().serializeNBT(provider));
        CCBNbtUtils.putFloat(compoundTag, COMPOUND_KEY_OPERATING_TICKS, controller.getOperatingTicks());
        CCBNbtUtils.putBoolean(compoundTag, COMPOUND_KEY_OPERATING, controller.isOperating());
    }

    void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        if (CCBNbtUtils.contains(compoundTag, COMPOUND_KEY_CORE)) {
            press.getCore().read(CCBNbtUtils.getCompound(compoundTag, COMPOUND_KEY_CORE));
        }
        if (CCBNbtUtils.contains(compoundTag, COMPOUND_KEY_PRESS_HEAD_ITEMS)) {
            press.getPressHeadInventory().deserializeNBT(provider, CCBNbtUtils.getCompound(compoundTag, COMPOUND_KEY_PRESS_HEAD_ITEMS));
        }
        if (CCBNbtUtils.contains(compoundTag, COMPOUND_KEY_PROCESSING_ITEMS)) {
            press.getAdditionInventory().deserializeNBT(provider, CCBNbtUtils.getCompound(compoundTag, COMPOUND_KEY_PROCESSING_ITEMS));
        }
        press.loadRecipeFilter(CCBNbtUtils.contains(compoundTag, COMPOUND_KEY_FILTER) ? ItemStack.parseOptional(provider, CCBNbtUtils.getCompound(compoundTag, COMPOUND_KEY_FILTER)) : ItemStack.EMPTY);
        if (CCBNbtUtils.contains(compoundTag, COMPOUND_KEY_INPUT_ITEMS)) {
            press.getInputInventory().deserializeNBT(provider, CCBNbtUtils.getCompound(compoundTag, COMPOUND_KEY_INPUT_ITEMS));
        }
        if (CCBNbtUtils.contains(compoundTag, COMPOUND_KEY_OUTPUT_ITEMS)) {
            press.getOutputInventory().deserializeNBT(provider, CCBNbtUtils.getCompound(compoundTag, COMPOUND_KEY_OUTPUT_ITEMS));
        }

        float operatingTicks = CCBNbtUtils.getFloatOrDefault(compoundTag, COMPOUND_KEY_OPERATING_TICKS, controller.getOperatingTicks());
        boolean operating = CCBNbtUtils.getBooleanOrDefault(compoundTag, COMPOUND_KEY_OPERATING, controller.isOperating());
        controller.loadOperationState(operating, operatingTicks, clientPacket);
    }
}
