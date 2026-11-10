package net.ty.createcraftedbeginning.content.opticalpower.laseremitter;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.ParametersAreNonnullByDefault;

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

    static void read(CompoundTag compoundTag, boolean clientPacket, LaserEmitterController controller, LaserEmitterDisplayState displayState) {
        if (!clientPacket) {
            return;
        }

        controller.restoreAllocation(compoundTag.getDouble(COMPOUND_KEY_OPTICAL_FLUX_LM));
        if (!compoundTag.hasUUID(COMPOUND_KEY_DISPLAY_INSTANCE)) {
            return;
        }

        displayState.restoreInstance(compoundTag.getUUID(COMPOUND_KEY_DISPLAY_INSTANCE));
    }
}
