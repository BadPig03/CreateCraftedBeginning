package net.ty.createcraftedbeginning.content.opticalpower.laseremitter;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.foundation.NbtValues;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserEmitterSerialization {
    private static final String COMPOUND_KEY_OPTICAL_FLUX_LM = "OpticalFluxLm";
    private static final String COMPOUND_KEY_DISPLAY_INSTANCE = "DisplayInstance";

    private LaserEmitterSerialization() {
    }

    static void write(CompoundTag compoundTag, boolean clientPacket, LaserEmitterController controller, LaserEmitterDisplayState displayState) {
        if (!clientPacket) {
            return;
        }

        compoundTag.putDouble(COMPOUND_KEY_OPTICAL_FLUX_LM, controller.getAllocatedFluxLm());
        compoundTag.putUUID(COMPOUND_KEY_DISPLAY_INSTANCE, displayState.instance());
    }

    static @Nullable ClientAllocation readClient(CompoundTag compoundTag, BlockPos pos, LaserEmitterDisplayState displayState) {
        double fluxLm = 0;
        if (compoundTag.contains(COMPOUND_KEY_OPTICAL_FLUX_LM)) {
            fluxLm = NbtValues.getDoubleOrDefault(compoundTag, COMPOUND_KEY_OPTICAL_FLUX_LM, Double.NaN);
        }
        if (!Double.isFinite(fluxLm)) {
            if (displayState.rejectAllocation()) {
                CCBAPI.LOGGER.warn("Rejected the laser emitter update at {}: '{}' must be a finite number, got {} lm.", pos, COMPOUND_KEY_OPTICAL_FLUX_LM, fluxLm);
            }

            return null;
        }

        String rangeKey = LaserEmitterRangeBehaviour.COMPOUND_KEY_LASER_RANGE;
        if (compoundTag.contains(rangeKey) && !Double.isFinite(NbtValues.getDoubleOrDefault(compoundTag, rangeKey, Double.NaN))) {
            if (displayState.rejectAllocation()) {
                CCBAPI.LOGGER.warn("Rejected the laser emitter update at {}: '{}' must be a finite number of blocks.", pos, rangeKey);
            }

            return null;
        }

        UUID instance = null;
        if (compoundTag.contains(COMPOUND_KEY_DISPLAY_INSTANCE)) {
            if (!compoundTag.hasUUID(COMPOUND_KEY_DISPLAY_INSTANCE)) {
                if (displayState.rejectAllocation()) {
                    CCBAPI.LOGGER.warn("Rejected the laser emitter update at {}: '{}' must be a UUID.", pos, COMPOUND_KEY_DISPLAY_INSTANCE);
                }

                return null;
            }

            instance = compoundTag.getUUID(COMPOUND_KEY_DISPLAY_INSTANCE);
        }
        return new ClientAllocation(fluxLm, instance);
    }

    record ClientAllocation(double fluxLm, @Nullable UUID instance) {}
}
