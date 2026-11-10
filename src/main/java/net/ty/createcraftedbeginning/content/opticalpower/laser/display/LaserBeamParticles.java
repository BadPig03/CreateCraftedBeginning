package net.ty.createcraftedbeginning.content.opticalpower.laser.display;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamGeometry.Section;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamMesh.DrawSection;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamMesh.MappedData;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamMesh.SamplingDomain;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamParticleIndex.FrameWork;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamParticleIndex.IndexWork;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamParticleIndex.ShellCrossing;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamParticleIndex.ShellFace;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamParticleIndex.ShellHit;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamParticlePaths.Path;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamParticleSpace.Interval;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamSurfaces.Face;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.OpticalPlane;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class LaserBeamParticles {
    private static final double SPACING = 0.25;
    private static final double EMISSION_PERIOD_TICKS = 100;
    private static final double FADE_DISTANCE = 0.25;
    private static final int MAX_PARTICLES = 32;
    private static final int MAX_ALPHA = 180;
    private static final double MIN_NORMAL_LENGTH_SQR = 0.5;
    private static final double LANE_MARGIN = 0.15;
    private static final double LANE_SPAN = 0.7;
    private static final double MIN_HALF_SIZE = 0.009;
    private static final double HALF_SIZE_VARIATION = 0.007;
    private static final double CROSS_SECTION_SIZE_FRACTION = 0.08;

    private LaserBeamParticles() {
    }

    public static List<GlowPoint> sample(List<Section> sections, double ticks, long seed, int stride) {
        if (sections.isEmpty() || stride <= 0) {
            return List.of();
        }

        List<ParticleSection> frames = new ArrayList<>();
        double totalLength = 0;
        for (Section section : sections) {
            List<Vec3> starts = section.starts();
            List<Vec3> ends = section.ends();
            Vec3 startCenter = Section.center(starts);
            Vec3 endCenter = Section.center(ends);
            double length = startCenter.distanceTo(endCenter);
            if (length < LaserBeamParticleIndex.MIN_GEOMETRY_LENGTH) {
                continue;
            }

            frames.add(new ParticleSection(section, startCenter, endCenter, length));
            totalLength += length;
        }

        int maxCandidates = (int) Math.ceil(totalLength * MAX_PARTICLES);
        RandomSource random = RandomSource.create(seed);
        double cycle = ticks / EMISSION_PERIOD_TICKS + random.nextDouble();
        long generation = (long) Math.floor(cycle);
        int slot = Math.floorMod(generation, stride);
        double phaseDistance = (cycle - generation) * SPACING;
        if (phaseDistance + slot * SPACING < (double) 0) {
            slot += (int) Math.ceil(((double) 0 - phaseDistance - slot * SPACING) / (stride * SPACING)) * stride;
        }

        double distance = phaseDistance + slot * SPACING;
        double sectionStart = 0;
        int candidates = 0;
        List<GlowPoint> points = new ArrayList<>();
        for (ParticleSection frame : frames) {
            Section section = frame.geometry();
            List<Vec3> starts = section.starts();
            List<Vec3> ends = section.ends();
            Vec3 start = starts.getFirst();
            Vec3 end = ends.getFirst();
            Vec3 startU = starts.get(1).subtract(start);
            Vec3 startV = starts.get(3).subtract(start);
            Vec3 endU = ends.get(1).subtract(end);
            Vec3 endV = ends.get(3).subtract(end);
            Vec3 startCenter = frame.startCenter();
            Vec3 endCenter = frame.endCenter();
            double length = frame.length();
            boolean fadesAtEnd = section.fadesAtEnd();
            double terminalFadeLength = Math.min(LaserBeamGeometry.TERMINAL_FADE_LENGTH, length);
            Vec3 startNormal = startU.cross(startV).normalize();
            Vec3 endNormal = endU.cross(endV).normalize();
            Vec3 direction = endCenter.subtract(startCenter).scale(1 / length);
            if (startNormal.lengthSqr() < MIN_NORMAL_LENGTH_SQR) {
                startNormal = direction;
            }

            if (endNormal.lengthSqr() < MIN_NORMAL_LENGTH_SQR) {
                endNormal = direction;
            }

            double sectionEnd = sectionStart + length;
            while (distance < sectionEnd && candidates < maxCandidates) {
                double progress = Mth.clamp((distance - sectionStart) / length, 0, 1);
                long identity = generation - slot;
                slot += stride;
                distance = phaseDistance + slot * SPACING;
                candidates++;
                random.setSeed(seed ^ identity * 0x9E3779B97F4A7C15L);
                double u = LANE_MARGIN + random.nextDouble() * LANE_SPAN;
                double v = LANE_MARGIN + random.nextDouble() * LANE_SPAN;
                double nominalSize = MIN_HALF_SIZE + random.nextDouble() * HALF_SIZE_VARIATION;
                double minU = SamplingDomain.FULL.minU();
                double minV = SamplingDomain.FULL.minV();
                double maxU = SamplingDomain.FULL.maxU();
                double maxV = SamplingDomain.FULL.maxV();
                if (u < minU || u >= maxU || v < minV || v >= maxV) {
                    continue;
                }

                u = (u - minU) / (maxU - minU);
                v = (v - minV) / (maxV - minV);
                Vec3 acrossU = startU.lerp(endU, progress);
                Vec3 acrossV = startV.lerp(endV, progress);
                Vec3 position = start.lerp(end, progress).add(acrossU.scale(u)).add(acrossV.scale(v));
                double clearance = Math.min(Math.abs(position.subtract(start).dot(startNormal)), Math.abs(position.subtract(end).dot(endNormal)));
                double lengthU = acrossU.length();
                double lengthV = acrossV.length();
                double edgeLength = Math.max(lengthU, lengthV);
                if (edgeLength < LaserBeamParticleIndex.MIN_GEOMETRY_LENGTH) {
                    continue;
                }

                double halfSize = Math.min(nominalSize, Math.min(lengthU / (maxU - minU), lengthV / (maxV - minV)) * CROSS_SECTION_SIZE_FRACTION);
                halfSize = Math.min(halfSize, clearance * LaserBeamParticleIndex.PLANE_CLEARANCE_SIZE_FRACTION);
                double fade = Mth.clamp(clearance / FADE_DISTANCE, 0, 1);
                double terminalFade = 1;
                if (fadesAtEnd) {
                    terminalFade = Mth.clamp((1 - progress) * length / terminalFadeLength, 0, 1);
                }

                int alpha = (int) Math.round(MAX_ALPHA * fade * fade * (3 - 2 * fade) * halfSize / nominalSize * terminalFade);
                if (alpha <= 0 || halfSize <= 0) {
                    continue;
                }

                points.add(new GlowPoint(position, halfSize, alpha));
            }

            sectionStart = sectionEnd;
            if (candidates >= maxCandidates) {
                break;
            }
        }

        return List.copyOf(points);
    }

    public record GlowPoint(Vec3 position, double halfSize, int alpha) {}

    public record DrawPoint(GlowPoint geometry, LaserBeamAppearance appearance, double opacity) {}

    public record FlowPoint(long identity, DrawPoint point, Vec3 anchor, Vec3 direction) {}

    public static final class ShellSampler {
        private static final Vec3 VISIBILITY_RAY = new Vec3(0.73, 0.43, 0.53).normalize();
        private final ShellFace[] frames;
        private final LaserBeamParticleIndex queryIndex;
        private final LaserBeamParticlePaths paths;
        private final @Nullable LaserBeamParticleSpace space;
        private final @Nullable BiPredicate<Face, GlowPoint> visible;

        public ShellSampler(List<Face> surfaces) {
            this(surfaces, null);
        }

        public ShellSampler(List<Face> surfaces, @Nullable BiPredicate<Face, GlowPoint> visible) {
            this(null, surfaces.stream().map(Face::draw).distinct().toList(), surfaces, visible);
        }

        public ShellSampler(LaserBeamContext context, List<DrawSection> sections, List<Face> surfaces) {
            this(context, sections, surfaces, null);
        }

        private ShellSampler(@Nullable LaserBeamContext context, List<DrawSection> sections, List<Face> surfaces, @Nullable BiPredicate<Face, GlowPoint> visible) {
            this.visible = visible;
            frames = new ShellFace[surfaces.size()];
            for (int index = 0; index < surfaces.size(); index++) {
                frames[index] = new ShellFace(surfaces.get(index));
            }

            queryIndex = new LaserBeamParticleIndex(frames, 0, frames.length);
            paths = new LaserBeamParticlePaths(context, sections);
            space = context == null ? null : new LaserBeamParticleSpace(sections, surfaces);
        }

        private ShellSampler(ShellFace[] frames, LaserBeamParticleIndex queryIndex, LaserBeamParticlePaths paths, @Nullable LaserBeamParticleSpace space, @Nullable BiPredicate<Face, GlowPoint> visible) {
            this.visible = visible;
            this.frames = frames;
            this.queryIndex = queryIndex;
            this.paths = paths;
            this.space = space;
        }

        public static LaserBeamWork<ShellSampler> begin(LaserBeamContext context, List<DrawSection> sections, List<Face> surfaces) {
            return new SamplerWork(context, sections, surfaces);
        }

        public ShellSampler withVisibility(BiPredicate<Face, GlowPoint> visibility) {
            return new ShellSampler(frames, queryIndex, paths, space, visibility);
        }

        public List<DrawPoint> sample(double ticks, long seed, int stride) {
            List<DrawPoint> result = new ArrayList<>();
            sample(ticks, seed, stride, point -> result.add(point.point()));
            return List.copyOf(result);
        }

        public List<FlowPoint> sampleFlow(double ticks, long seed, int stride) {
            List<FlowPoint> result = new ArrayList<>();
            sample(ticks, seed, stride, result::add);
            return List.copyOf(result);
        }

        private void sample(double ticks, long seed, int stride, Consumer<FlowPoint> output) {
            if (stride <= 0 || frames.length == 0) {
                return;
            }

            RandomSource random = RandomSource.create(seed);
            double cycle = ticks / EMISSION_PERIOD_TICKS + random.nextDouble();
            long generation = (long) Math.floor(cycle);
            double phaseDistance = (cycle - generation) * SPACING;
            List<Vec3> emitted = new ArrayList<>();
            List<Interval> intervals = new ArrayList<>();
            List<ShellCrossing> crossings = new ArrayList<>();
            for (int slot = Math.floorMod(generation, stride); phaseDistance + slot * SPACING < paths.length(); slot += stride) {
                long identity = generation - slot;
                double distance = phaseDistance + slot * SPACING;
                long particleSeed = seed ^ identity * 0x9E3779B97F4A7C15L;
                random.setSeed(particleSeed);
                double u = 2 * (LANE_MARGIN + random.nextDouble() * LANE_SPAN) - 1;
                double v = 2 * (LANE_MARGIN + random.nextDouble() * LANE_SPAN) - 1;
                double nominalSize = MIN_HALF_SIZE + random.nextDouble() * HALF_SIZE_VARIATION;
                double depthFraction = LANE_MARGIN + random.nextDouble() * LANE_SPAN;
                emitted.clear();
                for (Path path : paths.paths()) {
                    DrawSection draw = path.draw();
                    LaserBeamAppearance appearance = draw.appearance();
                    if (appearance.opacity() == 0) {
                        continue;
                    }

                    Vec3 position = path.sample(u, v, distance);
                    if (position == null) {
                        continue;
                    }

                    Vec3 anchor = position;
                    if (space != null) {
                        Vec3 radial = path.radialDirection(u, v);
                        Interval gap = space.gap(position, radial, intervals);
                        double from = gap.from();
                        double limit = Math.min(gap.to(), path.radialLimit(position, radial)) - from;
                        if (limit <= LaserBeamParticleIndex.MIN_GEOMETRY_LENGTH) {
                            continue;
                        }

                        Vec3 boundary = position.add(radial.scale(from));
                        double depth = queryIndex.exitDistance(frames, boundary, radial, limit, crossings);
                        if (!Double.isFinite(depth) || depth <= LaserBeamParticleIndex.MIN_GEOMETRY_LENGTH) {
                            continue;
                        }

                        LaserBeamBrightness widthField = draw.brightness();
                        MappedData mapped = draw.mapped();
                        if (mapped != null) {
                            widthField = mapped.widthField();
                        }

                        double width = 2 * LaserBeamBrightness.REFERENCE_RADIUS;
                        if (widthField != null) {
                            width = Math.abs(widthField.referenceWidthAt(position));
                        }

                        double offset = Math.min(width * depthFraction, depth * (1 - LANE_MARGIN));
                        position = boundary.add(radial.scale(offset));
                    }

                    Vec3 center = position;
                    if (emitted.stream().anyMatch(previous -> previous.distanceToSqr(center) < LaserBeamParticleIndex.MIN_GEOMETRY_LENGTH * LaserBeamParticleIndex.MIN_GEOMETRY_LENGTH)) {
                        continue;
                    }

                    double halfSize = queryIndex.constrainSize(frames, position, nominalSize);
                    if (space != null) {
                        halfSize = Math.min(nominalSize, Math.max(halfSize, space.shellClearance(position, nominalSize / LaserBeamParticleIndex.PLANE_CLEARANCE_SIZE_FRACTION) * LaserBeamParticleIndex.PLANE_CLEARANCE_SIZE_FRACTION));
                        halfSize = Math.min(halfSize, space.clearance(position, halfSize / LaserBeamParticleIndex.PLANE_CLEARANCE_SIZE_FRACTION) * LaserBeamParticleIndex.PLANE_CLEARANCE_SIZE_FRACTION);
                    }

                    double clearance = Double.POSITIVE_INFINITY;
                    for (OpticalPlane plane : path.endPlanes()) {
                        Vec3 normal = plane.normal();
                        if (normal.lengthSqr() < MIN_NORMAL_LENGTH_SQR) {
                            clearance = 0;
                            break;
                        }

                        clearance = Math.min(clearance, Math.abs(position.subtract(plane.point()).dot(normal)));
                    }

                    halfSize = Math.min(halfSize, clearance * LaserBeamParticleIndex.PLANE_CLEARANCE_SIZE_FRACTION);
                    double fade = Math.clamp(clearance / FADE_DISTANCE, 0, 1);
                    int alpha = (int) Math.round(MAX_ALPHA * fade * fade * (3 - 2 * fade) * halfSize / nominalSize * draw.fadeAt(position));
                    if (alpha <= 0 || halfSize < LaserBeamParticleIndex.MIN_GEOMETRY_LENGTH) {
                        continue;
                    }

                    GlowPoint glow = new GlowPoint(position, halfSize, alpha);
                    if (visible != null) {
                        ShellHit hit = queryIndex.queryHit(frames, position, VISIBILITY_RAY, new ShellHit(null, Double.POSITIVE_INFINITY));
                        ShellFace frame = hit.frame();
                        if (frame == null || !visible.test(frame.face(), glow)) {
                            continue;
                        }
                    }

                    output.accept(new FlowPoint(identity, new DrawPoint(glow, appearance, draw.opacityAt(position)), anchor, path.direction()));
                    emitted.add(position);
                }
            }
        }
    }

    private static final class SamplerWork implements LaserBeamWork<ShellSampler> {
        private final List<Face> surfaces;
        private final ShellFace[] frames;
        private int index;
        private final LaserBeamParticlePaths paths;
        private final LaserBeamParticleSpace space;
        private @Nullable FrameWork frameWork;
        private @Nullable IndexWork indexWork;
        private @Nullable ShellSampler result;

        private SamplerWork(LaserBeamContext context, List<DrawSection> sections, List<Face> surfaces) {
            this.surfaces = surfaces;
            paths = new LaserBeamParticlePaths(context, sections);
            space = new LaserBeamParticleSpace(sections, surfaces);
            frames = new ShellFace[surfaces.size()];
        }

        @Override
        public boolean step() {
            if (result != null) {
                return true;
            }

            if (index < surfaces.size()) {
                FrameWork current = frameWork;
                if (current == null) {
                    frameWork = new FrameWork(surfaces.get(index));
                    return false;
                }

                if (!current.step()) {
                    return false;
                }

                ShellFace frame = current.result();
                frames[index++] = frame;
                frameWork = null;
                return false;
            }

            IndexWork current = indexWork;
            if (current == null) {
                indexWork = new IndexWork(frames);
                return false;
            }

            if (!current.step()) {
                return false;
            }

            result = new ShellSampler(frames, current.result(), paths, space, null);
            return true;
        }

        @Override
        public ShellSampler result() {
            return Objects.requireNonNull(result, "Laser beam particle sampler preparation has not completed.");
        }
    }

    private record ParticleSection(Section geometry, Vec3 startCenter, Vec3 endCenter, double length) {}
}
