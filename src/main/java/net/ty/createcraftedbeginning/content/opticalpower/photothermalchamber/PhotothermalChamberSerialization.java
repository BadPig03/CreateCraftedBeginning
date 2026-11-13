package net.ty.createcraftedbeginning.content.opticalpower.photothermalchamber;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.opticalpower.energy.OpticalEnergyStorage;
import net.ty.createcraftedbeginning.foundation.NbtValues;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class PhotothermalChamberSerialization {
    private static final String COMPOUND_KEY_RECEIVED_FLUX_LM = "ReceivedFluxLm";
    private static final String COMPOUND_KEY_OFFERED_FLUX_LM = "OfferedFluxLm";
    private static final String COMPOUND_KEY_AVAILABLE_FLUX_LM = "AvailableFluxLm";
    private static final String COMPOUND_KEY_ILLUMINATED_PORTS = "IlluminatedPorts";
    private static final String COMPOUND_KEY_TARGET_PROGRESS = "TargetProgress";
    private static final int PORT_MASK = (1 << PhotothermalChamberPort.getPorts().size()) - 1;

    private PhotothermalChamberSerialization() {
    }

    public static void writeClientState(CompoundTag compoundTag, int targetProgress, int illuminatedPorts, double receivedFluxLm, double offeredFluxLm, double availableFluxLm) {
        compoundTag.putInt(COMPOUND_KEY_TARGET_PROGRESS, targetProgress);
        writeIllumination(compoundTag, illuminatedPorts, receivedFluxLm, offeredFluxLm, availableFluxLm);
    }

    public static void writeIllumination(CompoundTag compoundTag, int illuminatedPorts, double receivedFluxLm, double offeredFluxLm, double availableFluxLm) {
        compoundTag.putInt(COMPOUND_KEY_ILLUMINATED_PORTS, illuminatedPorts);
        compoundTag.putDouble(COMPOUND_KEY_RECEIVED_FLUX_LM, receivedFluxLm);
        compoundTag.putDouble(COMPOUND_KEY_OFFERED_FLUX_LM, offeredFluxLm);
        compoundTag.putDouble(COMPOUND_KEY_AVAILABLE_FLUX_LM, availableFluxLm);
    }

    static void write(CompoundTag compoundTag, boolean clientPacket, PhotothermalChamberController controller) {
        controller.getHeatState().write(compoundTag);
        if (!clientPacket) {
            return;
        }

        writeClientState(compoundTag, controller.getTargetProgress(), controller.getIlluminatedPorts(), controller.getReceivedFluxLm(), controller.getOfferedFluxLm(), controller.getAvailableFluxLm());
    }

    static @Nullable ClientSnapshot readClient(CompoundTag compoundTag, BlockPos pos, boolean reportFailure) {
        double quantity = OpticalEnergyStorage.readQuantityLumenSeconds(compoundTag);
        if (Double.isNaN(quantity)) {
            if (reportFailure) {
                CCBAPI.LOGGER.warn("Rejected the photothermal chamber update at {}: 'QuantityLumenSeconds' must be a finite non-negative number in lm·s.", pos);
            }

            return null;
        }

        double receivedFluxLm = readFlux(compoundTag, COMPOUND_KEY_RECEIVED_FLUX_LM);
        double offeredFluxLm = readFlux(compoundTag, COMPOUND_KEY_OFFERED_FLUX_LM);
        double availableFluxLm = readFlux(compoundTag, COMPOUND_KEY_AVAILABLE_FLUX_LM);
        if (!Double.isFinite(receivedFluxLm) || receivedFluxLm < 0 || !Double.isFinite(availableFluxLm) || availableFluxLm < receivedFluxLm || !Double.isFinite(offeredFluxLm) || offeredFluxLm < availableFluxLm) {
            if (reportFailure) {
                CCBAPI.LOGGER.warn("Rejected the photothermal chamber update at {}: luminous flux must be numeric and finite with 0 <= received <= available <= offered; got received={}, available={}, offered={} lm.", pos, receivedFluxLm, availableFluxLm, offeredFluxLm);
            }

            return null;
        }

        return new ClientSnapshot(quantity, compoundTag.getInt(COMPOUND_KEY_TARGET_PROGRESS), compoundTag.getInt(COMPOUND_KEY_ILLUMINATED_PORTS) & PORT_MASK, receivedFluxLm, offeredFluxLm, availableFluxLm);
    }

    private static double readFlux(CompoundTag compoundTag, String key) {
        if (!compoundTag.contains(key)) {
            return 0;
        }

        return NbtValues.getDoubleOrDefault(compoundTag, key, Double.NaN);
    }

    record ClientSnapshot(double quantityLumenSeconds, int targetProgress, int illuminatedPorts, double receivedFluxLm, double offeredFluxLm, double availableFluxLm) {
        void restore(CompoundTag compoundTag, PhotothermalChamberController controller) {
            controller.getHeatState().restore(compoundTag, quantityLumenSeconds);
            controller.restoreIllumination(targetProgress, illuminatedPorts);
            controller.restoreFlux(receivedFluxLm, offeredFluxLm, availableFluxLm);
        }
    }
}
