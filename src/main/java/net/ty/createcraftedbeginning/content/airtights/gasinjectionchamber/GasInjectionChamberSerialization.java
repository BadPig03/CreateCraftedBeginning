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

    void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        compoundTag.putInt(GasInjectionChamberOperationState.COMPOUND_KEY_PROCESSING_TICKS, operation.getProcessingTicks());
        filter.writeInstalledFilter(compoundTag, provider);
        if (clientPacket) {
            compoundTag.putBoolean(GasInjectionChamberFilterState.COMPOUND_KEY_FILTER_LOCKED, operation.type == OperationType.FAN_PROCESSING);
        }
        visual.writeCloud(compoundTag, clientPacket);
    }

    void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        if (compoundTag.contains(GasInjectionChamberOperationState.COMPOUND_KEY_PROCESSING_TICKS)) {
            operation.synchronizeProcessingTicks(compoundTag.getInt(GasInjectionChamberOperationState.COMPOUND_KEY_PROCESSING_TICKS), clientPacket);
        }

        filter.readInstalledFilter(compoundTag, provider);
        if (clientPacket) {
            filter.setClientLocked(compoundTag.getBoolean(GasInjectionChamberFilterState.COMPOUND_KEY_FILTER_LOCKED));
        }
        else {
            operation.clearTransientOperation();
            chamber.scheduleBasinCheck();
        }
        visual.readCloud(compoundTag, clientPacket).ifPresent(display::spawnCloud);
    }
}
