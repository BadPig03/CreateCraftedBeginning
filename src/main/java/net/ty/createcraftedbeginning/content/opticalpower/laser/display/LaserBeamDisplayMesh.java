package net.ty.createcraftedbeginning.content.opticalpower.laser.display;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamContours.Budget;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamDisplayInput.Base;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamMesh.DrawSection;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamShells.ShellMesh;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamShells.ShellReason;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamShells.ShellReuse;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamSurfaces.SurfaceBuild;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserRenderSnapshot;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class LaserBeamDisplayMesh {
    private LaserBeamDisplayMesh() {}

    public static LaserBeamDisplayInput prepare(LaserRenderSnapshot snapshot, List<DrawSection> sections) {
        Budget budget = new Budget();
        SurfaceBuild surfaces = LaserBeamSurfaces.build(sections, budget);
        Base base = new Base(sections, surfaces.faces(), surfaces.reason(), budget.remaining());
        return new LaserBeamDisplayInput(LaserBeamContext.from(snapshot), base);
    }

    public static LaserBeamWork<LaserBeamDisplayInput> beginPrepare(LaserRenderSnapshot snapshot) {
        return new Preparation(snapshot);
    }

    public static Result create(LaserBeamDisplayInput input) {
        Base base = input.base();
        if (!base.isComplete()) {
            return fallback(base, null);
        }

        ShellMesh shells = LaserBeamShells.build(input.context(), base.faces(), new Budget(base.remainingBudget()));
        return new Result(base.sections(), shells, Status.READY, "");
    }

    public static LaserBeamWork<Result> begin(LaserBeamDisplayInput input) {
        return begin(input, null);
    }

    public static LaserBeamWork<Result> begin(LaserBeamDisplayInput input, @Nullable Cache previous) {
        return new Build(input, previous);
    }

    private static Result fallback(Base base, @Nullable Cache cache) {
        ShellMesh shells = new ShellMesh(List.of(List.of(), List.of(), base.faces()), 3, ShellReason.LOCAL_FALLBACK);
        return new Result(base.sections(), shells, Status.UNSUPPORTED, "Original surface generation is incomplete: " + base.reason() + "; retained current source caps=" + base.faces().size() + '.', cache);
    }

    private static final class Preparation implements LaserBeamWork<LaserBeamDisplayInput> {
        private final LaserRenderSnapshot snapshot;
        private @Nullable LaserBeamDisplayInput result;

        private Preparation(LaserRenderSnapshot snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public boolean step() {
            if (result == null) {
                result = prepare(snapshot, LaserBeamMesh.create(snapshot));
            }

            return true;
        }

        @Override
        public LaserBeamDisplayInput result() {
            return Objects.requireNonNull(result, "Whole-beam display preparation has not completed.");
        }
    }

    private static final class Build implements LaserBeamWork<Result> {
        private final LaserBeamDisplayInput input;
        private final @Nullable Cache previous;
        private final Cache cache = new Cache();
        private @Nullable LaserBeamWork<ShellMesh> shellWork;
        private @Nullable Result result;

        private Build(LaserBeamDisplayInput input, @Nullable Cache previous) {
            this.input = input;
            this.previous = previous;
            cache.input = input;
        }

        @Override
        public boolean step() {
            if (result != null) {
                return true;
            }

            Base base = input.base();
            if (!base.isComplete()) {
                result = fallback(base, cache);
                return true;
            }

            LaserBeamWork<ShellMesh> work = shellWork;
            if (work == null) {
                shellWork = LaserBeamShells.begin(input.context(), base.faces(), new Budget(base.remainingBudget()), previous == null ? null : previous.shells, cache.shells);
                return false;
            }
            if (!work.step()) {
                return false;
            }

            result = new Result(base.sections(), work.result(), Status.READY, "", cache);
            shellWork = null;
            return true;
        }

        @Override
        public Result result() {
            return Objects.requireNonNull(result, "Laser beam display mesh build has not completed.");
        }
    }

    public enum Status {
        READY,
        UNSUPPORTED
    }

    public static final class Cache {
        private final ShellReuse shells = new ShellReuse();
        private @Nullable LaserBeamDisplayInput input;

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
