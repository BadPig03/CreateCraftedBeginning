package net.ty.createcraftedbeginning.content.opticalpower.laseremitter;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation;
import net.ty.createcraftedbeginning.content.opticalpower.laser.PropagationBudget;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.exposure.OpticalExposureLedger;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.BeamOcclusionSolver;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserCollisionContext;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserTraceObservation;
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
    private final Supplier<Vec3> start;
    private final Supplier<Vec3> direction;
    private final DoubleSupplier flux;
    private final IntSupplier range;
    private final LaserServerTraceCache cache = new LaserServerTraceCache();

    private @Nullable BeamPropagation propagation;
    private @Nullable LaserTraceObservation displayDependencies;

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
        propagation = trace(null, true);
        if (propagation == null) {
            cache.clear();
            OpticalExposureLedger.withdraw(blockEntity);
            if (blockEntity instanceof LaserEmitterBlockEntity emitter) {
                LaserServerDisplayQueue.update(emitter, null, null);
            }
            return;
        }

        OpticalExposureLedger.submit(blockEntity, propagation);
        if (!(blockEntity instanceof LaserEmitterBlockEntity emitter)) {
            return;
        }

        LaserTraceObservation dependencies = cache.dependencies(propagation);
        if (dependencies == null) {
            dependencies = displayDependencies;
        }
        LaserServerDisplayQueue.update(emitter, propagation, dependencies);
    }

    @Override
    public void unload() {
        cache.clear();
        propagation = null;
        displayDependencies = null;
        OpticalExposureLedger.withdraw(blockEntity);
        if (!(blockEntity instanceof LaserEmitterBlockEntity emitter)) {
            return;
        }

        LaserServerDisplayQueue.remove(emitter);
    }

    public @Nullable BeamPropagation getPropagation() {
        return propagation;
    }

    public @Nullable BeamPropagation trace(@Nullable LaserCollisionContext collision) {
        return trace(collision, false);
    }

    private @Nullable BeamPropagation trace(@Nullable LaserCollisionContext collision, boolean cached) {
        if (cached) {
            displayDependencies = null;
        }
        Level level = blockEntity.getLevel();
        double fluxLm = flux.getAsDouble();
        if (level == null || level.isClientSide || blockEntity.isVirtual() || blockEntity.isRemoved() || fluxLm <= 0) {
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
        if (cached) {
            if (!ray.inSubLevel() && !SubLevelBridge.resolve(level, blockEntity.getBlockPos()).inSubLevel()) {
                return cache.trace(level, source, sourceRange);
            }

            cache.clear();
        }
        if (collision == null) {
            collision = new LaserCollisionContext(1, cached);
        }
        BeamPropagation result = BeamOcclusionSolver.solve(level, source, PropagationBudget.standard(sourceRange), collision, false);
        if (cached) {
            displayDependencies = LaserTraceObservation.capture(level, result, collision, 1, true);
        }
        return result;
    }
}
