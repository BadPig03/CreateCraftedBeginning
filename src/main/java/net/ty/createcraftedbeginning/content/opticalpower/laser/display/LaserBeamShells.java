package net.ty.createcraftedbeginning.content.opticalpower.laser.display;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamContext.DisplayFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamContext.DisplaySegment;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamContext.DisplayState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamContours.Budget;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamContours.Result;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamGeometry.Section;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamMesh.DrawSection;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamSurfaces.Face;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.OpticalPlane;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.ToDoubleFunction;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class LaserBeamShells {
    private static final double EPSILON = LaserBeamSurfaces.EPSILON;
    private static final double OUTER_THICKNESS_RATIO = 3;
    private static final double INNER_THICKNESS_RATIO = 0.5;
    private static final double MAX_OFFSET_THICKNESS_RATIO = 4;
    private static final double OFFSET_BLEND_RATE = 2;
    private static final List<Double> SCALES = List.of(1.0, 0.5, 0.125);
    private static final int MAX_PATCH_DEPTH = 6;
    private static final double MIN_PATCH_WIDTH_RATIO = 4;
    private static final double TRANSITION_INSET = 0.5;
    private static final int MIN_PATCH_FACES = 2;
    private static final int MIN_PATCH_VERTICES = 8;
    private static final int MAX_SUBMISSION_FACES = 8192;
    private static final int MAX_SUBMISSION_VERTICES = 32768;
    private static final int POINT_QUERY_LEAF_SIZE = 8;

    private LaserBeamShells() {
    }

    public static ShellMesh build(LaserBeamContext context, List<Face> core) {
        return build(context, core, new Budget());
    }

    public static ShellMesh build(LaserBeamContext context, List<Face> core, Budget budget) {
        if (core.isEmpty()) {
            return new ShellMesh(List.of(List.of(), List.of(), core), 3, ShellReason.EMPTY);
        }

        Result merged = LaserBeamContours.merge(core, context.segments(), budget);
        core = merged.faces();
        if (core.size() > LaserBeamContours.MAX_FACES) {
            return new ShellMesh(List.of(List.of(), List.of(), List.of()), 3, ShellReason.SUBMISSION_BUDGET);
        }

        int vertices = LaserBeamContours.submittedVertices(core);
        if (vertices > LaserBeamContours.MAX_VERTICES) {
            return new ShellMesh(List.of(List.of(), List.of(), List.of()), 3, ShellReason.SUBMISSION_BUDGET);
        }

        List<List<Face>> fallback = List.of(List.of(), List.of(), core);
        boolean partial = false;
        List<Face> candidates = new ArrayList<>();
        Map<DrawSection, Boolean> renderability = new IdentityHashMap<>();
        for (Face face : core) {
            DrawSection draw = face.draw();
            if (!renderability.computeIfAbsent(draw, section -> section.geometry().isRenderable(section.getReferenceWidth()))) {
                partial = true;
                continue;
            }

            candidates.add(face);
        }

        if (partial || !merged.complete()) {
            candidates.sort(Comparator.comparingDouble(LaserBeamContours::area).reversed());
        }

        List<Face> contour = List.copyOf(candidates);
        ContourPointIndex pointIndex = new ContourPointIndex(context, contour);
        Map<Vec3, @Nullable Vec3> offsets = new LinkedHashMap<>();
        double sourceRadius = context.sourceRadius();
        List<Face> outer = new ArrayList<>();
        List<Face> inner = new ArrayList<>();
        boolean localized = false;
        ArrayDeque<PendingPatch> pending = new ArrayDeque<>();
        ArrayDeque<PendingPatch> narrowing = new ArrayDeque<>();
        for (Face face : contour) {
            pending.addLast(new PendingPatch(face, 0));
        }

        while (!pending.isEmpty() || !narrowing.isEmpty()) {
            if (vertices + MIN_PATCH_VERTICES> MAX_SUBMISSION_VERTICES || core.size() + outer.size() + inner.size() + MIN_PATCH_FACES> MAX_SUBMISSION_FACES) {
                partial = true;
                break;
            }

            if (!budget.spend(1)) {
                partial = true;
                break;
            }

            boolean narrow = pending.isEmpty();
            PendingPatch current;
            if (narrow) {
                current = narrowing.removeFirst();
            }
            else {
                current = pending.removeFirst();
            }

            Face face = current.face();
            DisplaySegment segment = context.segments().get(face.draw().segmentIndex());
            DisplayState beam = segment.start();
            BeamProfile profile = beam.profile();
            double radius = profile.radius();
            ToDoubleFunction<Vec3> thickness = point -> radius * OUTER_THICKNESS_RATIO;
            if (profile.widthSlope() != 0) {
                DisplayFrame frame = beam.frame();
                LaserBeamDisplayProfile display = LaserBeamDisplayProfile.fromBeam(beam);
                thickness = point -> {
                    Vec3 world = context.toWorldPosition(point);
                    double distance = Math.clamp(world.subtract(frame.origin()).dot(frame.direction()), 0, segment.remainingRange());
                    return display.radiusAt(distance) * OUTER_THICKNESS_RATIO;
                };
            }

            Vec3 direction = face.endCenter().subtract(face.startCenter()).normalize();
            List<Vec3> faceVertices = face.vertices();
            Vec3 anchor = faceVertices.getFirst();
            double minimum = 0;
            double maximum = 0;
            for (Vec3 point : faceVertices) {
                double distance = point.subtract(anchor).dot(direction);
                minimum = Math.min(minimum, distance);
                maximum = Math.max(maximum, distance);
            }

            int depth = current.depth();
            boolean canSplit = !narrow && depth < MAX_PATCH_DEPTH && maximum - minimum > sourceRadius * MIN_PATCH_WIDTH_RATIO;
            boolean accepted = false;
            boolean submissionRejected = false;
            for (double scale : SCALES) {
                if (budget.isExhausted()) {
                    break;
                }

                if (scale < 1 != narrow) {
                    continue;
                }

                ShellPatch patch = createPatch(face, pointIndex, offsets, thickness, scale, budget);
                if (patch == null) {
                    continue;
                }

                List<Face> outerPart = patch.outer();
                List<Face> innerPart = patch.inner();
                int addedVertices = LaserBeamContours.submittedVertices(outerPart) + LaserBeamContours.submittedVertices(innerPart);
                if (vertices + addedVertices> MAX_SUBMISSION_VERTICES || core.size() + outer.size() + inner.size() + outerPart.size() + innerPart.size() > MAX_SUBMISSION_FACES) {
                    submissionRejected = true;
                    break;
                }

                outer.addAll(outerPart);
                inner.addAll(innerPart);
                vertices += addedVertices;
                localized |= scale < 1;
                accepted = true;
                break;
            }

            if (accepted) {
                continue;
            }

            if (submissionRejected) {
                partial = true;
                continue;
            }

            localized = true;
            boolean exhausted = budget.isExhausted();
            if (!canSplit || exhausted) {
                if (!narrow && !exhausted) {
                    narrowing.addLast(current);
                }
                else {
                    partial = true;
                }

                continue;
            }

            Vec3 middle = anchor.add(direction.scale((minimum + maximum) * 0.5));
            Face first = LaserBeamContours.planarPolygon(face, LaserBeamSurfaces.clip(faceVertices, middle, direction, true));
            Face second = LaserBeamContours.planarPolygon(face, LaserBeamSurfaces.clip(faceVertices, middle, direction, false));
            if (first == null || second == null) {
                partial = true;
                continue;
            }

            pending.addLast(new PendingPatch(first, depth + 1));
            pending.addLast(new PendingPatch(second, depth + 1));
        }

        if (outer.isEmpty()) {
            return new ShellMesh(fallback, 3, ShellReason.LOCAL_FALLBACK);
        }

        int quality = 0;
        ShellReason reason = ShellReason.FULL;
        if (localized) {
            quality = 2;
            reason = ShellReason.LOCAL_NARROWED;
        }

        if (partial) {
            quality = 2;
            reason = ShellReason.PARTIAL;
        }

        return new ShellMesh(List.of(outer, inner, core), quality, reason);
    }

    static LaserBeamWork<ShellMesh> begin(LaserBeamContext context, List<Face> core, Budget budget, @Nullable ShellReuse previous, ShellReuse next) {
        return new Build(context, core, budget, previous, next);
    }

    private static final class Build implements LaserBeamWork<ShellMesh> {
        private final LaserBeamContext context;
        private final Budget budget;
        private final @Nullable ShellReuse previous;
        private final ShellReuse next;
        private List<Face> core;
        private Result merged;
        private @Nullable LaserBeamWork<Result> contourWork;
        private List<List<Face>> fallback;
        private final List<Face> candidates = new ArrayList<>();
        private final Map<DrawSection, Boolean> renderability = new IdentityHashMap<>();
        private final Map<Vec3, @Nullable Vec3> offsets = new LinkedHashMap<>();
        private ContourPointIndex pointIndex;
        private @Nullable BoundaryWork boundaryWork;
        private final List<Face> outer = new ArrayList<>();
        private final List<Face> inner = new ArrayList<>();
        private final List<Face> outerHalo = new ArrayList<>();
        private final List<Face> innerHalo = new ArrayList<>();
        private final ArrayDeque<PendingPatch> pending = new ArrayDeque<>();
        private final ArrayDeque<PendingPatch> narrowing = new ArrayDeque<>();
        private final double sourceRadius;
        private int phase;
        private int index;
        private int vertices;
        private boolean partial;
        private boolean localized;
        private ShellMesh result;

        private Build(LaserBeamContext context, List<Face> core, Budget budget, @Nullable ShellReuse previous, ShellReuse next) {
            this.context = context;
            this.core = core;
            this.budget = budget;
            this.previous = previous;
            this.next = next;
            sourceRadius = context.sourceRadius();
        }

        @Override
        public boolean step() {
            if (result != null) {
                return true;
            }

            if (phase == 0) {
                if (core.isEmpty()) {
                    result = new ShellMesh(List.of(List.of(), List.of(), core), 3, ShellReason.EMPTY);
                    return true;
                }

                if (contourWork == null) {
                    contourWork = LaserBeamContours.beginMerge(core, context.segments(), budget);
                    return false;
                }

                if (!contourWork.step()) {
                    return false;
                }

                merged = contourWork.result();
                contourWork = null;
                core = merged.faces();
                vertices = LaserBeamContours.submittedVertices(core);
                if (core.size() > LaserBeamContours.MAX_FACES || vertices > LaserBeamContours.MAX_VERTICES) {
                    result = new ShellMesh(List.of(List.of(), List.of(), List.of()), 3, ShellReason.SUBMISSION_BUDGET);
                    return true;
                }

                fallback = List.of(List.of(), List.of(), core);
                phase = 1;
                return false;
            }

            if (phase == 1) {
                if (index < core.size()) {
                    Face face = core.get(index++);
                    DrawSection draw = face.draw();
                    if (renderability.computeIfAbsent(draw, section -> section.geometry().isRenderable(section.getReferenceWidth()))) {
                        candidates.add(face);
                    }
                    else {
                        partial = true;
                    }
                    return false;
                }

                if (partial || !merged.complete()) {
                    candidates.sort(Comparator.comparingDouble(LaserBeamContours::area).reversed());
                }

                phase = 2;
                return false;
            }

            if (phase == 2) {
                List<Face> contour = List.copyOf(candidates);
                pointIndex = new ContourPointIndex(context, contour);
                for (Face face : contour) {
                    pending.addLast(new PendingPatch(face, 0));
                }

                phase = 3;
                return false;
            }

            if (phase == 3) {
                if (boundaryWork != null) {
                    if (!boundaryWork.step()) {
                        return false;
                    }

                    BoundaryWork completed = boundaryWork;
                    boundaryWork = null;
                    patch(completed);
                    return false;
                }

                if (pending.isEmpty() && narrowing.isEmpty()) {
                    phase = 4;
                    index = 0;
                    return false;
                }

                if (vertices + MIN_PATCH_VERTICES > MAX_SUBMISSION_VERTICES || core.size() + outer.size() + inner.size() + MIN_PATCH_FACES > MAX_SUBMISSION_FACES || !budget.spend(1)) {
                    partial = true;
                    phase = 4;
                    index = 0;
                    return false;
                }

                if (budget.isExhausted()) {
                    patch(null);
                    return false;
                }

                PendingPatch current = pending.isEmpty() ? narrowing.getFirst() : pending.getFirst();
                if (pointIndex.boundaries.containsKey(current.face())) {
                    patch(null);
                    return false;
                }

                boundaryWork = new BoundaryWork(pointIndex, current.face(), budget);
                return false;
            }

            if (outer.isEmpty()) {
                result = new ShellMesh(fallback, 3, ShellReason.LOCAL_FALLBACK);
                return true;
            }

            if (index < outer.size()) {
                outerHalo.add(outer.get(index));
                innerHalo.add(inner.get(index));
                index++;
                return false;
            }

            int quality = 0;
            ShellReason reason = ShellReason.FULL;
            if (localized) {
                quality = 2;
                reason = ShellReason.LOCAL_NARROWED;
            }

            if (partial) {
                quality = 2;
                reason = ShellReason.PARTIAL;
            }

            result = new ShellMesh(List.of(List.copyOf(outerHalo), List.copyOf(innerHalo), core), quality, reason);
            return true;
        }

        @Override
        public ShellMesh result() {
            return Objects.requireNonNull(result, "Laser beam shell build has not completed.");
        }

        private void patch(@Nullable BoundaryWork preparedBoundary) {
            boolean narrow = pending.isEmpty();
            PendingPatch current;
            if (narrow) {
                current = narrowing.removeFirst();
            }
            else {
                current = pending.removeFirst();
            }

            Face face = current.face();
            DisplaySegment segment = context.segments().get(face.draw().segmentIndex());
            DisplayState beam = segment.start();
            BeamProfile profile = beam.profile();
            double radius = profile.radius();
            ToDoubleFunction<Vec3> thickness = point -> radius * OUTER_THICKNESS_RATIO;
            if (profile.widthSlope() != 0) {
                DisplayFrame frame = beam.frame();
                LaserBeamDisplayProfile display = LaserBeamDisplayProfile.fromBeam(beam);
                thickness = point -> {
                    Vec3 world = context.toWorldPosition(point);
                    double distance = Math.clamp(world.subtract(frame.origin()).dot(frame.direction()), 0, segment.remainingRange());
                    return display.radiusAt(distance) * OUTER_THICKNESS_RATIO;
                };
            }

            Vec3 direction = face.endCenter().subtract(face.startCenter()).normalize();
            List<Vec3> faceVertices = face.vertices();
            Vec3 anchor = faceVertices.getFirst();
            double minimum = 0;
            double maximum = 0;
            for (Vec3 point : faceVertices) {
                double distance = point.subtract(anchor).dot(direction);
                minimum = Math.min(minimum, distance);
                maximum = Math.max(maximum, distance);
            }

            int depth = current.depth();
            boolean canSplit = !narrow && depth < MAX_PATCH_DEPTH && maximum - minimum > sourceRadius * MIN_PATCH_WIDTH_RATIO;
            for (double scale : SCALES) {
                if (preparedBoundary == null && budget.isExhausted()) {
                    break;
                }

                if (scale < 1 != narrow) {
                    continue;
                }

                ShellPatch patch = null;
                if (preparedBoundary == null || preparedBoundary.hasResult()) {
                    patch = createPatch(face, pointIndex, offsets, thickness, scale, budget, previous, next);
                }
                preparedBoundary = null;
                if (patch == null) {
                    continue;
                }

                List<Face> outerPart = patch.outer();
                List<Face> innerPart = patch.inner();
                int addedVertices = LaserBeamContours.submittedVertices(outerPart) + LaserBeamContours.submittedVertices(innerPart);
                if (vertices + addedVertices > MAX_SUBMISSION_VERTICES || core.size() + outer.size() + inner.size() + outerPart.size() + innerPart.size() > MAX_SUBMISSION_FACES) {
                    partial = true;
                    return;
                }

                outer.addAll(outerPart);
                inner.addAll(innerPart);
                vertices += addedVertices;
                localized |= scale < 1;
                return;
            }

            localized = true;
            boolean exhausted = budget.isExhausted();
            if (!canSplit || exhausted) {
                if (!narrow && !exhausted) {
                    narrowing.addLast(current);
                }
                else {
                    partial = true;
                }

                return;
            }

            Vec3 middle = anchor.add(direction.scale((minimum + maximum) * 0.5));
            Face first = LaserBeamContours.planarPolygon(face, LaserBeamSurfaces.clip(faceVertices, middle, direction, true));
            Face second = LaserBeamContours.planarPolygon(face, LaserBeamSurfaces.clip(faceVertices, middle, direction, false));
            if (first == null || second == null) {
                partial = true;
                return;
            }

            pending.addLast(new PendingPatch(first, depth + 1));
            pending.addLast(new PendingPatch(second, depth + 1));
        }
    }

    private static @Nullable ShellPatch createPatch(Face face, ContourPointIndex pointIndex, Map<Vec3, @Nullable Vec3> offsets, ToDoubleFunction<Vec3> thickness, double scale, Budget budget) {
        return createPatch(face, pointIndex, offsets, thickness, scale, budget, null, null);
    }

    private static @Nullable ShellPatch createPatch(Face face, ContourPointIndex pointIndex, Map<Vec3, @Nullable Vec3> offsets, ToDoubleFunction<Vec3> thickness, double scale, Budget budget, @Nullable ShellReuse previous, @Nullable ShellReuse next) {
        List<Vec3> boundary = pointIndex.boundary(face, budget);
        if (boundary == null) {
            return null;
        }

        List<Vec3> outerBoundary = new ArrayList<>();
        List<Vec3> innerBoundary = new ArrayList<>();
        for (Vec3 point : boundary) {
            if (!offsets.containsKey(point)) {
                offsets.put(point, offset(pointIndex, point, thickness.applyAsDouble(point), budget, previous, next));
            }

            Vec3 displacement = offsets.get(point);
            if (displacement == null) {
                return null;
            }

            outerBoundary.add(point.add(displacement));
            innerBoundary.add(point.add(displacement.scale(INNER_THICKNESS_RATIO)));
        }

        List<Face> outer = new ArrayList<>();
        List<Face> inner = new ArrayList<>();
        List<Vec3> outerCenter = outerBoundary;
        List<Vec3> innerCenter = innerBoundary;
        if (scale < 1) {
            Vec3 center = Vec3.ZERO;
            for (Vec3 point : boundary) {
                center = center.add(point);
            }

            center = center.scale(1.0 / boundary.size());
            outerCenter = new ArrayList<>();
            innerCenter = new ArrayList<>();
            for (Vec3 point : boundary) {
                Vec3 base = point.lerp(center, TRANSITION_INSET);
                if (!offsets.containsKey(base)) {
                    offsets.put(base, offset(pointIndex, base, thickness.applyAsDouble(base), budget, previous, next));
                }

                Vec3 displacement = offsets.get(base);
                if (displacement == null) {
                    return null;
                }

                displacement = displacement.scale(scale);
                outerCenter.add(base.add(displacement));
                innerCenter.add(base.add(displacement.scale(INNER_THICKNESS_RATIO)));
            }

        }

        PatchKey key = null;
        if (previous != null || next != null) {
            key = new PatchKey(face, List.copyOf(outerBoundary), List.copyOf(innerBoundary), List.copyOf(outerCenter), List.copyOf(innerCenter), scale);
        }
        ShellPatch retained = previous == null ? null : previous.patches.get(key);
        if (retained != null && next != null) {
            next.patches.put(key, retained);
            return retained;
        }

        if (scale < 1) {
            List<List<Vec3>> rings = List.of(outerBoundary, innerBoundary);
            List<List<Vec3>> centers = List.of(outerCenter, innerCenter);
            List<List<Face>> layers = List.of(outer, inner);
            for (int index = 0; index < boundary.size(); index++) {
                int nextIndex = (index + 1) % boundary.size();
                for (int layer = 0; layer < layers.size(); layer++) {
                    List<Vec3> edge = rings.get(layer);
                    List<Vec3> middle = centers.get(layer);
                    Vec3 start = edge.get(index);
                    Vec3 nextMiddle = middle.get(nextIndex);
                    Face first = transition(face, List.of(start, edge.get(nextIndex), nextMiddle));
                    Face second = transition(face, List.of(start, nextMiddle, middle.get(index)));
                    if (first == null || second == null) {
                        return null;
                    }

                    List<Face> faces = layers.get(layer);
                    faces.add(first);
                    faces.add(second);
                }
            }
        }

        Face outerFace = LaserBeamContours.polygon(face, outerCenter);
        Face innerFace = LaserBeamContours.polygon(face, innerCenter);
        if (outerFace == null || innerFace == null) {
            return null;
        }

        outer.add(outerFace);
        inner.add(innerFace);
        ShellPatch patch = new ShellPatch(List.copyOf(outer), List.copyOf(inner));
        if (next != null) {
            next.patches.put(key, patch);
        }
        return patch;
    }

    private static @Nullable Face transition(Face original, List<Vec3> vertices) {
        Vec3 first = vertices.getFirst();
        Vec3 normal = vertices.get(1).subtract(first).cross(vertices.get(2).subtract(first));
        double length = normal.length();
        if (!Double.isFinite(length) || length <= EPSILON * EPSILON) {
            return null;
        }

        normal = normal.scale(1 / length);
        Face reference = new Face(vertices, normal, original.draw(), original.startCenter(), original.endCenter(), new AABB(first, first));
        return LaserBeamContours.polygon(reference, vertices);
    }

    private static @Nullable Vec3 offset(ContourPointIndex pointIndex, Vec3 point, double thickness, Budget budget, @Nullable ShellReuse previous, @Nullable ShellReuse next) {
        if (next == null) {
            return offset(pointIndex, point, thickness, budget);
        }

        List<OffsetFace> neighbors = new ArrayList<>();
        int count = pointIndex.queryOffset(point);
        for (int index = 0; index < count; index++) {
            Face face = pointIndex.faces.get(pointIndex.offsetCandidates[index]);
            neighbors.add(new OffsetFace(face, pointIndex.contacts.getOrDefault(face.draw().segmentIndex(), List.of())));
        }
        OffsetKey key = new OffsetKey(point, thickness, List.copyOf(neighbors));
        OffsetValue retained = previous == null ? null : previous.offsets.get(key);
        if (retained != null && budget.remaining() >= retained.operations()) {
            budget.spend(retained.operations());
            next.offsets.put(key, retained);
            return retained.displacement();
        }

        int remaining = budget.remaining();
        Vec3 displacement = offset(pointIndex, point, thickness, budget, count);
        if (!budget.isExhausted()) {
            next.offsets.put(key, new OffsetValue(displacement, remaining - budget.remaining()));
        }
        return displacement;
    }

    private static @Nullable Vec3 offset(ContourPointIndex pointIndex, Vec3 point, double thickness, Budget budget) {
        if (thickness <= EPSILON) {
            return Vec3.ZERO;
        }

        return offset(pointIndex, point, thickness, budget, pointIndex.queryOffset(point));
    }

    private static @Nullable Vec3 offset(ContourPointIndex pointIndex, Vec3 point, double thickness, Budget budget, int candidateCount) {
        if (thickness <= EPSILON) {
            return Vec3.ZERO;
        }

        List<Face> touching = new ArrayList<>();
        List<Vec3> constraints = new ArrayList<>();
        boolean physicalBoundary = false;
        for (int index = 0; index < candidateCount; index++) {
            int ordinal = pointIndex.offsetCandidates[index];
            Face face = pointIndex.faces.get(ordinal);
            Vec3 faceOrigin = face.vertices().getFirst();
            Vec3 faceNormal = face.normal();
            double planeDistance = (point.x - faceOrigin.x) * faceNormal.x + (point.y - faceOrigin.y) * faceNormal.y + (point.z - faceOrigin.z) * faceNormal.z;
            if (planeDistance < pointIndex.planeMinimum[ordinal] || planeDistance > pointIndex.planeMaximum[ordinal]) {
                continue;
            }

            boolean outsideProjection = false;
            for (FaceProjection projection : pointIndex.projections.get(ordinal)) {
                double distance = point.subtract(faceOrigin).dot(projection.axis());
                if (distance < projection.minimum() || distance > projection.maximum()) {
                    outsideProjection = true;
                    break;
                }
            }

            if (outsideProjection) {
                Vec3 projected = point.subtract(faceNormal.scale(planeDistance));
                List<Vec3> vertices = face.vertices();
                boolean inside = true;
                for (int edge = 0; edge < vertices.size(); edge++) {
                    Vec3 a = vertices.get(edge);
                    Vec3 b = vertices.get((edge + 1) % vertices.size());
                    Vec3 delta = b.subtract(a);
                    if (projected.subtract(a).dot(delta.cross(faceNormal)) > EPSILON * Math.sqrt(delta.lengthSqr())) {
                        inside = false;
                        break;
                    }
                }

                if (!inside) {
                    continue;
                }
            }

            if (!budget.spend(1)) {
                return null;
            }

            if (face.distanceTo(point) > EPSILON) {
                continue;
            }

            touching.add(face);
            for (OpticalPlane contact : pointIndex.contacts.getOrDefault(face.draw().segmentIndex(), List.of())) {
                if (Math.abs(point.subtract(contact.point()).dot(contact.normal())) <= EPSILON) {
                    physicalBoundary = true;
                    break;
                }
            }

            List<OpticalPlane> planes = pointIndex.endPlanes.computeIfAbsent(face.draw(), draw -> {

                Section geometry = draw.geometry();
                List<OpticalPlane> result = new ArrayList<>();
                for (List<Vec3> cap : List.of(geometry.starts(), geometry.ends())) {
                    Vec3 first = cap.getFirst();
                    Vec3 normal = cap.get(1).subtract(first).cross(cap.get(2).subtract(first));
                    double length = normal.length();
                    if (length <= EPSILON * EPSILON) {
                        continue;
                    }

                    result.add(new OpticalPlane(first, normal.scale(1 / length)));
                }

                return List.copyOf(result);
            });
            for (OpticalPlane plane : planes) {
                if (Math.abs(point.subtract(plane.point()).dot(plane.normal())) > EPSILON) {
                    continue;
                }

                constraints.add(plane.normal());
            }
        }

        List<Vec3> basis = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        for (Vec3 normal : constraints) {
            if (!equation(basis, values, normal, 0)) {
                return null;
            }
        }

        int constraintCount = basis.size();
        for (Face face : touching) {
            Vec3 normal = face.normal();
            if (physicalBoundary) {
                normal = constrainedNormal(normal, basis, constraintCount, true);
            }

            if (normal == null) {
                continue;
            }

            equation(basis, values, normal, thickness);
        }

        Vec3 displacement = Vec3.ZERO;
        for (int index = 0; index < basis.size(); index++) {
            displacement = displacement.add(basis.get(index).scale(values.get(index)));
        }

        if (!Double.isFinite(displacement.lengthSqr())) {
            return null;
        }

        double length = displacement.length();
        double limit = thickness * MAX_OFFSET_THICKNESS_RATIO;
        if (length <= limit) {
            return displacement;
        }

        double regularization = touching.size() / (MAX_OFFSET_THICKNESS_RATIO * MAX_OFFSET_THICKNESS_RATIO);
        double[][] system = new double[3][4];
        for (int axis = 0; axis < 3; axis++) {
            system[axis][axis] = regularization;
        }

        for (Face face : touching) {
            Vec3 normal = constrainedNormal(face.normal(), basis, constraintCount, physicalBoundary);
            if (normal == null) {
                continue;
            }

            double[] components = {normal.x, normal.y, normal.z};
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 3; column++) {
                    system[row][column] += components[row] * components[column];
                }

                system[row][3] += thickness * components[row];
            }
        }

        for (int pivot = 0; pivot < 3; pivot++) {
            double divisor = system[pivot][pivot];
            for (int column = pivot; column < 4; column++) {
                system[pivot][column] /= divisor;
            }

            for (int row = 0; row < 3; row++) {
                if (row == pivot) {
                    continue;
                }

                double factor = system[row][pivot];
                for (int column = pivot; column < 4; column++) {
                    system[row][column] -= factor * system[pivot][column];
                }
            }
        }

        Vec3 approximate = new Vec3(system[0][3], system[1][3], system[2][3]);
        if (!Double.isFinite(approximate.lengthSqr())) {
            return null;
        }

        double blend = Math.clamp(OFFSET_BLEND_RATE * (length - limit) / (length - approximate.length()), 0, 1);
        return displacement.lerp(approximate, blend);
    }

    private static @Nullable Vec3 constrainedNormal(Vec3 normal, List<Vec3> basis, int constraintCount, boolean normalize) {
        if (constraintCount == 0) {
            return normal;
        }

        for (int index = 0; index < constraintCount; index++) {
            Vec3 axis = basis.get(index);
            normal = normal.subtract(axis.scale(normal.dot(axis)));
        }

        if (!normalize) {
            return normal;
        }

        double length = normal.length();
        if (length <= EPSILON) {
            return null;
        }

        return normal.scale(1 / length);
    }

    private static boolean equation(List<Vec3> basis, List<Double> values, Vec3 normal, double distance) {
        for (int index = 0; index < basis.size(); index++) {
            Vec3 axis = basis.get(index);
            double projection = normal.dot(axis);
            normal = normal.subtract(axis.scale(projection));
            distance -= projection * values.get(index);
        }

        double length = normal.length();
        if (length <= EPSILON) {
            return Math.abs(distance) <= EPSILON;
        }

        basis.add(normal.scale(1 / length));
        values.add(distance / length);
        return true;
    }

    private static final class ContourPointIndex {
        private final List<Face> faces;
        private final Map<Face, List<Vec3>> boundaries = new IdentityHashMap<>();
        private final Map<DrawSection, List<OpticalPlane>> endPlanes = new IdentityHashMap<>();
        private final Map<Integer, List<OpticalPlane>> contacts = new LinkedHashMap<>();
        private final double[] planeMinimum;
        private final double[] planeMaximum;
        private final List<List<FaceProjection>> projections;
        private final int[] candidates;
        private final int[] offsetCandidates;
        private final IndexedFace[] indexed;
        private final int[] unindexed;
        private final @Nullable PointNode root;

        private ContourPointIndex(LaserBeamContext context, List<Face> faces) {
            this.faces = faces;
            int size = faces.size();
            planeMinimum = new double[size];
            planeMaximum = new double[size];
            projections = new ArrayList<>(size);
            for (int index = 0; index < size; index++) {
                Face face = faces.get(index);
                List<Vec3> vertices = face.vertices();
                Vec3 first = vertices.getFirst();
                Vec3 normal = face.normal();
                double padding = EPSILON * Math.max(1, normal.length());
                double minimum = 0;
                double maximum = 0;
                for (Vec3 vertex : face.vertices()) {
                    double distance = vertex.subtract(first).dot(normal);
                    minimum = Math.min(minimum, distance);
                    maximum = Math.max(maximum, distance);
                }

                planeMinimum[index] = minimum - padding;
                planeMaximum[index] = maximum + padding;
                Vec3 longest = Vec3.ZERO;
                for (int edge = 0; edge < vertices.size(); edge++) {
                    Vec3 delta = vertices.get((edge + 1) % vertices.size()).subtract(vertices.get(edge));
                    if (!(delta.lengthSqr() > longest.lengthSqr())) {
                        continue;
                    }

                    longest = delta;
                }

                Vec3 along = longest.scale(1 / longest.length());
                Vec3 across = normal.cross(along);
                List<FaceProjection> bounds = new ArrayList<>();
                for (Vec3 axis : List.of(along, across)) {
                    double lower = 0;
                    double upper = 0;
                    for (Vec3 vertex : vertices) {
                        double distance = vertex.subtract(first).dot(axis);
                        lower = Math.min(lower, distance);
                        upper = Math.max(upper, distance);
                    }

                    double margin = EPSILON * axis.length();
                    bounds.add(new FaceProjection(axis, lower - margin, upper + margin));
                }

                projections.add(List.copyOf(bounds));
            }

            List<DisplaySegment> segments = context.segments();
            for (int index = 0; index < segments.size(); index++) {
                List<OpticalPlane> boundaries = new ArrayList<>();
                for (int owner : new int[]{index, segments.get(index).parentIndex()}) {
                    if (owner < 0) {
                        continue;
                    }

                    OpticalPlane hit = segments.get(owner).contact();
                    if (hit == null) {
                        continue;
                    }

                    boundaries.add(new OpticalPlane(context.toLocalPosition(hit.point()), context.toLocalDirection(hit.normal())));
                }

                if (boundaries.isEmpty()) {
                    continue;
                }

                contacts.put(index, List.copyOf(boundaries));
            }

            candidates = new int[size];
            offsetCandidates = new int[size];
            for (int index = 0; index < size; index++) {
                candidates[index] = index;
            }

            if (size <= POINT_QUERY_LEAF_SIZE) {
                indexed = new IndexedFace[0];
                unindexed = new int[0];
                root = null;
                return;
            }

            List<IndexedFace> finite = new ArrayList<>(size);
            int[] other = new int[size];
            int otherCount = 0;
            for (int index = 0; index < size; index++) {
                AABB bounds = faces.get(index).bounds();
                if (!Double.isFinite(bounds.minX) || !Double.isFinite(bounds.minY) || !Double.isFinite(bounds.minZ) || !Double.isFinite(bounds.maxX) || !Double.isFinite(bounds.maxY) || !Double.isFinite(bounds.maxZ)) {
                    other[otherCount++] = index;
                    continue;
                }

                finite.add(new IndexedFace(index, bounds));
            }

            indexed = finite.toArray(IndexedFace[]::new);
            unindexed = Arrays.copyOf(other, otherCount);
            if (indexed.length == 0) {
                root = null;
                return;
            }

            root = new PointNode(indexed, 0, indexed.length);
        }

        private @Nullable List<Vec3> boundary(Face face, Budget budget) {
            List<Vec3> retained = boundaries.get(face);
            if (retained != null) {
                return retained;
            }

            BoundaryWork work = new BoundaryWork(this, face, budget);
            while (!work.step()) {}
            if (!work.hasResult()) {
                return null;
            }

            return work.result();
        }

        private int queryOffset(Vec3 point) {
            int count = query(point);
            int retained = 0;
            for (int index = 0; index < count; index++) {
                int ordinal = candidates[index];
                AABB bounds = faces.get(ordinal).bounds();
                if (point.x < bounds.minX - EPSILON || point.x > bounds.maxX + EPSILON || point.y < bounds.minY - EPSILON || point.y > bounds.maxY + EPSILON || point.z < bounds.minZ - EPSILON || point.z > bounds.maxZ + EPSILON) {
                    continue;
                }

                offsetCandidates[retained++] = ordinal;
            }

            return retained;
        }

        private int query(Vec3 point) {
            if (root == null) {
                return faces.size();
            }

            System.arraycopy(unindexed, 0, candidates, 0, unindexed.length);
            int count = root.collect(indexed, point, candidates, unindexed.length);
            Arrays.sort(candidates, 0, count);
            return count;
        }

        private int query(Vec3 start, Vec3 end) {
            if (root == null) {
                return faces.size();
            }

            System.arraycopy(unindexed, 0, candidates, 0, unindexed.length);
            int count = root.collect(indexed, start, end, candidates, unindexed.length);
            Arrays.sort(candidates, 0, count);
            return count;
        }
    }

    private static final class BoundaryWork implements LaserBeamWork<List<Vec3>> {
        private static final int POINTS_PER_STEP = 256;
        private final ContourPointIndex owner;
        private final Face face;
        private final Budget budget;
        private final List<Vec3> vertices;
        private final List<Vec3> boundary = new ArrayList<>();
        private final TreeMap<Double, Vec3> split = new TreeMap<>();
        private int edge;
        private int candidate;
        private int vertex;
        private int count;
        private boolean scanning;
        private boolean done;
        private Vec3 start;
        private Vec3 end;
        private Vec3 delta;
        private double lengthSqr;
        private @Nullable List<Vec3> result;

        private BoundaryWork(ContourPointIndex owner, Face face, Budget budget) {
            this.owner = owner;
            this.face = face;
            this.budget = budget;
            vertices = face.vertices();
            result = owner.boundaries.get(face);
            done = result != null;
        }

        @Override
        public boolean step() {
            if (done) {
                return true;
            }

            if (!scanning) {
                if (edge >= vertices.size()) {
                    result = List.copyOf(boundary);
                    owner.boundaries.put(face, result);
                    done = true;
                    return true;
                }

                start = vertices.get(edge);
                end = vertices.get((edge + 1) % vertices.size());
                delta = end.subtract(start);
                lengthSqr = delta.lengthSqr();
                split.clear();
                split.put(0.0, start);
                count = owner.query(start, end);
                candidate = 0;
                vertex = 0;
                scanning = true;
                return false;
            }

            for (int processed = 0; processed < POINTS_PER_STEP && candidate < count; processed++) {
                Face neighbor = owner.faces.get(owner.candidates[candidate]);
                if (neighbor == face || vertex >= neighbor.vertices().size()) {
                    candidate++;
                    vertex = 0;
                    continue;
                }

                Vec3 point = neighbor.vertices().get(vertex++);
                if (point.distanceToSqr(start) <= EPSILON * EPSILON || point.distanceToSqr(end) <= EPSILON * EPSILON) {
                    continue;
                }

                double fraction = point.subtract(start).dot(delta) / lengthSqr;
                if (fraction <= 0 || fraction >= 1 || start.add(delta.scale(fraction)).distanceToSqr(point) > EPSILON * EPSILON) {
                    continue;
                }

                if (!budget.spend(1)) {
                    done = true;
                    return true;
                }

                split.putIfAbsent(fraction, point);
            }

            if (candidate < count) {
                return false;
            }

            for (Vec3 point : split.values()) {
                if (!boundary.isEmpty() && boundary.getLast().distanceToSqr(point) <= EPSILON * EPSILON) {
                    continue;
                }

                boundary.add(point);
            }

            edge++;
            scanning = false;
            return false;
        }

        @Override
        public List<Vec3> result() {
            if (!done) {
                throw new IllegalStateException("Shell boundary work is not complete.");
            }

            return Objects.requireNonNull(result, "Shell boundary work produced no result.");
        }

        private boolean hasResult() {
            return result != null;
        }
    }

    private static final class PointNode {
        private final int first;
        private final int end;
        private final AABB bounds;
        private final List<PointNode> children;

        private PointNode(IndexedFace[] indexed, int first, int end) {
            this.first = first;
            this.end = end;
            AABB initial = indexed[first].bounds();
            double minX = initial.minX;
            double minY = initial.minY;
            double minZ = initial.minZ;
            double maxX = initial.maxX;
            double maxY = initial.maxY;
            double maxZ = initial.maxZ;
            for (int index = first + 1; index < end; index++) {
                AABB current = indexed[index].bounds();
                minX = Math.min(minX, current.minX);
                minY = Math.min(minY, current.minY);
                minZ = Math.min(minZ, current.minZ);
                maxX = Math.max(maxX, current.maxX);
                maxY = Math.max(maxY, current.maxY);
                maxZ = Math.max(maxZ, current.maxZ);
            }

            bounds = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
            if (end - first <= POINT_QUERY_LEAF_SIZE) {
                children = List.of();
                return;
            }

            double extent = maxX - minX;
            ToDoubleFunction<IndexedFace> center = face -> {
                AABB faceBounds = face.bounds();
                return (faceBounds.minX + faceBounds.maxX) * 0.5;
            };
            if (maxY - minY> extent) {
                extent = maxY - minY;
                center = face -> {
                    AABB faceBounds = face.bounds();
                    return (faceBounds.minY + faceBounds.maxY) * 0.5;
                };
            }

            if (maxZ - minZ> extent) {
                center = face -> {
                    AABB faceBounds = face.bounds();
                    return (faceBounds.minZ + faceBounds.maxZ) * 0.5;
                };
            }

            Arrays.sort(indexed, first, end, Comparator.comparingDouble(center));
            int middle = first + (end - first) / 2;
            children = List.of(new PointNode(indexed, first, middle), new PointNode(indexed, middle, end));
        }

        private int collect(IndexedFace[] indexed, Vec3 point, int[] candidates, int count) {
            if (point.x < bounds.minX - EPSILON || point.x > bounds.maxX + EPSILON || point.y < bounds.minY - EPSILON || point.y > bounds.maxY + EPSILON || point.z < bounds.minZ - EPSILON || point.z > bounds.maxZ + EPSILON) {
                return count;
            }

            if (children.isEmpty()) {
                for (int index = first; index < end; index++) {
                    candidates[count++] = indexed[index].ordinal();
                }

                return count;
            }

            for (PointNode child : children) {
                count = child.collect(indexed, point, candidates, count);
            }

            return count;
        }

        private int collect(IndexedFace[] indexed, Vec3 start, Vec3 end, int[] candidates, int count) {
            AABB expanded = bounds.inflate(EPSILON);
            if (!expanded.contains(start) && !expanded.contains(end) && expanded.clip(start, end).isEmpty()) {
                return count;
            }

            if (children.isEmpty()) {
                for (int index = first; index < this.end; index++) {
                    candidates[count++] = indexed[index].ordinal();
                }

                return count;
            }

            for (PointNode child : children) {
                count = child.collect(indexed, start, end, candidates, count);
            }

            return count;
        }
    }

    static final class ShellReuse {
        private final Map<OffsetKey, OffsetValue> offsets = new LinkedHashMap<>();
        private final Map<PatchKey, ShellPatch> patches = new LinkedHashMap<>();
    }

    private record OffsetFace(Face face, List<OpticalPlane> contacts) {}

    private record OffsetKey(Vec3 point, double thickness, List<OffsetFace> neighbors) {}

    private record OffsetValue(@Nullable Vec3 displacement, int operations) {}

    private record PatchKey(Face face, List<Vec3> outerBoundary, List<Vec3> innerBoundary, List<Vec3> outerCenter, List<Vec3> innerCenter, double scale) {}

    private record IndexedFace(int ordinal, AABB bounds) {}

    private record FaceProjection(Vec3 axis, double minimum, double maximum) {}

    private record PendingPatch(Face face, int depth) {}

    private record ShellPatch(List<Face> outer, List<Face> inner) {}

    public enum ShellReason {
        EMPTY,
        SUBMISSION_BUDGET,
        LOCAL_FALLBACK,
        FULL,
        LOCAL_NARROWED,
        PARTIAL
    }

    public record ShellMesh(List<List<Face>> layers, int quality, ShellReason reason) {
        public ShellMesh {
            layers = layers.stream().map(List::copyOf).toList();
        }
    }
}
