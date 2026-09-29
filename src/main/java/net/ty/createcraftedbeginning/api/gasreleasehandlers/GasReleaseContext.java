package net.ty.createcraftedbeginning.api.gasreleasehandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;
import java.util.OptionalLong;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record GasReleaseContext(Level level, GasStack gas, BlockPos sourcePos, BlockPos effectPos, Optional<Direction> direction, GasReleaseCause cause, GasReleaseMode mode, OptionalLong sourcePressurePa, long atmosphericPressurePa) {
    private static final double LOG_2 = Math.log(2);
    private static final double MAX_PRESSURE_STEPS = 5;
    private static final float MIN_PRESSURE_SCALE = 0.75F;
    private static final float PRESSURE_SCALE_PER_STEP = 0.25F;

    public GasReleaseContext {
        gas = gas.copy();
        sourcePos = sourcePos.immutable();
        effectPos = effectPos.immutable();
        if (atmosphericPressurePa < GasPressure.VACUUM_PA) {
            throw new IllegalArgumentException("Atmospheric pressure must be non-negative; got " + atmosphericPressurePa + " Pa.");
        }

        if (sourcePressurePa.isPresent() && sourcePressurePa.getAsLong() < GasPressure.VACUUM_PA) {
            throw new IllegalArgumentException("Gas release source pressure must be non-negative; got " + sourcePressurePa.getAsLong() + " Pa.");
        }
    }

    @Override
    public GasStack gas() {
        return gas.copy();
    }

    public long gasAmount() {
        return gas.getAmount();
    }

    public OptionalLong pressureDeltaPa() {
        if (sourcePressurePa.isEmpty()) {
            return OptionalLong.empty();
        }

        return OptionalLong.of(Math.max(GasPressure.VACUUM_PA, sourcePressurePa.getAsLong() - atmosphericPressurePa));
    }

    public float effectInflation() {
        OptionalLong pressureDelta = pressureDeltaPa();
        if (pressureDelta.isEmpty()) {
            return 1;
        }

        double pressureDeltaAtm = (double) pressureDelta.getAsLong() / GasPressure.REFERENCE_PRESSURE_PA;
        double pressureSteps = Math.min(MAX_PRESSURE_STEPS, Math.log1p(pressureDeltaAtm) / LOG_2);
        return (float) (MIN_PRESSURE_SCALE + PRESSURE_SCALE_PER_STEP * pressureSteps);
    }

    public AABB effectBounds() {
        return new AABB(effectPos).inflate(effectInflation());
    }
}
