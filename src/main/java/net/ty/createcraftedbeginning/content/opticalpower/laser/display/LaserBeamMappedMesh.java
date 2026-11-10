package net.ty.createcraftedbeginning.content.opticalpower.laser.display;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalHit;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalTarget;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamContours.Budget;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayInput.Base;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayInput.MappedFamily;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayInput.Mapping;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayInput.MappingState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayMesh.Cache;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayMesh.Result;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayMesh.Status;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamMappedGeometry.FamilyGeometry;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamMesh.DrawSection;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamShells.ShellMesh;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamSurfaces.Face;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamSurfaces.SurfaceBuild;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserDisplayFamilies.Family;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserRenderSnapshot;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class LaserBeamMappedMesh {
    private LaserBeamMappedMesh() {}

    public static Result create(LaserRenderSnapshot snapshot, List<DrawSection> original) {
        PreparedCore prepared = prepareCore(snapshot, original);
        Base base = prepared.base;
        ShellMesh baseline = LaserBeamDisplayMesh.baseline(prepared.context, base);
        try {
            base = classify(base);
            List<Family> families = discover(snapshot, base);
            Mapping selection = selection(snapshot, families);
            Result rejection = LaserBeamDisplayMesh.reject(base, selection, baseline, null);
            if (rejection != null) {
                return rejection;
            }

            Mapping mapping = mapped(base, selection.selected(), LaserBeamMappedGeometry.create(families));
            return LaserBeamDisplayMesh.createMapped(new LaserBeamDisplayInput(prepared.context, base, mapping), baseline);
        }
        catch (UnsupportedOperationException unsupported) {
            return new Result(base.sections(), baseline, Status.UNSUPPORTED, unsupported.getMessage());
        }
    }

    public static LaserBeamDisplayInput prepare(LaserRenderSnapshot snapshot, List<DrawSection> original) {
        PreparedCore prepared = prepareCore(snapshot, original);
        Base base = classify(prepared.base);
        Mapping mapping = new Mapping(MappingState.NO_FAMILY, "No eligible source-domain display family.", Set.of(), List.of());
        try {
            List<Family> families = discover(snapshot, base);
            mapping = selection(snapshot, families);
            if (base.isComplete() && mapping.state() == MappingState.READY) {
                mapping = mapped(base, mapping.selected(), LaserBeamMappedGeometry.create(families));
            }
        }
        catch (UnsupportedOperationException unsupported) {
            mapping = new Mapping(MappingState.UNSUPPORTED, unsupported.getMessage(), mapping.selected(), List.of());
        }

        return new LaserBeamDisplayInput(prepared.context, base, mapping);
    }

    public static LaserBeamWork<LaserBeamDisplayInput> beginPrepare(LaserRenderSnapshot snapshot) {
        return beginPrepare(snapshot, true);
    }

    public static LaserBeamWork<LaserBeamDisplayInput> beginPrepare(LaserRenderSnapshot snapshot, boolean mapping) {
        return new Preparation(snapshot, mapping);
    }

    public static LaserBeamWork<Result> beginExact(LaserRenderSnapshot snapshot, List<DrawSection> original, @Nullable Cache previous) {
        return new Build(snapshot, original, previous);
    }

    private static PreparedCore prepareCore(LaserRenderSnapshot snapshot, List<DrawSection> original) {
        Budget budget = new Budget();
        SurfaceBuild surfaces = LaserBeamSurfaces.build(original, budget);
        Base base = new Base(original, surfaces.faces(), surfaces.reason(), budget.remaining(), Set.of());
        return new PreparedCore(LaserBeamContext.from(snapshot), base);
    }

    private static Base classify(Base base) {
        Set<Integer> invalid = new HashSet<>();
        List<Face> faces = base.faces();
        for (Face face : faces) {
            DrawSection draw = face.draw();
            if (draw.renderSupport().isRenderable(draw.getReferenceWidth())) {
                continue;
            }

            invalid.add(draw.segmentIndex());
        }

        return new Base(base.sections(), faces, base.reason(), base.remainingBudget(), invalid);
    }

    private static List<Family> discover(LaserRenderSnapshot snapshot, Base base) {
        if (!base.isComplete() || moving(snapshot)) {
            return List.of();
        }

        return LaserDisplayFamilies.discover(snapshot.propagation(), base.invalidSegments());
    }

    private static boolean moving(LaserRenderSnapshot snapshot) {
        return snapshot.propagation().segments().stream().anyMatch(segment -> {
            OpticalHit hit = segment.hit();
            if (hit == null) {
                return false;
            }

            OpticalTarget target = hit.target();
            return target.contraptionId() != null || target.inSubLevel();
        });
    }

    private static Mapping selection(LaserRenderSnapshot snapshot, List<Family> families) {
        if (moving(snapshot)) {
            return new Mapping(MappingState.UNSUPPORTED, "A moving or sub-level optical target requires the original display builder.", Set.of(), List.of());
        }

        if (families.isEmpty()) {
            return new Mapping(MappingState.NO_FAMILY, "No eligible source-domain display family.", Set.of(), List.of());
        }

        Set<Integer> selected = new HashSet<>();
        families.forEach(family -> selected.addAll(family.displayIds()));
        return new Mapping(MappingState.READY, "", selected, List.of());
    }

    private static Mapping mapped(Base base, Set<Integer> selected, List<FamilyGeometry> geometries) {
        List<MappedFamily> families = geometries.stream().map(geometry -> {
            Family family = geometry.family();
            List<Integer> displayIds = family.displayIds();
            int representative = displayIds.stream().min(Integer::compareTo).orElseThrow();
            double pathDistance = base.sections().stream().filter(draw -> displayIds.contains(draw.segmentIndex())).findFirst().orElseThrow().pathDistance();
            return new MappedFamily(displayIds, representative, pathDistance, LaserBeamAppearance.fromBeam(family.source()), geometry.tiles(), geometry.polygonal());
        }).toList();
        return new Mapping(MappingState.READY, "", selected, families);
    }

    private static final class Preparation implements LaserBeamWork<LaserBeamDisplayInput> {
        private final LaserRenderSnapshot snapshot;
        private final boolean mapping;
        private Base base;
        private Mapping selection;
        private @Nullable LaserBeamWork<List<FamilyGeometry>> geometry;
        private @Nullable LaserBeamDisplayInput result;

        private Preparation(LaserRenderSnapshot snapshot, boolean mapping) {
            this.snapshot = snapshot;
            this.mapping = mapping;
        }

        @Override
        public boolean step() {
            if (result != null) {
                return true;
            }

            try {
                if (base == null) {
                    base = prepareCore(snapshot, LaserBeamMesh.create(snapshot)).base;
                    if (!mapping) {
                        result = new LaserBeamDisplayInput(LaserBeamContext.from(snapshot), base, new Mapping(MappingState.UNSUPPORTED, "Source-domain display is limited to ordinary client-world sources and optical targets.", Set.of(), List.of()));
                        return true;
                    }

                    base = classify(base);
                    List<Family> families = discover(snapshot, base);
                    selection = selection(snapshot, families);
                    if (!base.isComplete() || selection.state() != MappingState.READY) {
                        result = new LaserBeamDisplayInput(LaserBeamContext.from(snapshot), base, selection);
                        return true;
                    }

                    geometry = LaserBeamMappedGeometry.begin(families);
                    return false;
                }

                LaserBeamWork<List<FamilyGeometry>> work = Objects.requireNonNull(geometry, "Laser beam display input preparation requires active family geometry work.");
                if (!work.step()) {
                    return false;
                }

                result = new LaserBeamDisplayInput(LaserBeamContext.from(snapshot), base, mapped(base, selection.selected(), work.result()));
                geometry = null;
                return true;
            }
            catch (UnsupportedOperationException unsupported) {
                if (base == null) {
                    throw unsupported;
                }

                Set<Integer> selected = Set.of();
                if (selection != null) {
                    selected = selection.selected();
                }
                result = new LaserBeamDisplayInput(LaserBeamContext.from(snapshot), base, new Mapping(MappingState.UNSUPPORTED, unsupported.getMessage(), selected, List.of()));
                geometry = null;
                return true;
            }
        }

        @Override
        public LaserBeamDisplayInput result() {
            return Objects.requireNonNull(result, "Laser beam display input preparation has not completed.");
        }
    }

    private static final class Build implements LaserBeamWork<Result> {
        private final LaserRenderSnapshot snapshot;
        private final List<DrawSection> original;
        private final @Nullable Cache previous;
        private final Cache cache = new Cache();
        private PreparedCore prepared;
        private @Nullable LaserBeamWork<ShellMesh> shellWork;
        private @Nullable LaserBeamWork<List<FamilyGeometry>> geometryWork;
        private @Nullable LaserBeamWork<Result> meshWork;
        private final Set<Integer> invalid = new HashSet<>();
        private Base base;
        private Mapping selection;
        private ShellMesh baseline;
        private Result result;
        private int stage;
        private int index;

        private Build(LaserRenderSnapshot snapshot, List<DrawSection> original, @Nullable Cache previous) {
            this.snapshot = snapshot;
            this.original = List.copyOf(original);
            this.previous = previous;
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

                result = new Result(prepared.base.sections(), baseline, Status.UNSUPPORTED, unsupported.getMessage(), cache);
                return true;
            }
        }

        @Override
        public Result result() {
            return Objects.requireNonNull(result, "Mapped laser beam mesh build has not completed.");
        }

        private boolean advance() {
            if (stage == 0) {
                prepared = prepareCore(snapshot, original);
                base = prepared.base;
                if (!base.isComplete()) {
                    baseline = LaserBeamDisplayMesh.fallback(base);
                    result = Objects.requireNonNull(LaserBeamDisplayMesh.reject(base, selection(snapshot, List.of()), baseline, cache), "An incomplete source surface must produce a fallback laser beam display result.");
                    return true;
                }

                shellWork = LaserBeamDisplayMesh.beginBaseline(prepared.context, base, previous, cache);
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
                if (moving(snapshot)) {
                    result = Objects.requireNonNull(LaserBeamDisplayMesh.reject(base, selection(snapshot, List.of()), baseline, cache), "A moving or sub-level optical target must produce an unmapped laser beam display result.");
                    return true;
                }

                stage = 2;
                return false;
            }

            if (stage == 2) {
                if (index < base.faces().size()) {
                    DrawSection draw = base.faces().get(index++).draw();
                    if (!draw.renderSupport().isRenderable(draw.getReferenceWidth())) {
                        invalid.add(draw.segmentIndex());
                    }

                    return false;
                }

                base = new Base(base.sections(), base.faces(), base.reason(), base.remainingBudget(), invalid);
                List<Family> families = discover(snapshot, base);
                selection = selection(snapshot, families);
                if (selection.state() != MappingState.READY) {
                    cache.input = new LaserBeamDisplayInput(prepared.context, base, selection);
                }
                result = LaserBeamDisplayMesh.reject(base, selection, baseline, cache);
                if (result != null) {
                    return true;
                }

                geometryWork = LaserBeamMappedGeometry.begin(families);
                stage = 3;
                return false;
            }

            if (stage == 3) {
                LaserBeamWork<List<FamilyGeometry>> work = Objects.requireNonNull(geometryWork, "Mapped laser beam build requires active family geometry work.");
                if (!work.step()) {
                    return false;
                }

                Mapping mapping = mapped(base, selection.selected(), work.result());
                geometryWork = null;
                LaserBeamDisplayInput input = new LaserBeamDisplayInput(prepared.context, base, mapping);
                cache.input = input;
                meshWork = LaserBeamDisplayMesh.beginMapped(input, baseline, previous, cache);
                stage = 4;
                return false;
            }

            LaserBeamWork<Result> work = Objects.requireNonNull(meshWork, "Mapped laser beam build requires active display mesh work.");
            if (!work.step()) {
                return false;
            }

            result = work.result();
            meshWork = null;
            return true;
        }
    }

    private record PreparedCore(LaserBeamContext context, Base base) {}
}
