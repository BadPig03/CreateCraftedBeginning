package net.ty.createcraftedbeginning.content.opticalpower.laser.display;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Segment;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.OpticalPlane;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class LaserBeamGeometry {
    public static final double TERMINAL_FADE_LENGTH = 1;

    private static final double PARALLEL_TOLERANCE = 1.0E-8;
    private static final double GRAZING_COSINE = 0.05;
    private static final double MAX_JOINT_EXTENSION = 0.25;
    private static final double SEGMENT_JOINT_FRACTION = 0.4;
    private static final double EMISSION_OFFSET = 9.765625E-4;
    private static final double RANGE_END_TOLERANCE = 1.0E-7;

    private LaserBeamGeometry() {
    }

    public static List<Section> createSections(List<Segment> segments, Vec3[] initialOffsets, boolean emitting, double referenceWidth, Function<Segment, @Nullable OpticalPlane> findPlane) {
        if (segments.isEmpty()) {
            return List.of();
        }

        Segment first = segments.getFirst();
        BeamFrame firstFrame = first.start().frame();
        Vec3 firstDirection = firstFrame.direction();
        Vec3 emissionStart = firstFrame.origin();
        if (emitting) {
            emissionStart = emissionStart.add(firstDirection.scale(Math.min(EMISSION_OFFSET, first.length() * 0.25)));
        }

        Vec3[] offsets = initialOffsets.clone();
        List<Vec3> starts = new ArrayList<>();
        for (Vec3 offset : offsets) {
            starts.add(emissionStart.add(offset));
        }
        starts = List.copyOf(starts);
        List<Section> sections = new ArrayList<>();
        int segmentCount = segments.size();
        for (int index = 0; index < segmentCount; index++) {
            Segment segment = segments.get(index);
            BeamFrame frame = segment.start().frame();
            Vec3 direction = frame.direction();
            double length = segment.length();
            OpticalPlane plane = findPlane.apply(segment);
            Vec3 center = frame.origin().add(direction.scale(length));
            Vec3[] jointOffsets = offsets.clone();
            double grazingScale;
            Vec3 normal = Vec3.ZERO;
            if (plane != null) {
                center = plane.point();
                normal = plane.normal();
                double denominator = direction.dot(normal);
                double incidence = Math.abs(denominator);
                double grazing = Mth.clamp(incidence / GRAZING_COSINE, 0, 1);
                grazingScale = grazing * grazing * (3 - 2 * grazing);
                double extensionLimit = Math.min(Math.min(MAX_JOINT_EXTENSION, 4 * referenceWidth), length * SEGMENT_JOINT_FRACTION);
                extensionLimit = Math.min(extensionLimit, Math.max(0, segment.remainingRange() - length));
                Vec3 outgoing = direction;
                if (index + 1 < segmentCount) {
                    Segment next = segments.get(index + 1);
                    extensionLimit = Math.min(extensionLimit, next.length() * SEGMENT_JOINT_FRACTION);
                    outgoing = next.start().frame().direction();
                }

                double scale = 1;
                for (int corner = 0; corner < offsets.length; corner++) {
                    Vec3 offset = offsets[corner];
                    Vec3 cut = Vec3.ZERO;
                    if (incidence > PARALLEL_TOLERANCE) {
                        cut = offset.subtract(direction.scale(offset.dot(normal) / denominator));
                    }

                    jointOffsets[corner] = cut;
                    double extension = Math.max(Math.abs(cut.dot(direction)), Math.abs(cut.dot(outgoing)));
                    if (!(extension > extensionLimit)) {
                        continue;
                    }

                    scale = Math.min(scale, extensionLimit / extension);
                }
                scale = Math.clamp(scale, 0, 1) * grazingScale;
                for (int corner = 0; corner < offsets.length; corner++) {
                    jointOffsets[corner] = jointOffsets[corner].scale(scale);
                }
            }

            List<Vec3> ends = new ArrayList<>();
            for (Vec3 offset : jointOffsets) {
                ends.add(center.add(offset));
            }
            List<Vec3> joint = List.copyOf(ends);
            boolean fadesAtEnd = index == segmentCount - 1 && plane == null && segment.hit() == null && Math.abs(length - segment.remainingRange()) <= RANGE_END_TOLERANCE;
            sections.add(new Section(starts, joint, fadesAtEnd));
            starts = joint;
            if (plane == null || index + 1 == segmentCount) {
                continue;
            }

            for (int corner = 0; corner < offsets.length; corner++) {
                Vec3 jointOffset = jointOffsets[corner];
                Vec3 offset = jointOffset.subtract(direction.scale(jointOffset.dot(direction)));
                offsets[corner] = offset.subtract(normal.scale(2 * offset.dot(normal)));
            }
        }
        return List.copyOf(sections);
    }

    public record Section(List<Vec3> starts, List<Vec3> ends, boolean fadesAtEnd, boolean variableWidth) {
        private static final double MIN_EPSILON = 1.0E-9;
        private static final double MAX_EPSILON = 1.0E-7;
        private static final double RELATIVE_EPSILON = 1.0E-5;
        private static final double EDGE_DIVISOR = 128;
        private static final double EDGE_EPSILONS = 32;
        private static final double CONTACT_FRACTION = 0.45;
        private static final double CONTACT_CLEARANCE = 0.05;
        private static final double TAPER_WIDTH_RATIO = 16;
        private static final int MAX_TAPER_PARTS = 4;

        public Section(List<Vec3> starts, List<Vec3> ends, boolean fadesAtEnd) {
            this(starts, ends, fadesAtEnd, false);
        }

        public Section {
            if (variableWidth && starts.size() == ends.size()) {
                List<Vec3> compactStarts = new ArrayList<>(starts);
                List<Vec3> compactEnds = new ArrayList<>(ends);
                for (int index = compactStarts.size() - 1; index >= 0 && compactStarts.size() > 3; index--) {
                    int next = (index + 1) % compactStarts.size();
                    if (compactStarts.get(index).distanceTo(compactStarts.get(next)) > MAX_EPSILON || compactEnds.get(index).distanceTo(compactEnds.get(next)) > MAX_EPSILON) {
                        continue;
                    }

                    compactStarts.remove(index);
                    compactEnds.remove(index);
                }
                starts = compactStarts;
                ends = compactEnds;
            }
            starts = List.copyOf(starts);
            ends = List.copyOf(ends);
        }

        public static Vec3 center(List<Vec3> vertices) {
            Vec3 center = Vec3.ZERO;
            for (Vec3 vertex : vertices) {
                center = center.add(vertex);
            }
            return center.scale(1.0 / vertices.size());
        }

        private static double minimumEdge(List<Vec3> vertices) {
            double width = Double.POSITIVE_INFINITY;
            for (int corner = 0; corner < vertices.size(); corner++) {
                width = Math.min(width, vertices.get(corner).distanceTo(vertices.get((corner + 1) % vertices.size())));
            }
            return width;
        }

        private static boolean validBoundary(List<Vec3> vertices, double edge, double epsilon) {
            if (vertices.size() < 3 || !Double.isFinite(edge)) {
                return false;
            }

            for (Vec3 vertex : vertices) {
                if (Double.isFinite(vertex.x) && Double.isFinite(vertex.y) && Double.isFinite(vertex.z)) {
                    continue;
                }

                return false;
            }
            Vec3 first = vertices.getFirst();
            Vec3 cross = vertices.get(1).subtract(first).cross(vertices.getLast().subtract(first));
            double area = cross.length();
            if (area < edge * edge || minimumEdge(vertices) < edge) {
                return false;
            }

            Vec3 normal = cross.scale(1 / area);
            for (int corner = 0; corner < vertices.size(); corner++) {
                Vec3 vertex = vertices.get(corner);
                Vec3 next = vertices.get((corner + 1) % vertices.size());
                Vec3 previous = vertices.get((corner + vertices.size() - 1) % vertices.size());
                if (Math.abs(vertex.subtract(first).dot(normal)) > epsilon || next.subtract(vertex).cross(previous.subtract(vertex)).dot(normal) < edge * edge) {
                    return false;
                }
            }
            return true;
        }

        private static boolean isPoint(List<Vec3> vertices, double epsilon) {
            Vec3 first = vertices.getFirst();
            for (Vec3 vertex : vertices) {
                if (vertex.distanceToSqr(first) > epsilon * epsilon) {
                    return false;
                }
            }
            return true;
        }

        private static boolean containsVolume(List<Vec3> face, List<Vec3> vertices, Vec3 center, double epsilon) {
            Vec3 first = face.getFirst();
            Vec3 normal = face.get(1).subtract(first).cross(face.get(2).subtract(first));
            double length = normal.length();
            if (!Double.isFinite(length) || length <= epsilon * epsilon) {
                return false;
            }

            normal = normal.scale(1 / length);
            if (normal.dot(center.subtract(first)) > 0) {
                normal = normal.scale(-1);
            }
            for (Vec3 vertex : vertices) {
                if (normal.dot(vertex.subtract(first)) <= epsilon) {
                    continue;
                }

                return false;
            }
            return true;
        }

        public List<Section> createParts(boolean grazingTerminal) {
            if (starts.size() != 4 || ends.size() != 4) {
                return List.of(this);
            }

            Vec3 firstStart = starts.getFirst();
            Vec3 firstEnd = ends.getFirst();
            Vec3 startCenter = firstStart.lerp(starts.get(2), 0.5);
            Vec3 endCenter = firstEnd.lerp(ends.get(2), 0.5);
            Vec3 direction = endCenter.subtract(startCenter);
            double length = direction.length();
            if (!Double.isFinite(length) || length <= MAX_EPSILON) {
                return List.of(this);
            }

            direction = direction.scale(1 / length);
            Vec3 startNormal = starts.get(1).subtract(firstStart).cross(starts.get(3).subtract(firstStart));
            Vec3 endNormal = ends.get(1).subtract(firstEnd).cross(ends.get(3).subtract(firstEnd));
            for (Vec3 candidate : List.of(startNormal, endNormal)) {
                double magnitude = candidate.length();
                if (!Double.isFinite(magnitude) || magnitude <= MAX_EPSILON * MAX_EPSILON) {
                    continue;
                }

                Vec3 axis = candidate.scale(1 / magnitude);
                double alignment = direction.dot(axis);
                if (Math.abs(alignment) < 1 - MAX_EPSILON) {
                    continue;
                }

                direction = axis.scale(Math.signum(alignment));
                break;
            }

            List<Vec3> startOffsets = new ArrayList<>();
            List<Vec3> endOffsets = new ArrayList<>();
            double skew = 0;
            for (int corner = 0; corner < 4; corner++) {
                Vec3 start = starts.get(corner).subtract(startCenter);
                Vec3 end = ends.get(corner).subtract(endCenter);
                skew = Math.max(skew, Math.max(Math.abs(start.dot(direction)), Math.abs(end.dot(direction))));
                startOffsets.add(start.subtract(direction.scale(start.dot(direction))));
                endOffsets.add(end.subtract(direction.scale(end.dot(direction))));
            }
            double startWidth = minimumEdge(startOffsets);
            double endWidth = minimumEdge(endOffsets);
            if (skew <= MAX_EPSILON || !Double.isFinite(startWidth) || !Double.isFinite(endWidth) || startWidth <= MAX_EPSILON || endWidth <= MAX_EPSILON && (!grazingTerminal || endWidth <= 0) || Math.abs(startWidth - endWidth) <= MAX_EPSILON) {
                return List.of(this);
            }

            double ratio = endWidth / startWidth;
            for (int corner = 0; corner < 4; corner++) {
                if (startOffsets.get(corner).scale(ratio).distanceTo(endOffsets.get(corner)) <= MAX_EPSILON) {
                    continue;
                }

                return List.of(this);
            }
            List<Vec3> offsets = startOffsets;
            boolean expanding = endWidth > startWidth;
            if (expanding) {
                offsets = endOffsets;
            }
            double wideWidth = Math.max(startWidth, endWidth);
            double clearance = Math.min(length * CONTACT_CLEARANCE, 4 * wideWidth);
            double contact = Math.min(length * CONTACT_FRACTION, Math.max(skew + clearance, Math.min(MAX_JOINT_EXTENSION, 4 * wideWidth)));
            Vec3 contactNormal = expanding ? startNormal : endNormal;
            double incidence = Math.abs(direction.dot(contactNormal));
            if (incidence > MAX_EPSILON * contactNormal.length()) {
                double required = 0;
                for (Vec3 offset : offsets) {
                    required = Math.max(required, Math.abs(offset.dot(contactNormal)) / incidence);
                }
                required += clearance;
                if (Double.isFinite(required) && required <= length * CONTACT_FRACTION) {
                    contact = Math.max(contact, required);
                }
            }
            double axialLength = endCenter.subtract(startCenter).dot(direction);
            Vec3 center = startCenter.add(direction.scale(axialLength - contact));
            if (expanding) {
                center = endCenter.subtract(direction.scale(axialLength - contact));
            }
            List<Vec3> middle = new ArrayList<>();
            for (Vec3 offset : offsets) {
                middle.add(center.add(offset));
            }
            double narrowWidth = Math.min(startWidth, endWidth);
            List<Vec3> narrow = expanding ? starts : ends;
            List<List<Vec3>> boundaries = new ArrayList<>();
            boundaries.add(middle);
            double width = wideWidth;
            for (int part = 1; part < MAX_TAPER_PARTS && width > narrowWidth * TAPER_WIDTH_RATIO; part++) {
                width /= TAPER_WIDTH_RATIO;
                double fraction = (wideWidth - width) / (wideWidth - narrowWidth);
                List<Vec3> boundary = new ArrayList<>();
                for (int corner = 0; corner < 4; corner++) {
                    boundary.add(middle.get(corner).lerp(narrow.get(corner), fraction));
                }
                boundaries.add(List.copyOf(boundary));
            }
            boundaries.add(narrow);
            List<Section> parts = new ArrayList<>();
            if (expanding) {
                for (int index = boundaries.size() - 1; index > 0; index--) {
                    parts.add(new Section(boundaries.get(index), boundaries.get(index - 1), false, variableWidth));
                }
                parts.add(new Section(middle, ends, fadesAtEnd, variableWidth));
                return List.copyOf(parts);
            }

            parts.add(new Section(starts, middle, false, variableWidth));
            for (int index = 1; index < boundaries.size(); index++) {
                parts.add(new Section(boundaries.get(index - 1), boundaries.get(index), index == boundaries.size() - 1 && fadesAtEnd, variableWidth));
            }
            return List.copyOf(parts);
        }

        public boolean isRenderable(double referenceWidth) {
            return !createSides(referenceWidth).isEmpty();
        }

        public List<List<Vec3>> createSides(double referenceWidth) {
            if (starts.size() < 3 || ends.size() != starts.size()) {
                return List.of();
            }

            Vec3 startCenter = center(starts);
            Vec3 endCenter = center(ends);
            Vec3 direction = endCenter.subtract(startCenter);
            double length = direction.length();
            double startEdge = minimumEdge(starts);
            double endEdge = minimumEdge(ends);
            double localWidth = Math.max(startEdge, endEdge);
            double width = Math.max(referenceWidth, localWidth);
            double epsilon = Math.clamp(RELATIVE_EPSILON * Math.min(width, length), MIN_EPSILON, MAX_EPSILON);
            double edgeWidth = variableWidth ? Math.min(startEdge, endEdge) : localWidth;
            double edge = Math.max(EDGE_EPSILONS * epsilon, Math.min(edgeWidth, length) / EDGE_DIVISOR);
            boolean startPoint = variableWidth && isPoint(starts, epsilon);
            boolean endPoint = variableWidth && isPoint(ends, epsilon);
            if (!Double.isFinite(length) || length <= epsilon || startPoint && endPoint || !startPoint && !validBoundary(starts, 2 * edge, epsilon) || !endPoint && !validBoundary(ends, 2 * edge, epsilon)) {
                return List.of();
            }

            direction = direction.scale(1 / length);
            List<Vec3> vertices = new ArrayList<>(starts);
            vertices.addAll(ends);
            Vec3 center = startCenter.lerp(endCenter, 0.5);
            if (!startPoint && !containsVolume(starts, vertices, center, epsilon) || !endPoint && !containsVolume(ends, vertices, center, epsilon)) {
                return List.of();
            }

            List<List<Vec3>> sides = new ArrayList<>();
            for (int corner = 0; corner < starts.size(); corner++) {
                int next = (corner + 1) % starts.size();
                Vec3 start = starts.get(corner);
                Vec3 end = ends.get(corner);
                double advance = end.subtract(start).dot(direction);
                if (!startPoint && !endPoint && (variableWidth ? advance < -epsilon : advance <= epsilon)) {
                    return List.of();
                }

                List<Vec3> side = List.of(start, end, ends.get(next), starts.get(next));
                if (startPoint) {
                    side = List.of(start, end, ends.get(next));
                }
                else if (endPoint) {
                    side = List.of(start, end, starts.get(next));
                }
                else if (variableWidth) {
                    List<Vec3> compact = new ArrayList<>();
                    for (Vec3 point : side) {
                        if (compact.isEmpty() || compact.getLast().distanceTo(point) > epsilon) {
                            compact.add(point);
                        }
                    }
                    if (compact.size() > 1 && compact.getFirst().distanceTo(compact.getLast()) <= epsilon) {
                        compact.removeLast();
                    }
                    if (compact.size() < 3) {
                        continue;
                    }

                    side = List.copyOf(compact);
                }
                if (validBoundary(side, edge, epsilon) && containsVolume(side, vertices, center, epsilon)) {
                    sides.add(side);
                    continue;
                }

                if (side.size() == 3) {
                    return List.of();
                }

                boolean found = false;
                for (int diagonal = 0; diagonal < 2; diagonal++) {
                    Vec3 first = side.get(diagonal);
                    Vec3 second = side.get((diagonal + 1) % 4);
                    Vec3 third = side.get((diagonal + 2) % 4);
                    Vec3 fourth = side.get((diagonal + 3) % 4);
                    List<Vec3> a = List.of(first, second, third);
                    List<Vec3> b = List.of(first, third, fourth);
                    if (second.subtract(first).cross(third.subtract(first)).length() < 2 * edge * edge || third.subtract(first).cross(fourth.subtract(first)).length() < 2 * edge * edge || !containsVolume(a, vertices, center, epsilon) || !containsVolume(b, vertices, center, epsilon)) {
                        continue;
                    }

                    sides.add(a);
                    sides.add(b);
                    found = true;
                    break;
                }
                if (found) {
                    continue;
                }

                return List.of();
            }
            return List.copyOf(sides);
        }

        public List<Vec3> getStartCap() {
            double width = minimumEdge(starts);
            double epsilon = Math.clamp(RELATIVE_EPSILON * width, MIN_EPSILON, MAX_EPSILON);
            double edge = Math.max(EDGE_EPSILONS * epsilon, width / EDGE_DIVISOR);
            if (!validBoundary(starts, 2 * edge, epsilon)) {
                return List.of();
            }

            return starts;
        }
    }
}
