package net.ty.createcraftedbeginning.content.opticalpower.laser.exposure;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class OpticalExposureSettlement {
    public static final double TICK_SECONDS = 0.05;

    private final BlockEntity recipient;
    private long settledTick = Long.MIN_VALUE;

    public OpticalExposureSettlement(BlockEntity recipient) {
        this.recipient = recipient;
    }

    public @Nullable Receipt settle(double maxFluxLm, Predicate<OpticalExposure> accepts) {
        if (!Double.isFinite(maxFluxLm) || maxFluxLm < 0) {
            throw new IllegalArgumentException("Received luminous flux limit must be finite and non-negative; got " + maxFluxLm + " lm.");
        }

        if (!(recipient.getLevel() instanceof ServerLevel level) || recipient.isRemoved()) {
            return null;
        }

        long gameTime = level.getGameTime();
        if (settledTick == gameTime) {
            return null;
        }

        List<OpticalExposure> accepted = new ArrayList<>();
        double terminatedFluxLm = 0;
        double offeredFluxLm = 0;
        for (OpticalExposure exposure : OpticalExposureLedger.getPrevious(recipient)) {
            double absorbedFluxLm = exposure.absorbedFluxLm();
            if (absorbedFluxLm <= 0 || !accepts.test(exposure)) {
                continue;
            }

            terminatedFluxLm += absorbedFluxLm;
            offeredFluxLm += exposure.receivedFluxLm();
            accepted.add(exposure);
        }
        Receipt receipt = new Receipt(accepted, terminatedFluxLm, offeredFluxLm, Math.min(offeredFluxLm, maxFluxLm));
        settledTick = gameTime;
        return receipt;
    }

    public record Receipt(List<OpticalExposure> exposures, double terminatedFluxLm, double offeredFluxLm, double receivedFluxLm) {
        public Receipt {
            exposures = List.copyOf(exposures);
            if (!Double.isFinite(terminatedFluxLm) || terminatedFluxLm < offeredFluxLm || !Double.isFinite(offeredFluxLm) || !Double.isFinite(receivedFluxLm) || receivedFluxLm < 0 || offeredFluxLm < receivedFluxLm) {
                throw new IllegalArgumentException("Optical receipt must have finite 0 <= received <= offered <= terminated luminous flux; got received=" + receivedFluxLm + ", offered=" + offeredFluxLm + ", terminated=" + terminatedFluxLm + " lm.");
            }
        }

        public double dissipatedFluxLm() {
            return terminatedFluxLm - receivedFluxLm;
        }

    }
}
