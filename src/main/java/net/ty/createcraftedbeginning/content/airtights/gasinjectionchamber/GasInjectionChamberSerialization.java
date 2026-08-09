package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberOperationState.OperationType;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasInjectionChamberSerialization {
    private final GasInjectionChamberBlockEntity chamber;
    private final GasInjectionChamberOperationState operation;
    private final GasInjectionChamberFilterState filter;
    private final GasInjectionChamberVisualState visual;
    private final GasInjectionChamberDisplay display;

    GasInjectionChamberSerialization(GasInjectionChamberBlockEntity chamber, GasInjectionChamberOperationState operation, GasInjectionChamberFilterState filter, GasInjectionChamberVisualState visual, GasInjectionChamberDisplay display) {
        this.chamber = chamber;
        this.operation = operation;
        this.filter = filter;
        this.visual = visual;
        this.display = display;
    }

    void write(CompoundTag tag, Provider provider, boolean clientPacket) {
        tag.putInt(GasInjectionChamberOperationState.COMPOUND_KEY_PROCESSING_TICKS, operation.getProcessingTicks());
        filter.writeInstalledFilter(tag, provider);
        if (clientPacket) {
            tag.putBoolean(GasInjectionChamberFilterState.COMPOUND_KEY_FILTER_LOCKED, operation.type == OperationType.FAN_PROCESSING);
        }
        else {
            operation.writeOperation(tag, provider);
        }
        visual.writeCloud(tag, clientPacket);
    }

    void read(CompoundTag tag, Provider provider, boolean clientPacket) {
        if (tag.contains(GasInjectionChamberOperationState.COMPOUND_KEY_PROCESSING_TICKS)) {
            operation.synchronizeProcessingTicks(tag.getInt(GasInjectionChamberOperationState.COMPOUND_KEY_PROCESSING_TICKS), clientPacket);
        }

        filter.readInstalledFilter(tag, provider);
        if (clientPacket) {
            filter.setClientLocked(tag.getBoolean(GasInjectionChamberFilterState.COMPOUND_KEY_FILTER_LOCKED));
        }
        else if (!operation.readOperation(tag, provider, chamber::isFanProcessingOperationStillValid)) {
            chamber.clearOperationState();
        }

        visual.readCloud(tag, clientPacket).ifPresent(display::spawnCloud);
    }
}
