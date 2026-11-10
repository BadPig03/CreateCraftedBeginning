package net.ty.createcraftedbeginning.content.opticalpower.laseremitter;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Reason;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Termination;
import net.ty.createcraftedbeginning.content.opticalpower.laser.PropagationBudget;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.BeamOcclusionSolver;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserCollisionContext;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserTraceObservation;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserServerTraceCache {
    private @Nullable Entry entry;

    BeamPropagation trace(Level level, BeamState source, int range) {
        Entry cached = entry;
        if (cached != null && cached.level().get() == level && cached.range() == range) {
            BeamPropagation previous = cached.propagation();
            if (previous.source().equals(source) && cached.dependencies().matchesStaticWorld(level)) {
                return previous;
            }
        }

        entry = null;
        LaserCollisionContext collision = new LaserCollisionContext(1, true);
        BeamPropagation propagation = BeamOcclusionSolver.solve(level, source, PropagationBudget.standard(range), collision, false);
        for (Termination termination : propagation.terminations()) {
            Reason reason = termination.reason();
            if (reason != Reason.ABSORBED && reason != Reason.RANGE_EXHAUSTED && reason != Reason.BELOW_MIN_POWER) {
                return propagation;
            }
        }
        LaserTraceObservation dependencies = LaserTraceObservation.capture(level, propagation, collision, 1, false);
        if (dependencies == null) {
            return propagation;
        }

        entry = new Entry(new WeakReference<>(level), range, propagation, dependencies);
        return propagation;
    }

    void clear() {
        entry = null;
    }

    @Nullable LaserTraceObservation dependencies(BeamPropagation propagation) {
        if (entry == null || entry.propagation() != propagation) {
            return null;
        }

        return entry.dependencies();
    }

    private record Entry(WeakReference<Level> level, int range, BeamPropagation propagation, LaserTraceObservation dependencies) {}
}
