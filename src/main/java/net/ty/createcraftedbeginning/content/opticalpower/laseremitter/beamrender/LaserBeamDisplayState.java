package net.ty.createcraftedbeginning.content.opticalpower.laseremitter.beamrender;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamShells.ShellMesh;
import net.ty.createcraftedbeginning.content.opticalpower.laser.display.LaserBeamSurfaces.Face;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserBeamDisplayState {
    private static final double HIDE_PIXELS = 0.75;
    private static final double RECOVER_PIXELS = 1.5;
    private boolean small;

    List<List<Face>> select(ShellMesh mesh, double widthPixels) {
        if (widthPixels < HIDE_PIXELS) {
            small = true;
        }
        else if (widthPixels >= RECOVER_PIXELS) {
            small = false;
        }
        if (small) {
            return List.of(List.of(), List.of(), mesh.layers().get(2));
        }

        return mesh.layers();
    }
}
