package net.ty.createcraftedbeginning.content.opticalpower.photothermalreceiver;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.opticalpower.laser.exposure.OpticalExposure;
import net.ty.createcraftedbeginning.content.opticalpower.laser.exposure.OpticalExposureSettlement;
import net.ty.createcraftedbeginning.content.opticalpower.laser.exposure.OpticalExposureSettlement.Receipt;
import net.ty.createcraftedbeginning.content.opticalpower.laser.exposure.OpticalIllumination;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class PhotothermalReceiverController {
    private static final int HEAT_SYNC_INTERVAL_TICKS = 5;

    private final PhotothermalReceiverBlockEntity receiver;
    private final PhotothermalHeatState heatState = new PhotothermalHeatState();
    private final OpticalExposureSettlement settlement;
    private double receivedFluxLm;
    private double offeredFluxLm;
    private double availableFluxLm;
    private int illuminatedPorts;
    private int targetProgress;

    PhotothermalReceiverController(PhotothermalReceiverBlockEntity receiver) {
        this.receiver = receiver;
        settlement = new OpticalExposureSettlement(receiver);
    }

    void tickServer() {
        Level level = receiver.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        long gameTime = level.getGameTime();
        double maxFluxLm = CCBConfig.server().opticalPower.photothermalReceiver.maxReceivedFluxLm.get();
        Receipt receipt = settlement.settle(maxFluxLm, exposure -> exposure.hit().target().contraptionId() == null);
        if (receipt == null) {
            return;
        }

        double previousReceivedFluxLm = receivedFluxLm;
        double previousOfferedFluxLm = offeredFluxLm;
        double previousAvailableFluxLm = availableFluxLm;
        double previousQuantityLumenSeconds = heatState.getQuantityLumenSeconds();
        int previousIlluminatedPorts = illuminatedPorts;
        int previousTargetProgress = targetProgress;
        illuminatedPorts = 0;
        OpticalIllumination illumination = new OpticalIllumination();
        List<PhotothermalReceiverPort> ports = PhotothermalReceiverPort.getPorts();
        VoxelShape laserShape = PhotothermalReceiverPort.getLaserShape();
        for (OpticalExposure exposure : receipt.exposures()) {
            for (PhotothermalReceiverPort port : ports) {
                illumination.add(exposure, port.ordinal(), port.getReceivingShape(), laserShape);
            }
        }
        for (PhotothermalReceiverPort port : ports) {
            if (illumination.getQualifiedArea(port.ordinal(), 0) <= 0) {
                continue;
            }

            illuminatedPorts |= port.getMask();
        }
        offeredFluxLm = Math.min(receipt.offeredFluxLm(), illumination.getFluxLm());
        availableFluxLm = Math.min(offeredFluxLm, maxFluxLm);
        targetProgress = PhotothermalHeatState.resolveTargetHeatProgress(illumination);
        int previousHeatProgress = heatState.getHeatProgress();
        receivedFluxLm = Math.min(availableFluxLm, (PhotothermalHeatState.CAPACITY_LUMEN_SECONDS - previousQuantityLumenSeconds) / OpticalExposureSettlement.TICK_SECONDS);
        heatState.tick(targetProgress, receivedFluxLm * OpticalExposureSettlement.TICK_SECONDS, availableFluxLm);
        HeatLevel outputHeatLevel = switch (heatState.getHeatLevel()) {
            case 1 -> HeatLevel.KINDLED;
            case 2 -> HeatLevel.SEETHING;
            default -> HeatLevel.NONE;
        };
        BlockState state = receiver.getBlockState();
        if (state.getValue(BlazeBurnerBlock.HEAT_LEVEL) != outputHeatLevel) {
            level.setBlockAndUpdate(receiver.getBlockPos(), state.setValue(BlazeBurnerBlock.HEAT_LEVEL, outputHeatLevel));
            receiver.notifyUpdate();
            return;
        }

        int heatProgress = heatState.getHeatProgress();
        boolean heatProgressChanged = previousHeatProgress != heatProgress;
        boolean heatStateChanged = heatProgressChanged || previousQuantityLumenSeconds != heatState.getQuantityLumenSeconds();
        if (heatStateChanged) {
            receiver.setChanged();
        }

        boolean illuminationChanged = previousIlluminatedPorts != illuminatedPorts || previousReceivedFluxLm != receivedFluxLm || previousOfferedFluxLm != offeredFluxLm || previousAvailableFluxLm != availableFluxLm || previousTargetProgress != targetProgress;
        boolean reachedHeatTarget = heatProgressChanged && heatProgress == targetProgress;
        boolean periodicHeatSync = heatStateChanged && gameTime % HEAT_SYNC_INTERVAL_TICKS == 0;
        if (!illuminationChanged && !reachedHeatTarget && !periodicHeatSync) {
            return;
        }

        receiver.sendData();
    }

    void restoreIllumination(int targetProgress, int illuminatedPorts) {
        this.targetProgress = targetProgress;
        this.illuminatedPorts = illuminatedPorts;
    }

    void restoreFlux(double receivedFluxLm, double offeredFluxLm, double availableFluxLm) {
        this.receivedFluxLm = receivedFluxLm;
        this.offeredFluxLm = offeredFluxLm;
        this.availableFluxLm = availableFluxLm;
    }

    PhotothermalHeatState getHeatState() {
        return heatState;
    }

    int getIlluminatedPorts() {
        return illuminatedPorts;
    }

    boolean isPortIlluminated(PhotothermalReceiverPort port) {
        return (illuminatedPorts & port.getMask()) != 0;
    }

    double getReceivedFluxLm() {
        return receivedFluxLm;
    }

    double getOfferedFluxLm() {
        return offeredFluxLm;
    }

    double getAvailableFluxLm() {
        return availableFluxLm;
    }

    int getTargetProgress() {
        return targetProgress;
    }

    int getHeatLevel() {
        return heatState.getHeatLevel();
    }

    int getHeatProgress() {
        return heatState.getHeatProgress();
    }
}
