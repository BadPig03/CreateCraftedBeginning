package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightReactorKettleSerialization {
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

    public AirtightReactorKettleSerialization(AirtightReactorKettleBlockEntity kettle, AirtightReactorKettleController controller) {
        this.kettle = kettle;
        this.controller = controller;
    }

    public void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        compoundTag.put(COMPOUND_KEY_CORE, kettle.getCore().write());
        compoundTag.put(COMPOUND_KEY_FILTER, kettle.getRecipeFilter().saveOptional(provider));
        compoundTag.put(COMPOUND_KEY_INPUT_ITEMS, kettle.getInputInventory().serializeNBT(provider));
        compoundTag.put(COMPOUND_KEY_OUTPUT_ITEMS, kettle.getOutputInventory().serializeNBT(provider));
        if (!clientPacket) {
            return;
        }

        compoundTag.putInt(COMPOUND_KEY_OPERATING_TICKS, controller.getOperatingTicks());
        compoundTag.putInt(COMPOUND_KEY_PROCESSING_TICKS, controller.getProcessingTicks());
        compoundTag.putBoolean(COMPOUND_KEY_OPERATING, controller.isOperating());
        compoundTag.putBoolean(COMPOUND_KEY_OPEN_STATE, controller.getWindowsOpenState());
    }

    public void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        if (compoundTag.contains(COMPOUND_KEY_CORE)) {
            kettle.getCore().read(compoundTag.getCompound(COMPOUND_KEY_CORE));
        }
        boolean hasStoredFilter = compoundTag.contains(COMPOUND_KEY_FILTER);
        kettle.loadRecipeFilter(hasStoredFilter ? ItemStack.parseOptional(provider, compoundTag.getCompound(COMPOUND_KEY_FILTER)) : ItemStack.EMPTY, hasStoredFilter);
        if (compoundTag.contains(COMPOUND_KEY_INPUT_ITEMS)) {
            kettle.getInputInventory().deserializeNBT(provider, compoundTag.getCompound(COMPOUND_KEY_INPUT_ITEMS));
        }
        if (compoundTag.contains(COMPOUND_KEY_OUTPUT_ITEMS)) {
            kettle.getOutputInventory().deserializeNBT(provider, compoundTag.getCompound(COMPOUND_KEY_OUTPUT_ITEMS));
        }

        int operatingTicks = compoundTag.contains(COMPOUND_KEY_OPERATING_TICKS) ? compoundTag.getInt(COMPOUND_KEY_OPERATING_TICKS) : controller.getOperatingTicks();
        int processingTicks = compoundTag.contains(COMPOUND_KEY_PROCESSING_TICKS) ? compoundTag.getInt(COMPOUND_KEY_PROCESSING_TICKS) : controller.getProcessingTicks();
        boolean operating = compoundTag.contains(COMPOUND_KEY_OPERATING) ? compoundTag.getBoolean(COMPOUND_KEY_OPERATING) : controller.isOperating();
        boolean windowsOpen = compoundTag.contains(COMPOUND_KEY_OPEN_STATE) ? compoundTag.getBoolean(COMPOUND_KEY_OPEN_STATE) : controller.getWindowsOpenState();
        controller.loadOperationState(operating, operatingTicks, processingTicks, windowsOpen, clientPacket);
    }
}
