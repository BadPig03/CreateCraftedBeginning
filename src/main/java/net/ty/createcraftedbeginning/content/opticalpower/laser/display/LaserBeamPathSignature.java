package net.ty.createcraftedbeginning.content.opticalpower.laser.display;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Segment;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalApertureFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalHit;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalTarget;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamContext.DisplayState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.OpticalAperture;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserRenderSnapshot;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class LaserBeamPathSignature {
    private LaserBeamPathSignature() {}

    public record Input(DisplayState source, LaserBeamContext context, List<Path> paths, List<Target> targets) {
        public Input {
            paths = List.copyOf(paths);
            targets = List.copyOf(targets);
        }

        public static Input from(LaserRenderSnapshot snapshot) {
            List<Target> targets = snapshot.propagation().segments().stream().map(segment -> new Target(segment.hit() == null ? null : segment.hit().target().blockPos())).toList();
            return new Input(LaserBeamContext.state(snapshot.propagation().source()), LaserBeamContext.from(snapshot), paths(snapshot), targets);
        }

        private static @Unmodifiable List<Path> paths(LaserRenderSnapshot snapshot) {
            List<Path> paths = new ArrayList<>();
            List<Segment> segments = snapshot.propagation().segments();
            for (Segment segment : segments) {
                int parent = segment.parentIndex();
                List<Event> events = new ArrayList<>();
                if (parent >= 0) {
                    events.addAll(paths.get(parent).events());
                    OpticalHit hit = segments.get(parent).hit();
                    if (hit != null && hit.surface().aperture() != null) {
                        Vec3 normal = hit.surface().normal();
                        events.add(new Event(hit.target(), normal, Math.round(hit.position().dot(normal) * 1.0E6), hit.surface().aperture(), hit.surface().apertureFrame(), hit.surface().interactor().getClass().getName()));
                    }
                }
                paths.add(new Path(List.copyOf(events)));
            }
            return List.copyOf(paths);
        }
    }

    public record Target(@Nullable BlockPos pos) {}

    public record Event(OpticalTarget target, Vec3 normal, long plane, OpticalAperture aperture, @Nullable OpticalApertureFrame frame, String interactor) {}

    public record Path(List<Event> events) {
        public Path {
            events = List.copyOf(events);
        }
    }
}
