package net.ty.createcraftedbeginning.content.opticalpower.laseremitter.beamrender;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamContext;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamMesh.DrawSection;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamParticles.ShellSampler;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamShells.ShellMesh;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamSurfaces.Face;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamView;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamWork;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserBeamPreparedBuffers {
    private LaserBeamPreparedBuffers() {}

    static LaserBeamWork<Result> begin(LaserBeamContext context, List<DrawSection> sections, ShellMesh shells, Vec3 blockOrigin) {
        return new BufferWork(context, sections, shells, blockOrigin);
    }

    record Result(LaserBeamView view, LaserBeamVertices vertices, @Nullable ShellSampler particles) {}

    private static final class BufferWork implements LaserBeamWork<Result> {
        private final LaserBeamContext context;
        private final List<DrawSection> sections;
        private final ShellMesh shells;
        private final Vec3 blockOrigin;
        private @Nullable LaserBeamWork<LaserBeamView> viewWork;
        private @Nullable LaserBeamWork<LaserBeamVertices> verticesWork;
        private @Nullable LaserBeamWork<ShellSampler> samplerWork;
        private @Nullable LaserBeamView view;
        private @Nullable LaserBeamVertices vertices;
        private @Nullable ShellSampler particles;
        private int phase;
        private @Nullable Result result;

        private BufferWork(LaserBeamContext context, List<DrawSection> sections, ShellMesh shells, Vec3 blockOrigin) {
            this.context = context;
            this.sections = sections;
            this.shells = shells;
            this.blockOrigin = blockOrigin;
        }

        @Override
        public boolean step() {
            if (result != null) {
                return true;
            }

            if (phase == 0) {
                LaserBeamWork<LaserBeamView> work = viewWork;
                if (work == null) {
                    viewWork = LaserBeamView.begin(context, sections);
                    return false;
                }

                if (!work.step()) {
                    return false;
                }

                view = work.result();
                viewWork = null;
                phase = 1;
                return false;
            }

            if (phase == 1) {
                LaserBeamWork<LaserBeamVertices> work = verticesWork;
                if (work == null) {
                    verticesWork = LaserBeamVertices.begin(blockOrigin, shells.layers());
                    return false;
                }

                if (!work.step()) {
                    return false;
                }

                vertices = work.result();
                verticesWork = null;
                phase = 2;
                return false;
            }

            List<Face> faces = shells.layers().getFirst();
            if (!faces.isEmpty()) {
                LaserBeamWork<ShellSampler> work = samplerWork;
                if (work == null) {
                    samplerWork = ShellSampler.begin(context, sections, faces);
                    return false;
                }

                if (!work.step()) {
                    return false;
                }

                particles = work.result();
                samplerWork = null;
            }

            result = new Result(Objects.requireNonNull(view, "Laser buffer preparation requires a completed view."), Objects.requireNonNull(vertices, "Laser buffer preparation requires completed vertices."), particles);
            return true;
        }

        @Override
        public Result result() {
            return Objects.requireNonNull(result, "Buffer preparation has not completed.");
        }
    }
}
