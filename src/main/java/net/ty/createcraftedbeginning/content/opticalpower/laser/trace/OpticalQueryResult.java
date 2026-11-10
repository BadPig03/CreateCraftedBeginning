package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalHit;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
record OpticalQueryResult(double length, @Nullable OpticalHit hit) {
    OpticalQueryResult {
        if (!Double.isFinite(length) || length < 0) {
            throw new IllegalArgumentException("Optical query length must be finite and non-negative; got " + length + " blocks.");
        }
    }
}
