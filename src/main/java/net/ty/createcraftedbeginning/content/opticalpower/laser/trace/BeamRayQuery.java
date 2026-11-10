package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalHit;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFootprint;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile.Span;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.BeamPropagationSolver.BeamQuery;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BeamRayQuery implements BeamQuery {
    private final Level level;
    private final LaserCollisionContext collision;
    private final boolean rendering;
    private final int limit;
    private final Map<Ray, @Nullable OpticalQueryResult> results = new HashMap<>();
    private int queries;
    private boolean exhausted;

    BeamRayQuery(Level level, LaserCollisionContext collision, boolean rendering, int limit) {
        this.level = level;
        this.collision = collision;
        this.rendering = rendering;
        this.limit = limit;
    }

    @Override
    public @Nullable OpticalQueryResult findNext(BeamState beam, double range) {
        BeamFootprint footprint = beam.footprint();
        BeamProfile profile = beam.profile();
        if (profile.widthSlope() == 0 || footprint.horizontalOffset() == 0 && footprint.verticalOffset() == 0) {
            return query(beam.frame(), range, true, true, false);
        }

        List<Span> spans = profile.spans(range);
        for (Span span : spans) {
            double from = span.from();
            double to = span.to();
            Vec3 start = beam.advance(from).frame().origin();
            Vec3 end = beam.advance(to).frame().origin();
            Vec3 delta = end.subtract(start);
            double length = delta.length();
            if (length == 0) {
                continue;
            }

            OpticalQueryResult result = query(BeamFrame.fromDirection(start, delta.scale(1 / length)), length, from == 0, false, to < range);
            if (result == null) {
                if (from == 0) {
                    return null;
                }

                return new OpticalQueryResult(from, null);
            }

            OpticalHit hit = result.hit();
            double traveled = result.length();
            if (traveled > length) {
                throw new IllegalStateException("Optical query exceeded chord length " + length + "; got " + traveled + " blocks.");
            }

            if (hit == null && traveled == length) {
                continue;
            }

            double fraction = traveled / length;
            return new OpticalQueryResult(Math.clamp(from + (to - from) * fraction, from, to), hit);
        }
        return new OpticalQueryResult(range, null);
    }

    int queries() {
        return queries;
    }

    boolean exhausted() {
        return exhausted;
    }

    private @Nullable OpticalQueryResult query(BeamFrame frame, double range, boolean skipStart, boolean trimEnd, boolean closeEnd) {
        Ray ray = new Ray(frame, range, skipStart, trimEnd, closeEnd);
        OpticalQueryResult cached = results.get(ray);
        if (cached != null || results.containsKey(ray)) {
            return cached;
        }

        if (queries >= limit) {
            exhausted = true;
            return null;
        }

        queries++;
        OpticalQueryResult result = OpticalWorldQuery.findNext(level, frame, range, collision, rendering, skipStart, trimEnd, closeEnd);
        results.put(ray, result);
        return result;
    }

    private record Ray(BeamFrame frame, double range, boolean skipStart, boolean trimEnd, boolean closeEnd) {}
}
