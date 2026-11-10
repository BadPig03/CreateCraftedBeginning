package net.ty.createcraftedbeginning.api.gasreleasehandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.EntityArea;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Predicate;

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

        if (sourcePressurePa.isPresent()) {
            long pressurePa = sourcePressurePa.getAsLong();
            if (pressurePa < GasPressure.VACUUM_PA) {
                throw new IllegalArgumentException("Gas release source pressure must be non-negative; got " + pressurePa + " Pa.");
            }
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

    /**
     * Returns the release bounds in the source coordinate frame. Entity queries should use
     * {@link #findEntities(Class)} to preserve these bounds when a physical structure rotates.
     */
    public AABB effectBounds() {
        return new AABB(effectPos).inflate(effectInflation());
    }

    /**
     * Finds entities intersecting the release volume, including its current physical orientation.
     */
    public <T extends Entity> List<T> findEntities(Class<T> entityClass) {
        return findEntities(entityClass, entity -> true);
    }

    /**
     * Finds entities intersecting the release volume and satisfying the caller's predicate.
     */
    public <T extends Entity> List<T> findEntities(Class<T> entityClass, Predicate<? super T> predicate) {
        AABB bounds = effectBounds();
        EntityArea area = SubLevelBridge.createEntityArea(level, sourcePos, bounds);
        return level.getEntitiesOfClass(entityClass, bounds, entity -> area.intersects(entity) && predicate.test(entity));
    }
}
