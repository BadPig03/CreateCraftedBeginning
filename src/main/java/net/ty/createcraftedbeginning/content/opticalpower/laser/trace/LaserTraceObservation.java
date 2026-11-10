package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserRigidDisplay.Candidate;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class LaserTraceObservation {
    private final LaserTraceDependencies dependencies;

    private LaserTraceObservation(LaserTraceDependencies dependencies) {
        this.dependencies = dependencies;
    }

    public static @Nullable LaserTraceObservation capture(Level level, BeamPropagation propagation, LaserCollisionContext collision, float partialTick, boolean rendering) {
        LaserTraceDependencies dependencies = collision.getGeometry().capture(level, propagation, partialTick, rendering);
        if (dependencies == null) {
            return null;
        }

        return new LaserTraceObservation(dependencies);
    }

    public boolean matchesStaticWorld(Level level) {
        return dependencies.matchesStaticWorld(level);
    }

    public @Nullable LaserStaticDependencies staticDependencies() {
        return LaserStaticDependencies.from(dependencies);
    }

    public @Nullable Candidate captureRigid(Level level, BlockPos pos, BeamPropagation propagation) {
        return LaserRigidDisplay.capture(level, pos, propagation, dependencies);
    }
}
