package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.RenderSource;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class LaserRenderTraceCache {
    private @Nullable Entry entry;
    private @Nullable SourceObservation sourceObservation;
    private @Nullable MotionObservation motionObservation;

    public @Nullable LaserRenderGeometry captureGeometry(LaserRenderSnapshot snapshot) {
        Entry cached = entry;
        if (cached == null || cached.snapshot() != snapshot || cached.renderSource() != null) {
            return null;
        }

        LaserTraceDependencies dependencies = cached.dependencies();
        if (!dependencies.subLevels().isEmpty() || !dependencies.contraptions().isEmpty()) {
            return null;
        }

        return new LaserRenderGeometry(cached.level(), dependencies);
    }

    public void clear() {
        entry = null;
        sourceObservation = null;
        motionObservation = null;
    }

    public LaserRenderSnapshot trace(Level level, BeamState source, Vec3 origin, double range, float partialTick) {
        if (!Float.isFinite(partialTick) || partialTick < 0 || partialTick > 1) {
            throw new IllegalArgumentException("Render trace partial tick must be finite and in [0, 1]; got " + partialTick + '.');
        }

        if (!(level instanceof ClientLevel)) {
            clear();
            return LaserRenderSnapshot.trace(level, source, origin, range, partialTick);
        }

        Vec3 anchor = origin.add(0.5, 0.5, 0.5);
        RenderSource renderSource = null;
        if (SubLevelBridge.resolve(level, anchor).inSubLevel()) {
            renderSource = SubLevelBridge.captureRenderSource(level, anchor, partialTick);
            if (renderSource == null) {
                clear();
                return LaserRenderSnapshot.trace(level, source, origin, range, partialTick);
            }

            SourceObservation observed = sourceObservation;
            if (observed == null || observed.level().get() != level || !observed.source().equals(renderSource)) {
                sourceObservation = new SourceObservation(new WeakReference<>(level), renderSource);
                entry = null;
                motionObservation = null;
                return LaserRenderSnapshot.trace(level, source, origin, range, partialTick);
            }
        }
        else {
            sourceObservation = null;
        }

        MotionObservation observed = motionObservation;
        motionObservation = null;
        if (observed != null) {
            WeakReference<Level> observedLevel = observed.level();
            if (observedLevel.get() == level && observed.source().equals(source) && observed.origin().equals(origin) && observed.range() == range) {
                LaserTraceMotion motion = observed.motion();
                LaserTraceMotion current = motion.observe(level, partialTick);
                if (current == null) {
                    return LaserRenderSnapshot.trace(level, source, origin, range, partialTick);
                }

                if (!motion.equals(current)) {
                    motionObservation = new MotionObservation(observedLevel, source, origin, range, current);
                    return LaserRenderSnapshot.trace(level, source, origin, range, partialTick);
                }
            }
        }

        Entry cached = entry;
        if (cached != null && cached.source().equals(source) && cached.origin().equals(origin) && cached.range() == range && Objects.equals(cached.renderSource(), renderSource)) {
            WeakReference<Level> cachedLevel = cached.level();
            if (cachedLevel.get() == level) {
                LaserTraceDependencies dependencies = cached.dependencies();
                if (dependencies.matches(level, partialTick)) {
                    return cached.snapshot();
                }

                LaserTraceMotion motion = new LaserTraceMotion(dependencies.bounds(), dependencies.paths(), dependencies.subLevels(), dependencies.contraptions());
                LaserTraceMotion current = motion.observe(level, partialTick);
                if (current != null && !motion.equals(current)) {
                    entry = null;
                    motionObservation = new MotionObservation(cachedLevel, source, origin, range, current);
                    return LaserRenderSnapshot.trace(level, source, origin, range, partialTick);
                }
            }
        }

        entry = null;
        LaserCollisionContext collision = new LaserCollisionContext(partialTick, true);
        LaserRenderSnapshot snapshot = LaserRenderSnapshot.trace(level, source, origin, range, collision);
        LaserTraceDependencies dependencies = collision.getGeometry().capture(level, snapshot.propagation(), partialTick, true);
        if (dependencies == null) {
            return snapshot;
        }

        entry = new Entry(new WeakReference<>(level), source, origin, range, renderSource, dependencies, snapshot);
        return snapshot;
    }

    private record Entry(WeakReference<Level> level, BeamState source, Vec3 origin, double range, @Nullable RenderSource renderSource, LaserTraceDependencies dependencies, LaserRenderSnapshot snapshot) {}

    private record SourceObservation(WeakReference<Level> level, RenderSource source) {}

    private record MotionObservation(WeakReference<Level> level, BeamState source, Vec3 origin, double range, LaserTraceMotion motion) {}
}
