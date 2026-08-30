package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.foundation.CCBNbtUtils;

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

    private final AirtightReactorKettleBlockEntity kettle;
    private final AirtightReactorKettleController controller;

    AirtightReactorKettleSerialization(AirtightReactorKettleBlockEntity kettle, AirtightReactorKettleController controller) {
        this.kettle = kettle;
        this.controller = controller;
    }

    void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        CCBNbtUtils.putTag(compoundTag, COMPOUND_KEY_CORE, kettle.getCore().write());
        CCBNbtUtils.putTag(compoundTag, COMPOUND_KEY_FILTER, kettle.getRecipeFilter().saveOptional(provider));
        CCBNbtUtils.putTag(compoundTag, COMPOUND_KEY_INPUT_ITEMS, kettle.getInputInventory().serializeNBT(provider));
        CCBNbtUtils.putTag(compoundTag, COMPOUND_KEY_OUTPUT_ITEMS, kettle.getOutputInventory().serializeNBT(provider));
        if (!clientPacket) {
            return;
        }

        CCBNbtUtils.putInt(compoundTag, COMPOUND_KEY_OPERATING_TICKS, controller.getOperatingTicks());
        CCBNbtUtils.putInt(compoundTag, COMPOUND_KEY_PROCESSING_TICKS, controller.getProcessingTicks());
        CCBNbtUtils.putBoolean(compoundTag, COMPOUND_KEY_OPERATING, controller.isOperating());
        CCBNbtUtils.putBoolean(compoundTag, COMPOUND_KEY_OPEN_STATE, controller.getWindowsOpenState());
    }

    void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        if (CCBNbtUtils.contains(compoundTag, COMPOUND_KEY_CORE)) {
            kettle.getCore().read(CCBNbtUtils.getCompound(compoundTag, COMPOUND_KEY_CORE));
        }
        boolean hasSerializedFilter = CCBNbtUtils.contains(compoundTag, COMPOUND_KEY_FILTER);
        kettle.loadRecipeFilter(hasSerializedFilter ? ItemStack.parseOptional(provider, CCBNbtUtils.getCompound(compoundTag, COMPOUND_KEY_FILTER)) : ItemStack.EMPTY, hasSerializedFilter);
        if (CCBNbtUtils.contains(compoundTag, COMPOUND_KEY_INPUT_ITEMS)) {
            kettle.getInputInventory().deserializeNBT(provider, CCBNbtUtils.getCompound(compoundTag, COMPOUND_KEY_INPUT_ITEMS));
        }
        if (CCBNbtUtils.contains(compoundTag, COMPOUND_KEY_OUTPUT_ITEMS)) {
            kettle.getOutputInventory().deserializeNBT(provider, CCBNbtUtils.getCompound(compoundTag, COMPOUND_KEY_OUTPUT_ITEMS));
        }

        int operatingTicks = CCBNbtUtils.getIntOrDefault(compoundTag, COMPOUND_KEY_OPERATING_TICKS, controller.getOperatingTicks());
        int processingTicks = CCBNbtUtils.getIntOrDefault(compoundTag, COMPOUND_KEY_PROCESSING_TICKS, controller.getProcessingTicks());
        boolean isOperating = CCBNbtUtils.getBooleanOrDefault(compoundTag, COMPOUND_KEY_OPERATING, controller.isOperating());
        boolean windowsOpen = CCBNbtUtils.getBooleanOrDefault(compoundTag, COMPOUND_KEY_OPEN_STATE, controller.getWindowsOpenState());
        controller.loadOperationState(isOperating, operatingTicks, processingTicks, windowsOpen, clientPacket);
    }
}
