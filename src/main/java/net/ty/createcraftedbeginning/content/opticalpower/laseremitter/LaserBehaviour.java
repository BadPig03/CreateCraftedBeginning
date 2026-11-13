package net.ty.createcraftedbeginning.content.opticalpower.laseremitter;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation;
import net.ty.createcraftedbeginning.content.opticalpower.laser.PropagationBudget;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.exposure.OpticalExposureLedger;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.BeamOcclusionSolver;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserCollisionContext;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.RayProjection;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class LaserBehaviour extends BlockEntityBehaviour {
    public static final int MIN_RANGE = 1;
    public static final int MAX_RANGE = 32;
    public static final BehaviourType<LaserBehaviour> TYPE = new BehaviourType<>();
    private static final BeamProfile SOURCE_PROFILE = new BeamProfile(0.03125);
    private static final int TRACE_RETRY_DELAY_TICKS = 20;
    private final Supplier<Vec3> start;
    private final Supplier<Vec3> direction;
    private final DoubleSupplier flux;
    private final IntSupplier range;
    private final LaserServerTraceCache cache = new LaserServerTraceCache();

    private @Nullable BeamPropagation propagation;
    private long traceRetryTick = Long.MIN_VALUE;

    public LaserBehaviour(SmartBlockEntity blockEntity, Supplier<Vec3> start, Supplier<Vec3> direction, DoubleSupplier flux, IntSupplier range) {
        super(blockEntity);
        this.start = start;
        this.direction = direction;
        this.flux = flux;
        this.range = range;
    }

    public static BeamState createSource(Vec3 origin, Vec3 direction, double fluxLm) {
        return new BeamState(BeamFrame.fromDirection(origin, direction), SOURCE_PROFILE, fluxLm);
    }

    @Override
    public BehaviourType<?> getType() {
        return TYPE;
    }

    @Override
    public void tick() {
        Level level = blockEntity.getLevel();
        if (level == null || level.isClientSide) {
            traceRetryTick = Long.MIN_VALUE;
            clearPropagation();
            return;
        }

        long tick = level.getGameTime();
        if (tick < traceRetryTick) {
            return;
        }

        BeamPropagation result;
        LaserCollisionContext displayCollision = null;
        try {
            TraceSource source = createTraceSource(level);
            if (source == null) {
                result = null;
            }
            else {
                if (!source.inSubLevel() && !SubLevelBridge.resolve(level, blockEntity.getBlockPos()).inSubLevel()) {
                    result = cache.trace(level, source.beam(), source.range());
                }
                else {
                    cache.clear();
                    displayCollision = new LaserCollisionContext(1, true);
                    result = BeamOcclusionSolver.solve(level, source.beam(), PropagationBudget.standard(source.range()), displayCollision, false);
                }
                OpticalExposureLedger.submit(blockEntity, result);
            }
            traceRetryTick = Long.MIN_VALUE;
        }
        catch (RuntimeException failure) {
            result = null;
            if (traceRetryTick == Long.MIN_VALUE) {
                CCBAPI.LOGGER.warn("Failed to update laser propagation or exposure at {} in dimension '{}'; pausing this source and retrying after {} ticks.", blockEntity.getBlockPos(), level.dimension().location(), TRACE_RETRY_DELAY_TICKS, failure);
            }

            traceRetryTick = tick + TRACE_RETRY_DELAY_TICKS;
        }

        if (result == null) {
            clearPropagation();
            if (!(blockEntity instanceof LaserEmitterBlockEntity emitter)) {
                return;
            }

            LaserServerDisplayQueue.update(emitter, null, null, null);
            return;
        }

        propagation = result;
        if (!(blockEntity instanceof LaserEmitterBlockEntity emitter)) {
            return;
        }

        LaserEmitterAdvancements.award(emitter, result);
        LaserServerDisplayQueue.update(emitter, result, cache.dependencies(result), displayCollision);
    }

    @Override
    public void unload() {
        traceRetryTick = Long.MIN_VALUE;
        clearPropagation();
        if (!(blockEntity instanceof LaserEmitterBlockEntity emitter)) {
            return;
        }

        LaserServerDisplayQueue.remove(emitter);
    }

    public @Nullable BeamPropagation getPropagation() {
        return propagation;
    }

    public @Nullable BeamPropagation trace(@Nullable LaserCollisionContext collision) {
        Level level = blockEntity.getLevel();
        if (level == null || level.isClientSide) {
            return null;
        }

        TraceSource source = createTraceSource(level);
        if (source == null) {
            return null;
        }

        if (collision == null) {
            collision = new LaserCollisionContext(1, false);
        }
        return BeamOcclusionSolver.solve(level, source.beam(), PropagationBudget.standard(source.range()), collision, false);
    }

    private @Nullable TraceSource createTraceSource(Level level) {
        double fluxLm = flux.getAsDouble();
        if (blockEntity.isVirtual() || blockEntity.isRemoved() || fluxLm <= 0) {
            return null;
        }

        Vec3 origin = start.get();
        Vec3 localDirection = direction.get();
        int sourceRange = Mth.clamp(range.getAsInt(), MIN_RANGE, MAX_RANGE);
        BeamState localSource = createSource(origin, localDirection, fluxLm);
        BeamFrame localFrame = localSource.frame();
        RayProjection ray = SubLevelBridge.projectRay(level, origin, origin.add(localFrame.direction()));
        RayProjection axis = SubLevelBridge.projectRay(level, origin, origin.add(localFrame.transverseAxis()));
        Vec3 worldStart = ray.worldStart();
        BeamFrame worldFrame = new BeamFrame(worldStart, ray.worldEnd().subtract(worldStart), axis.worldEnd().subtract(axis.worldStart()));
        BeamState source = new BeamState(worldFrame, localSource.profile(), localSource.fluxLm(), localSource.footprint());
        return new TraceSource(source, sourceRange, ray.inSubLevel());
    }

    private void clearPropagation() {
        cache.clear();
        propagation = null;
        OpticalExposureLedger.withdraw(blockEntity);
    }

    private record TraceSource(BeamState beam, int range, boolean inSubLevel) {}
}
