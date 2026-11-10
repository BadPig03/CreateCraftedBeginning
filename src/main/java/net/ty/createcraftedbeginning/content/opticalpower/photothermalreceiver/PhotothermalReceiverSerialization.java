package net.ty.createcraftedbeginning.content.opticalpower.photothermalreceiver;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class PhotothermalReceiverSerialization {
    private static final String COMPOUND_KEY_RECEIVED_FLUX_LM = "ReceivedFluxLm";
    private static final String COMPOUND_KEY_OFFERED_FLUX_LM = "OfferedFluxLm";
    private static final String COMPOUND_KEY_AVAILABLE_FLUX_LM = "AvailableFluxLm";
    private static final String COMPOUND_KEY_ILLUMINATED_PORTS = "IlluminatedPorts";
    private static final String COMPOUND_KEY_TARGET_PROGRESS = "TargetProgress";
    private static final int PORT_MASK = (1 << PhotothermalReceiverPort.getPorts().size()) - 1;

    private PhotothermalReceiverSerialization() {
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

    static void write(CompoundTag compoundTag, boolean clientPacket, PhotothermalReceiverController controller) {
        controller.getHeatState().write(compoundTag);
        if (!clientPacket) {
            return;
        }

        writeClientState(compoundTag, controller.getTargetProgress(), controller.getIlluminatedPorts(), controller.getReceivedFluxLm(), controller.getOfferedFluxLm(), controller.getAvailableFluxLm());
    }

    static void read(CompoundTag compoundTag, boolean clientPacket, PhotothermalReceiverController controller) {
        controller.getHeatState().read(compoundTag);
        if (!clientPacket) {
            return;
        }

        controller.restoreIllumination(compoundTag.getInt(COMPOUND_KEY_TARGET_PROGRESS), compoundTag.getInt(COMPOUND_KEY_ILLUMINATED_PORTS) & PORT_MASK);
        double receivedFluxLm = compoundTag.getDouble(COMPOUND_KEY_RECEIVED_FLUX_LM);
        double offeredFluxLm = compoundTag.getDouble(COMPOUND_KEY_OFFERED_FLUX_LM);
        double availableFluxLm = compoundTag.getDouble(COMPOUND_KEY_AVAILABLE_FLUX_LM);
        if (!Double.isFinite(receivedFluxLm) || receivedFluxLm < 0 || !Double.isFinite(availableFluxLm) || availableFluxLm < receivedFluxLm || !Double.isFinite(offeredFluxLm) || offeredFluxLm < availableFluxLm) {
            throw new IllegalArgumentException("Photothermal packet luminous flux must be finite and satisfy 0 <= received <= available <= offered; got received=" + receivedFluxLm + ", available=" + availableFluxLm + ", offered=" + offeredFluxLm + " lm.");
        }

        controller.restoreFlux(receivedFluxLm, offeredFluxLm, availableFluxLm);
    }
}
