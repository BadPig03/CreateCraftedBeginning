package net.ty.createcraftedbeginning.content.opticalpower.laser.display;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion.Point;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamContours.Budget;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayInput.Base;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayInput.MappedFamily;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayInput.Mapping;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayInput.MappingState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayInput.Tile;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamGeometry.Section;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamMesh.DrawSection;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamMesh.MappedData;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamMesh.SamplingDomain;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamShells.ShellMesh;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamShells.ShellReason;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamShells.ShellReuse;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamSurfaces.Face;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamSurfaces.SurfaceBuild;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamSurfaces.SurfaceReason;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.OpticalPlane;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class LaserBeamDisplayMesh {
    private static final double MIN_FACE_NORMAL_LENGTH = 1.0E-14;

    private LaserBeamDisplayMesh() {}

    public static Result create(LaserBeamDisplayInput input) {
        Base base = input.base();
        ShellMesh baseline = baseline(input.context(), base);
        Result rejection = reject(base, input.mapping(), baseline, null);
        if (rejection != null) {
            return rejection;
        }

        return createMapped(input, baseline);
    }

    public static Result createUnmapped(LaserBeamContext context, List<DrawSection> sections, Budget budget, String mappingReason) {
        SurfaceBuild surfaces = LaserBeamSurfaces.build(sections, budget);
        ShellMesh shells = LaserBeamShells.build(context, surfaces.faces(), budget);
        if (surfaces.isComplete()) {
            return new Result(sections, shells, Status.UNSUPPORTED, mappingReason);
        }

        if (shells.reason() == ShellReason.EMPTY) {
            shells = new ShellMesh(shells.layers(), shells.quality(), ShellReason.LOCAL_FALLBACK);
        }
        return new Result(sections, shells, Status.UNSUPPORTED, describeSurfaceFailure(surfaces.reason(), shells.layers().getLast().size()));
    }

    public static LaserBeamWork<Result> begin(LaserBeamDisplayInput input) {
        return begin(input, null);
    }

    public static LaserBeamWork<Result> begin(LaserBeamDisplayInput input, @Nullable Cache previous) {
        return new Build(input, null, previous, new Cache());
    }

    static Result createMapped(LaserBeamDisplayInput input, ShellMesh baseline) {
        Base base = input.base();
        try {
            Mapping mapping = input.mapping();
            Set<Integer> selected = mapping.selected();
            List<DrawSection> sections = new ArrayList<>(base.sections().stream().filter(draw -> !selected.contains(draw.segmentIndex())).toList());
            List<Face> core = new ArrayList<>(base.faces().stream().filter(face -> !selected.contains(face.draw().segmentIndex())).toList());
            List<Face> generated = new ArrayList<>();
            LaserBeamContext context = input.context();
            for (MappedFamily family : mapping.families()) {
                for (Tile tile : family.tiles()) {
                    addTile(context, family, tile, family.representative(), family.pathDistance(), family.appearance(), sections, generated);
                }
            }

            Budget budget = new Budget();
            resolve(mapping, core, generated, budget);
            ShellMesh shells = LaserBeamShells.build(context, core, budget);
            requireComplete(shells);
            return new Result(sections, shells, Status.APPLIED, "");
        }
        catch (UnsupportedOperationException unsupported) {
            return new Result(base.sections(), baseline, Status.UNSUPPORTED, unsupported.getMessage());
        }
    }

    static ShellMesh baseline(LaserBeamContext context, Base base) {
        if (!base.isComplete()) {
            return fallback(base);
        }

        return LaserBeamShells.build(context, base.faces(), new Budget(base.remainingBudget()));
    }

    static LaserBeamWork<ShellMesh> beginBaseline(LaserBeamContext context, Base base, @Nullable Cache previous, Cache cache) {
        List<Face> faces = base.faces();
        Budget budget = new Budget(base.remainingBudget());
        if (previous == null) {
            return LaserBeamShells.begin(context, faces, budget, null, cache.shells);
        }

        return LaserBeamShells.begin(context, faces, budget, previous.shells, cache.shells);
    }

    static ShellMesh fallback(Base base) {
        return new ShellMesh(List.of(List.of(), List.of(), base.faces()), 3, ShellReason.LOCAL_FALLBACK);
    }

    static @Nullable Result reject(Base base, Mapping mapping, ShellMesh baseline, @Nullable Cache cache) {
        if (!base.isComplete()) {
            return new Result(base.sections(), baseline, Status.UNSUPPORTED, describeSurfaceFailure(base.reason(), base.faces().size()), cache);
        }

        if (!mapping.selected().isEmpty() && (baseline.reason() != ShellReason.FULL || baseline.quality() != 0)) {
            boolean covered = baseline.reason() == ShellReason.PARTIAL && base.remainingBudget() > 0 && !base.invalidSegments().isEmpty() && mapping.selected().containsAll(base.invalidSegments());
            boolean preserved = base.faces().stream().filter(face -> !mapping.selected().contains(face.draw().segmentIndex())).allMatch(face -> face.draw().renderSupport().isRenderable(face.draw().getReferenceWidth()));
            if (!covered || !preserved) {
                return new Result(base.sections(), baseline, Status.UNSUPPORTED, "An incomplete production shell has an uncovered or unclassified branch.", cache);
            }
        }

        if (mapping.state() == MappingState.NO_FAMILY) {
            return new Result(base.sections(), baseline, Status.NO_FAMILY, mapping.reason(), cache);
        }

        if (mapping.state() == MappingState.UNSUPPORTED) {
            return new Result(base.sections(), baseline, Status.UNSUPPORTED, mapping.reason(), cache);
        }

        return null;
    }

    static LaserBeamWork<Result> beginMapped(LaserBeamDisplayInput input, ShellMesh baseline, @Nullable Cache previous, Cache cache) {
        return new Build(input, baseline, previous, cache);
    }

    private static void resolve(Mapping mapping, List<Face> core, List<Face> generated, Budget budget) {
        if (mapping.families().stream().anyMatch(MappedFamily::polygonal)) {
            List<Face> composition = new ArrayList<>(core);
            composition.addAll(generated);
            SurfaceBuild resolved = LaserBeamSurfaces.resolve(composition, budget);
            if (!resolved.isComplete()) {
                throw new UnsupportedOperationException("Mapped surface generation is incomplete: " + resolved.reason() + '.');
            }

            generated = resolved.faces().stream().filter(face -> face.draw().mapped() != null).toList();
        }

        if (generated.isEmpty()) {
            throw new UnsupportedOperationException("Mapped display contains no resolved exterior.");
        }

        core.addAll(generated);
    }

    private static void requireComplete(ShellMesh shells) {
        if (shells.reason() != ShellReason.FULL || shells.quality() != 0 || shells.layers().stream().anyMatch(List::isEmpty)) {
            throw new UnsupportedOperationException("Mapped display cannot produce complete production shells.");
        }
    }

    private static String describeSurfaceFailure(SurfaceReason reason, int retainedCaps) {
        return "Original surface generation is incomplete: " + reason + "; retained current source caps=" + retainedCaps + '.';
    }

    private static final class Build implements LaserBeamWork<Result> {
        private final LaserBeamDisplayInput input;
        private final @Nullable Cache previous;
        private final Cache cache;
        private final Budget mappedBudget = new Budget();
        private final List<DrawSection> sections = new ArrayList<>();
        private final List<Face> core = new ArrayList<>();
        private final List<Face> generated = new ArrayList<>();
        private @Nullable LaserBeamWork<ShellMesh> shellWork;
        private ShellMesh baseline;
        private Result result;
        private int stage;
        private int familyIndex;
        private int tileIndex;

        private Build(LaserBeamDisplayInput input, @Nullable ShellMesh baseline, @Nullable Cache previous, Cache cache) {
            this.input = input;
            cache.input = input;
            this.previous = previous;
            this.cache = cache;
            this.baseline = baseline;
            if (baseline == null) {
                return;
            }

            stage = 2;
        }

        @Override
        public boolean step() {
            if (result != null) {
                return true;
            }

            try {
                return advance();
            }
            catch (UnsupportedOperationException unsupported) {
                if (baseline == null) {
                    throw unsupported;
                }

                result = new Result(input.base().sections(), baseline, Status.UNSUPPORTED, unsupported.getMessage(), cache);
                return true;
            }
        }

        @Override
        public Result result() {
            return Objects.requireNonNull(result, "Laser beam display mesh build has not completed.");
        }

        private boolean advance() {
            Base base = input.base();
            Mapping mapping = input.mapping();
            if (stage == 0) {
                if (!base.isComplete()) {
                    baseline = fallback(base);
                    result = Objects.requireNonNull(reject(base, mapping, baseline, cache), "An incomplete source surface must produce a fallback laser beam display result.");
                    return true;
                }

                shellWork = beginBaseline(input.context(), base, previous, cache);
                stage = 1;
                return false;
            }

            if (stage == 1) {
                LaserBeamWork<ShellMesh> work = Objects.requireNonNull(shellWork, "Baseline laser beam shell build must be initialized before it can advance.");
                if (!work.step()) {
                    return false;
                }

                baseline = work.result();
                shellWork = null;
                stage = 2;
                return false;
            }

            if (stage == 2) {
                result = reject(base, mapping, baseline, cache);
                if (result != null) {
                    return true;
                }

                Set<Integer> selected = mapping.selected();
                sections.addAll(base.sections().stream().filter(draw -> !selected.contains(draw.segmentIndex())).toList());
                core.addAll(base.faces().stream().filter(face -> !selected.contains(face.draw().segmentIndex())).toList());
                stage = 3;
                return false;
            }

            if (stage == 3) {
                List<MappedFamily> families = mapping.families();
                if (familyIndex < families.size()) {
                    MappedFamily family = families.get(familyIndex);
                    List<Tile> tiles = family.tiles();
                    if (tileIndex < tiles.size()) {
                        addTile(input.context(), family, tiles.get(tileIndex++), family.representative(), family.pathDistance(), family.appearance(), sections, generated);
                        return false;
                    }

                    familyIndex++;
                    tileIndex = 0;
                    return false;
                }

                stage = 4;
                return false;
            }

            if (stage == 4) {
                resolve(mapping, core, generated, mappedBudget);
                shellWork = LaserBeamShells.begin(input.context(), core, mappedBudget, previous == null ? null : previous.shells, cache.shells);
                stage = 5;
                return false;
            }

            LaserBeamWork<ShellMesh> work = Objects.requireNonNull(shellWork, "Mapped laser beam shell build must be initialized before it can advance.");
            if (!work.step()) {
                return false;
            }

            ShellMesh shells = work.result();
            shellWork = null;
            requireComplete(shells);
            result = new Result(sections, shells, Status.APPLIED, "", cache);
            return true;
        }
    }

    private static void addTile(LaserBeamContext context, MappedFamily family, Tile tile, int representative, double pathDistance, LaserBeamAppearance appearance, List<DrawSection> sections, List<Face> generated) {
        Section logical = local(context, tile.geometry());
        Section support = local(context, tile.support());
        List<Point> polygon = tile.polygon();
        BeamRegion region = new BeamRegion(polygon);
        double minU = polygon.stream().mapToDouble(Point::u).min().orElseThrow();
        double maxU = polygon.stream().mapToDouble(Point::u).max().orElseThrow();
        double minV = polygon.stream().mapToDouble(Point::v).min().orElseThrow();
        double maxV = polygon.stream().mapToDouble(Point::v).max().orElseThrow();
        SamplingDomain domain = new SamplingDomain((minU + 1) / 2, (minV + 1) / 2, (maxU + 1) / 2, (maxV + 1) / 2, region);
        LaserBeamBrightness field = local(context, tile.field());
        LaserBeamBrightness halo = local(context, tile.haloField());
        List<OpticalPlane> endPlanes = tile.endPlanes();
        List<OpticalPlane> boundaries = endPlanes == null ? null : endPlanes.stream().map(plane -> new OpticalPlane(context.toLocalPosition(plane.point()), context.toLocalDirection(plane.normal()))).toList();
        MappedData data = new MappedData(support, field, halo, boundaries);
        double along = pathDistance + tile.pathOffset();
        DrawSection draw = new DrawSection(logical, appearance, representative, along, null, domain, false, field, data);
        sections.add(draw);
        Vec3 startCenter = context.toLocalPosition(tile.startCenter());
        Vec3 endCenter = context.toLocalPosition(tile.endCenter());
        List<Vec3> starts = logical.starts();
        List<Vec3> ends = logical.ends();
        int edgeCount = starts.size();
        List<Boolean> exposedEdges = tile.exposedEdges();
        List<Vec3> outwardDirections = tile.outward();
        boolean polygonal = family.polygonal();
        Vec3 interior = null;
        List<Vec3> supportVertices = List.of();
        if (polygonal) {
            interior = context.toLocalPosition(Objects.requireNonNull(tile.interior(), "Polygonal tile must have an interior point."));
            supportVertices = Stream.concat(starts.stream(), ends.stream()).toList();
        }
        for (int edge = 0; edge < edgeCount; edge++) {
            if (!exposedEdges.get(edge)) {
                continue;
            }

            int next = (edge + 1) % edgeCount;
            List<Vec3> side = LaserBeamSurfaces.validPolygon(List.of(starts.get(edge), ends.get(edge), ends.get(next), starts.get(next)));
            if (side.isEmpty()) {
                continue;
            }

            Vec3 outward = context.toLocalDirection(outwardDirections.get(edge));
            if (!polygonal && (edge == 1 || edge == 3)) {
                add(generated, side, draw, startCenter, endCenter, outward);
                continue;
            }

            List<List<Vec3>> triangles = new ArrayList<>();
            Vec3 firstSideVertex = side.getFirst();
            for (int index = 1; index + 1 < side.size(); index++) {
                triangles.add(List.of(firstSideVertex, side.get(index), side.get(index + 1)));
            }

            for (List<Vec3> triangle : triangles) {
                Vec3 first = triangle.getFirst();
                Vec3 cross = triangle.get(1).subtract(first).cross(triangle.get(2).subtract(first));
                if (cross.length() < MIN_FACE_NORMAL_LENGTH) {
                    continue;
                }

                DrawSection supported = draw;
                if (interior != null) {
                    Vec3 normal = cross.normalize();
                    Vec3 apex = interior;
                    double distance = Math.abs(apex.subtract(first).dot(normal));
                    for (Vec3 vertex : supportVertices) {
                        double candidate = Math.abs(vertex.subtract(first).dot(normal));
                        if (!(candidate > distance)) {
                            continue;
                        }

                        apex = vertex;
                        distance = candidate;
                    }

                    Section tetrahedron = new Section(Collections.nCopies(3, apex), triangle, false, true);
                    MappedData tetrahedronData = new MappedData(tetrahedron, field, halo, boundaries);
                    supported = new DrawSection(logical, appearance, representative, along, null, domain, false, field, tetrahedronData);
                    if (!tetrahedron.isRenderable(supported.getReferenceWidth())) {
                        throw new UnsupportedOperationException("Mapped exterior has no valid local volume support.");
                    }
                }

                add(generated, triangle, supported, startCenter, endCenter, outward);
            }
        }
    }

    private static Section local(LaserBeamContext context, Section geometry) {
        return new Section(geometry.starts().stream().map(context::toLocalPosition).toList(), geometry.ends().stream().map(context::toLocalPosition).toList(), geometry.fadesAtEnd(), geometry.variableWidth());
    }

    private static LaserBeamBrightness local(LaserBeamContext context, LaserBeamBrightness brightness) {
        return new LaserBeamBrightness(context.toLocalPosition(brightness.origin()), context.toLocalDirection(brightness.direction()), brightness.profile());
    }

    private static void add(List<Face> faces, List<Vec3> points, DrawSection draw, Vec3 start, Vec3 end, Vec3 outward) {
        Vec3 first = points.getFirst();
        Vec3 normal = points.get(1).subtract(first).cross(points.get(2).subtract(first));
        if (normal.length() < MIN_FACE_NORMAL_LENGTH) {
            return;
        }

        normal = normal.normalize();
        if (normal.dot(outward) < 0) {
            points = new ArrayList<>(points);
            Collections.reverse(points);
            normal = normal.scale(-1);
        }

        Vec3 boundsOrigin = points.getFirst();
        AABB bounds = new AABB(boundsOrigin, boundsOrigin);
        for (Vec3 point : points) {
            bounds = bounds.minmax(new AABB(point, point));
        }

        faces.add(new Face(List.copyOf(points), normal, draw, start, end, bounds));
    }

    public enum Status {
        NO_FAMILY,
        APPLIED,
        UNSUPPORTED
    }

    public static final class Cache {
        private final ShellReuse shells = new ShellReuse();
        @Nullable LaserBeamDisplayInput input;

        public @Nullable LaserBeamDisplayInput input() {
            return input;
        }
    }

    public record Result(List<DrawSection> sections, ShellMesh shells, Status status, String reason, @Nullable Cache cache) {
        public Result {
            sections = List.copyOf(sections);
        }

        public Result(List<DrawSection> sections, ShellMesh shells, Status status, String reason) {
            this(sections, shells, status, reason, null);
        }
    }
}
