package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightReactorKettleSerialization {
    private static final String COMPOUND_KEY_CORE = "Core";
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

    void write(CompoundTag tag, Provider provider) {
        tag.put(COMPOUND_KEY_CORE, kettle.getCore().write());
        tag.put(COMPOUND_KEY_INPUT_ITEMS, kettle.getInputInventory().serializeNBT(provider));
        tag.put(COMPOUND_KEY_OUTPUT_ITEMS, kettle.getOutputInventory().serializeNBT(provider));
        tag.putInt(COMPOUND_KEY_OPERATING_TICKS, controller.getOperatingTicks());
        tag.putInt(COMPOUND_KEY_PROCESSING_TICKS, controller.getProcessingTicks());
        tag.putBoolean(COMPOUND_KEY_OPERATING, controller.isOperating());
        tag.putBoolean(COMPOUND_KEY_OPEN_STATE, controller.getWindowsOpenState());
    }

    void read(CompoundTag tag, Provider provider, boolean clientPacket) {
        if (tag.contains(COMPOUND_KEY_CORE)) {
            kettle.getCore().read(tag.getCompound(COMPOUND_KEY_CORE));
        }
        if (tag.contains(COMPOUND_KEY_INPUT_ITEMS)) {
            kettle.getInputInventory().deserializeNBT(provider, tag.getCompound(COMPOUND_KEY_INPUT_ITEMS));
        }
        if (tag.contains(COMPOUND_KEY_OUTPUT_ITEMS)) {
            kettle.getOutputInventory().deserializeNBT(provider, tag.getCompound(COMPOUND_KEY_OUTPUT_ITEMS));
        }

        int operatingTicks = tag.contains(COMPOUND_KEY_OPERATING_TICKS) ? tag.getInt(COMPOUND_KEY_OPERATING_TICKS) : controller.getOperatingTicks();
        int processingTicks = tag.contains(COMPOUND_KEY_PROCESSING_TICKS) ? tag.getInt(COMPOUND_KEY_PROCESSING_TICKS) : controller.getProcessingTicks();
        boolean operating = tag.contains(COMPOUND_KEY_OPERATING) ? tag.getBoolean(COMPOUND_KEY_OPERATING) : controller.isOperating();
        boolean windowsOpen = tag.contains(COMPOUND_KEY_OPEN_STATE) ? tag.getBoolean(COMPOUND_KEY_OPEN_STATE) : controller.getWindowsOpenState();
        controller.loadOperationState(operating, operatingTicks, processingTicks, windowsOpen, clientPacket);
    }
}
