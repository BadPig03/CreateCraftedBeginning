package net.ty.createcraftedbeginning.content.opticalpower.laser.display;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamMesh.DrawSection;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamSurfaces.Face;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamSurfaces.SurfaceReason;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record LaserBeamDisplayInput(LaserBeamContext context, Base base) {
    public record Base(List<DrawSection> sections, List<Face> faces, SurfaceReason reason, int remainingBudget) {
        public Base {
            sections = List.copyOf(sections);
            faces = List.copyOf(faces);
        }

        public boolean isComplete() {
            return reason == SurfaceReason.COMPLETE || reason == SurfaceReason.EMPTY;
        }
    }

}
