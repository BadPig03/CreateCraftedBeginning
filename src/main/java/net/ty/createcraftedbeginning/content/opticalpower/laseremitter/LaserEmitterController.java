package net.ty.createcraftedbeginning.content.opticalpower.laseremitter;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerNetwork;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.BooleanSupplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserEmitterController {
    private final LaserEmitterBlockEntity emitter;
    private @Nullable BooleanSupplier allocationValidity;
    private double allocatedFluxLm;

    LaserEmitterController(LaserEmitterBlockEntity emitter) {
        this.emitter = emitter;
    }

    void restoreAllocation(double fluxLm) {
        allocatedFluxLm = Math.max(0, fluxLm);
        allocationValidity = null;
    }

    void applyAllocation(double fluxLm, @Nullable BooleanSupplier allocationValidity) {
        Level level = emitter.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        if (!Double.isFinite(fluxLm)) {
            throw new IllegalArgumentException("Allocated luminous flux must be finite; got " + fluxLm + " lm.");
        }

        double clamped = Mth.clamp(fluxLm, 0, OpticalPowerNetwork.getMaxConsumerFluxLm());
        boolean changed = allocatedFluxLm != clamped || getAllocatedFluxLm() != clamped;
        this.allocationValidity = allocationValidity;
        if (!changed) {
            return;
        }

        allocatedFluxLm = clamped;
        emitter.notifyUpdate();
    }

    double getAllocatedFluxLm() {
        BooleanSupplier validity = allocationValidity;
        if (validity != null && !validity.getAsBoolean()) {
            return 0;
        }

        return allocatedFluxLm;
    }
}
