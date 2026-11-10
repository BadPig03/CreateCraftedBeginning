package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.OcclusionWork;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Reason;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Segment;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Termination;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalHit;
import net.ty.createcraftedbeginning.content.opticalpower.laser.PropagationBudget;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFootprint;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.BeamAperturePartition.Cut;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.BeamPropagationSolver.Trace;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class BeamOcclusionSolver {
    public static final int MAX_REGIONS = 128;
    private static final double MIN_REGION_FRACTION = 1.0E-5;
    private static final double SAMPLE_INSET = 1.0E-4;
    private static final double HIT_NORMAL_TOLERANCE_SQR = 1.0E-10;
    private static final double HIT_PLANE_TOLERANCE = 1.0E-5;
    private static final double FLUX_RELATIVE_TOLERANCE = 1.0E-10;
    private static final double RADIUS_RELATIVE_TOLERANCE = 0.001;

    private BeamOcclusionSolver() {
    }

    public static BeamPropagation solve(Level level, BeamState source, PropagationBudget budget, float partialTick) {
        return solve(level, source, budget, partialTick, false);
    }

    public static BeamPropagation solve(Level level, BeamState source, PropagationBudget budget, float partialTick, boolean rendering) {
        Objects.requireNonNull(level, "Beam occlusion solving requires a non-null level.");
        return solve(level, source, budget, new LaserCollisionContext(partialTick), rendering);
    }

    public static BeamPropagation solve(Level level, BeamState source, PropagationBudget budget, LaserCollisionContext collision, boolean rendering) {
        float partialTick = collision.getPartialTick();
        if (!Float.isFinite(partialTick) || partialTick < 0 || partialTick > 1) {
            throw new IllegalArgumentException("Occlusion partial tick must be finite and in [0, 1]; got " + partialTick + '.');
        }

        double flux = source.fluxLm();
        double minimumFlux = budget.minFluxLm();
        if (flux < minimumFlux) {
            return new BeamPropagation(source, List.of(), List.of(new Termination(-1, source, Reason.BELOW_MIN_POWER)));
        }

        int maxSegments = budget.maxSegments();
        int limit = Math.min(MAX_REGIONS, Math.min(budget.maxBeams(), maxSegments));
        BeamRayQuery query = new BeamRayQuery(level, collision, rendering, maxSegments);
        BeamVolumeQuery volume = new BeamVolumeQuery(level, collision, rendering);
        ArrayDeque<Cell> pending = new ArrayDeque<>();
        pending.add(new Cell(-1, -1, 1, 1));
        List<Segment> segments = new ArrayList<>();
        List<Termination> terminations = new ArrayList<>();
        int regions = 1;
        while (!pending.isEmpty()) {
            Cell cell = pending.removeFirst();
            double left = cell.left();
            double right = cell.right();
            double bottom = cell.bottom();
            double top = cell.top();
            double width = right - left;
            double height = top - bottom;
            BeamState beam = region(source, cell, (left + right) / 2, (bottom + top) / 2);
            if (beam.fluxLm() < minimumFlux) {
                terminations.add(new Termination(-1, beam, Reason.BELOW_MIN_POWER));
                continue;
            }

            if (query.exhausted() || segments.size() >= maxSegments) {
                terminations.add(new Termination(-1, beam, Reason.OCCLUSION_LIMIT));
                continue;
            }

            Trace centerTrace = new Trace(beam, budget, List.of(beam));
            List<Trace> samples = new ArrayList<>();
            samples.add(centerTrace);
            for (int corner = 0; corner < 4; corner++) {
                double x = left + width * SAMPLE_INSET;
                double y = bottom + height * SAMPLE_INSET;
                if ((corner & 1) != 0) {
                    x = right - width * SAMPLE_INSET;
                }
                if ((corner & 2) != 0) {
                    y = top - height * SAMPLE_INSET;
                }
                BeamState state = region(source, cell, x, y);
                samples.add(new Trace(state, budget, List.of(state)));
            }
            boolean uniform = true;
            Cut cut = null;
            while (uniform && cut == null && !query.exhausted() && !centerTrace.isComplete()) {
                for (Trace sample : samples) {
                    sample.advance(query);
                    uniform &= samePath(centerTrace, sample);
                }
                cut = BeamAperturePartition.find(source, cell, samples);
            }
            Trace bottomLeft = samples.get(1);
            Trace bottomRight = samples.get(2);
            Trace topLeft = samples.get(3);
            Trace topRight = samples.get(4);
            boolean horizontalChange = !samePath(bottomLeft, bottomRight) || !samePath(topLeft, topRight);
            boolean verticalChange = !samePath(bottomLeft, topLeft) || !samePath(bottomRight, topRight);
            BeamPropagation center = null;
            List<BeamPropagation> corners = new ArrayList<>();
            if (uniform && cut == null && !query.exhausted()) {
                center = centerTrace.result();
                for (int corner = 1; corner < samples.size(); corner++) {
                    corners.add(samples.get(corner).result());
                }
            }
            List<Segment> accepted = null;
            boolean partitioned = false;
            if (cut == null && !query.exhausted()) {
                BeamPropagation partition = null;
                if (!uniform && beam.profile().widthSlope() == 0) {
                    partition = BeamPlanarPartition.solve(level, beam, budget, collision, rendering, query, limit - regions + 1, maxSegments - segments.size());
                }
                if (partition == null) {
                    partition = BeamTerminalPartition.solve(level, beam, budget, collision, rendering, samples, query, volume, limit - regions + 1, maxSegments - segments.size());
                }
                if (partition != null) {
                    center = partition;
                    accepted = partition.segments();
                    regions += partition.occlusionWork().sampledRegions() - 1;
                    partitioned = true;
                }
            }
            if (center != null && !partitioned) {
                ContactResolution resolution = resolveContacts(center.segments(), cell);
                accepted = resolution.segments();
                cut = resolution.cut();
            }
            if (center != null && accepted != null && (partitioned || !volume.hasUnseenObstacle(center, corners))) {
                if (segments.size() + accepted.size() > maxSegments) {
                    terminations.add(new Termination(-1, beam, Reason.SEGMENT_LIMIT));
                    continue;
                }

                int offset = segments.size();
                for (Segment segment : accepted) {
                    int parent = segment.parentIndex();
                    if (parent >= 0) {
                        parent += offset;
                    }
                    segments.add(new Segment(parent, segment.start(), segment.incident(), segment.length(), segment.remainingRange(), segment.hit(), segment.absorbedFluxLm(), segment.contact()));
                }
                for (Termination termination : center.terminations()) {
                    int parent = termination.parentIndex();
                    if (parent >= 0) {
                        parent += offset;
                    }
                    terminations.add(new Termination(parent, termination.state(), termination.reason()));
                }
                continue;
            }

            if (regions >= limit || query.exhausted() || Math.max(width, height) <= MIN_REGION_FRACTION) {
                terminations.add(new Termination(-1, beam, Reason.OCCLUSION_LIMIT));
                continue;
            }

            boolean horizontal = width >= height;
            if (horizontalChange != verticalChange) {
                horizontal = horizontalChange;
            }
            double middle = (bottom + top) / 2;
            if (horizontal && width > MIN_REGION_FRACTION) {
                middle = (left + right) / 2;
            }
            else {
                horizontal = false;
            }
            if (cut != null) {
                horizontal = cut.horizontal();
                middle = cut.coordinate();
            }
            if (horizontal) {
                pending.addLast(new Cell(left, bottom, middle, top));
                pending.addLast(new Cell(middle, bottom, right, top));
            }
            else {
                pending.addLast(new Cell(left, bottom, right, middle));
                pending.addLast(new Cell(left, middle, right, top));
            }
            regions++;
        }
        BeamPropagation result = new BeamPropagation(source, segments, terminations, new OcclusionWork(regions, query.queries()));
        double accounted = result.getAbsorbedFluxLm() + result.getRemainingFluxLm();
        if (Math.abs(accounted - flux) > flux * FLUX_RELATIVE_TOLERANCE) {
            throw new IllegalStateException("Regional propagation must conserve flux; got input=" + flux + ", accounted=" + accounted + " lm.");
        }

        return result;
    }

    static boolean samePath(Trace first, Trace second, int prefixLength) {
        List<Segment> left = first.segments();
        List<Segment> right = second.segments();
        List<Termination> firstEnds = first.terminations();
        List<Termination> secondEnds = second.terminations();
        if (first.isComplete() != second.isComplete() || left.size() != right.size() || firstEnds.size() != secondEnds.size()) {
            return false;
        }

        for (int index = prefixLength; index < left.size(); index++) {
            Segment firstSegment = left.get(index);
            Segment secondSegment = right.get(index);
            BeamState firstIncident = firstSegment.incident();
            BeamState secondIncident = secondSegment.incident();
            OpticalHit a = firstSegment.hit();
            OpticalHit b = secondSegment.hit();
            if (a != null && b != null && a.target().equals(b.target()) && b.surface().apertureFrame() != null && secondSegment.absorbedFluxLm() == secondIncident.fluxLm() && BeamAperturePartition.isAbsorbed(firstSegment)) {
                continue;
            }

            if (!sameHit(a, b)) {
                return false;
            }

            if (a == null || b == null) {
                continue;
            }

            double radius = firstIncident.profile().radius();
            double otherRadius = secondIncident.profile().radius();
            if (Math.abs(radius - otherRadius) > radius * RADIUS_RELATIVE_TOLERANCE) {
                return false;
            }
        }
        for (int index = 0; index < firstEnds.size(); index++) {
            if (firstEnds.get(index).reason() != secondEnds.get(index).reason()) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameHit(@Nullable OpticalHit a, @Nullable OpticalHit b) {
        if (a == null || b == null) {
            return a == b;
        }

        Vec3 normal = a.surface().normal();
        return a.target().equals(b.target()) && normal.distanceToSqr(b.surface().normal()) <= HIT_NORMAL_TOLERANCE_SQR && Math.abs(a.position().subtract(b.position()).dot(normal)) <= HIT_PLANE_TOLERANCE;
    }

    private static ContactResolution resolveContacts(List<Segment> segments, Cell cell) {
        List<Segment> resolved = new ArrayList<>(segments.size());
        for (Segment segment : segments) {
            BeamState start = segment.start();
            OpticalHit hit = segment.hit();
            if (hit == null || start.profile().widthSlope() == 0) {
                resolved.add(segment);
                continue;
            }

            double remainingRange = segment.remainingRange();
            BeamContact contact = BeamContact.create(start, remainingRange, hit);
            if (contact == null) {
                return new ContactResolution(null, null);
            }

            double absorbedFluxLm = segment.absorbedFluxLm();
            if (absorbedFluxLm > 0 && !contact.uniformDensity()) {
                boolean horizontal = contact.splitHorizontal();
                double coordinate = horizontal ? (cell.left() + cell.right()) / 2 : (cell.bottom() + cell.top()) / 2;
                return new ContactResolution(null, new Cut(horizontal, coordinate));
            }

            resolved.add(new Segment(segment.parentIndex(), start, segment.incident(), segment.length(), remainingRange, hit, absorbedFluxLm, contact));
        }
        return new ContactResolution(List.copyOf(resolved), null);
    }

    private static BeamState region(BeamState source, Cell cell, double x, double y) {
        BeamFrame frame = source.frame();
        BeamFootprint original = source.footprint();
        BeamProfile profile = source.profile();
        double radius = profile.radius();
        double horizontalScale = original.horizontalScale();
        double verticalScale = original.verticalScale();
        Vec3 transverse = frame.transverseAxis();
        double offsetX = horizontalScale * x;
        double offsetY = verticalScale * y;
        Vec3 origin = frame.origin().add(transverse.scale(radius * offsetX)).add(frame.getSecondTransverseAxis().scale(radius * offsetY));
        double scaleX = (cell.right() - cell.left()) / 2;
        double scaleY = (cell.top() - cell.bottom()) / 2;
        BeamFootprint footprint = new BeamFootprint(horizontalScale * scaleX, verticalScale * scaleY, original.horizontalOffset() + offsetX, original.verticalOffset() + offsetY);
        return new BeamState(new BeamFrame(origin, frame.direction(), transverse), profile, source.fluxLm() * scaleX * scaleY, footprint);
    }

    private static boolean samePath(Trace first, Trace second) {
        return samePath(first, second, 0);
    }

    record Cell(double left, double bottom, double right, double top) {}

    private record ContactResolution(@Nullable List<Segment> segments, @Nullable Cut cut) {}

}
