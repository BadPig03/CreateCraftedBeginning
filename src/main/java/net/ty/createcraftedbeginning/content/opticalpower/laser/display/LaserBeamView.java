package net.ty.createcraftedbeginning.content.opticalpower.laser.display;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamContext.DisplayFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamContext.DisplaySegment;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamContext.DisplayState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamGeometry.Section;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamMesh.DrawSection;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class LaserBeamView {
    private final LaserBeamContext context;
    private final double sourceRadius;
    private final boolean uniform;
    private final List<Span> spans;

    private LaserBeamView(LaserBeamContext context, double sourceRadius, boolean uniform, List<Span> spans) {
        this.context = context;
        this.sourceRadius = sourceRadius;
        this.uniform = uniform;
        this.spans = List.copyOf(spans);
    }

    public static LaserBeamView create(LaserBeamContext context, List<DrawSection> sections) {
        List<DisplaySegment> segments = context.segments();
        double sourceRadius = context.sourceRadius();
        boolean uniform = segments.stream().allMatch(segment -> {
            BeamProfile profile = segment.start().profile();
            return profile.widthSlope() == 0 && profile.radius() == sourceRadius;
        });
        if (uniform && !sections.isEmpty()) {
            return new LaserBeamView(context, sourceRadius, true, List.of());
        }

        List<Span> spans = new ArrayList<>(sections.size());
        for (DrawSection draw : sections) {

            DisplaySegment segment = segments.get(draw.segmentIndex());
            DisplayState beam = segment.start();
            LaserBeamDisplayProfile display = LaserBeamDisplayProfile.fromBeam(beam);
            DisplayFrame frame = beam.frame();
            Vec3 origin = frame.origin();
            Vec3 direction = frame.direction();
            Section geometry = draw.geometry();
            List<List<Vec3>> caps = List.of(geometry.starts(), geometry.ends());
            List<Vec3> centers = new ArrayList<>(2);
            double[] radii = new double[2];
            for (int index = 0; index < caps.size(); index++) {
                List<Vec3> cap = caps.get(index);
                centers.add(context.toWorldPosition(Section.center(cap)));
                for (Vec3 vertex : cap) {
                    Vec3 point = context.toWorldPosition(vertex);
                    double distance = Math.clamp(point.subtract(origin).dot(direction), 0, segment.remainingRange());
                    radii[index] = Math.max(radii[index], display.radiusAt(distance));
                }
            }

            Vec3 start = centers.getFirst();
            spans.add(new Span(start, centers.get(1).subtract(start), radii[0], radii[1]));
        }

        return new LaserBeamView(context, sourceRadius, false, spans);
    }

    public static LaserBeamWork<LaserBeamView> begin(LaserBeamContext context, List<DrawSection> sections) {
        return new ViewWork(context, sections);
    }

    public double radiusDistanceRatio(Vec3 position) {
        if (uniform) {
            return sourceRadius / Math.max(sourceRadius, context.distanceTo(position));
        }

        double maximum = 0;
        for (Span span : spans) {
            double ratio = Math.max(span.ratioAt(position, 0), span.ratioAt(position, 1));
            Vec3 origin = span.origin();
            Vec3 delta = span.delta();
            double x = origin.x - position.x;
            double y = origin.y - position.y;
            double z = origin.z - position.z;
            double q0 = x * x + y * y + z * z;
            double q1 = x * delta.x + y * delta.y + z * delta.z;
            double q2 = delta.lengthSqr();
            double radius = span.startRadius();
            double change = span.endRadius() - radius;
            double denominator = radius * q2 - change * q1;
            if (denominator != 0) {
                double fraction = (change * q0 - radius * q1) / denominator;
                if (fraction > 0 && fraction < 1) {
                    ratio = Math.max(ratio, span.ratioAt(position, fraction));
                }
            }

            maximum = Math.max(maximum, ratio);
        }

        return maximum;
    }

    private static final class ViewWork implements LaserBeamWork<LaserBeamView> {
        private final LaserBeamContext context;
        private final List<DrawSection> sections;
        private final List<DisplaySegment> segments;
        private final double sourceRadius;
        private final List<Span> spans = new ArrayList<>();
        private int phase;
        private int index;
        private int cap;
        private int vertex;
        private Vec3 center = Vec3.ZERO;
        private final Vec3[] centers = new Vec3[2];
        private final double[] radii = new double[2];
        private @Nullable DrawSection current;
        private @Nullable LaserBeamDisplayProfile display;
        private @Nullable DisplayFrame frame;
        private @Nullable LaserBeamView result;

        private ViewWork(LaserBeamContext context, List<DrawSection> sections) {
            this.context = context;
            this.sections = sections;
            segments = context.segments();
            sourceRadius = context.sourceRadius();
        }

        @Override
        public boolean step() {
            if (result != null) {
                return true;
            }

            if (phase == 0) {
                if (index < segments.size()) {
                    BeamProfile profile = segments.get(index++).start().profile();
                    if (profile.widthSlope() != 0 || profile.radius() != sourceRadius) {
                        index = 0;
                        phase = 2;
                    }

                    return false;
                }

                index = 0;
                phase = 1;
                return false;
            }

            if (phase == 1) {
                if (sections.isEmpty()) {
                    phase = 2;
                    return false;
                }

                result = new LaserBeamView(context, sourceRadius, true, List.of());
                return true;
            }

            if (index >= sections.size()) {
                result = new LaserBeamView(context, sourceRadius, false, spans);
                return true;
            }

            DrawSection draw = current;
            if (draw == null) {
                draw = sections.get(index);
                current = draw;
                cap = 0;
                vertex = 0;
                center = Vec3.ZERO;
                radii[0] = 0;
                radii[1] = 0;
                DisplayState beam = segments.get(draw.segmentIndex()).start();
                display = LaserBeamDisplayProfile.fromBeam(beam);
                frame = beam.frame();

                return false;
            }

            List<Vec3> points = cap == 0 ? draw.geometry().starts() : draw.geometry().ends();
            if (vertex < points.size()) {
                Vec3 point = points.get(vertex++);
                center = center.add(point);
                DisplayFrame beamFrame = Objects.requireNonNull(frame, "Whole-beam radius sampling requires a beam frame.");
                Vec3 world = context.toWorldPosition(point);
                double distance = Math.clamp(world.subtract(beamFrame.origin()).dot(beamFrame.direction()), 0, segments.get(draw.segmentIndex()).remainingRange());
                double radius = Objects.requireNonNull(display, "Whole-beam radius sampling requires a display profile.").radiusAt(distance);
                radii[cap] = Math.max(radii[cap], radius);
                return false;
            }

            centers[cap] = context.toWorldPosition(center.scale(1.0 / points.size()));
            if (cap == 0) {
                cap = 1;
                vertex = 0;
                center = Vec3.ZERO;
                return false;
            }

            Vec3 start = centers[0];
            spans.add(new Span(start, centers[1].subtract(start), radii[0], radii[1]));
            index++;
            current = null;
            display = null;
            frame = null;
            return false;
        }

        @Override
        public LaserBeamView result() {
            return Objects.requireNonNull(result, "Laser beam view preparation has not completed.");
        }
    }

    private record Span(Vec3 origin, Vec3 delta, double startRadius, double endRadius) {
        private double ratioAt(Vec3 position, double fraction) {
            double x = origin.x - position.x + delta.x * fraction;
            double y = origin.y - position.y + delta.y * fraction;
            double z = origin.z - position.z + delta.z * fraction;
            double radius = startRadius + (endRadius - startRadius) * fraction;
            if (radius <= 0) {
                return 0;
            }

            return radius / Math.max(radius, Math.sqrt(x * x + y * y + z * z));
        }
    }
}
