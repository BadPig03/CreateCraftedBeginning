package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberOperationState.OperationType;
import net.ty.createcraftedbeginning.foundation.NbtValues;

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
        filter.writeInstalledFilter(compoundTag, provider);
        if (clientPacket) {
            compoundTag.putFloat(GasInjectionChamberOperationState.COMPOUND_KEY_PROCESSING_TICKS, operation.getProcessingTicks());
            compoundTag.putFloat(GasInjectionChamberOperationState.COMPOUND_KEY_PRESSURE_SPEED_MULTIPLIER, operation.getPressureSpeedMultiplier());
            compoundTag.putInt(GasInjectionChamberOperationState.COMPOUND_KEY_PROCESSING_CYCLE, operation.getProcessingCycle());
            compoundTag.putBoolean(GasInjectionChamberFilterState.COMPOUND_KEY_FILTER_LOCKED, operation.type == OperationType.FAN_PROCESSING);
        }
        visual.writeCloud(compoundTag, clientPacket);
    }

    void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        if (clientPacket && compoundTag.contains(GasInjectionChamberOperationState.COMPOUND_KEY_PROCESSING_TICKS)) {
            float processingTicks = NbtValues.getFloatOrDefault(compoundTag, GasInjectionChamberOperationState.COMPOUND_KEY_PROCESSING_TICKS, operation.getProcessingTicks());
            float pressureSpeedMultiplier = NbtValues.getFloatOrDefault(compoundTag, GasInjectionChamberOperationState.COMPOUND_KEY_PRESSURE_SPEED_MULTIPLIER, 1.0F);
            int processingCycle = NbtValues.getIntOrDefault(compoundTag, GasInjectionChamberOperationState.COMPOUND_KEY_PROCESSING_CYCLE, operation.getProcessingCycle());
            operation.synchronizeProcessingState(processingTicks, pressureSpeedMultiplier, processingCycle);
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
