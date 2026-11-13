package net.ty.createcraftedbeginning.content.opticalpower.laser.geometry;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.BeamContact.Patch;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record BeamTerminal(List<Face> faces, Vec3 worldOrigin, Vec3 axisX, Vec3 axisY, Vec3 axisZ) {
    public static final BeamTerminal EMPTY = new BeamTerminal(List.of(), Vec3.ZERO, new Vec3(1, 0, 0), new Vec3(0, 1, 0), new Vec3(0, 0, 1));

    public BeamTerminal {
        faces = List.copyOf(faces);
    }

    public double receivedFraction() {
        double fraction = 0;
        for (Face face : faces) {
            for (Patch patch : face.patches()) {
                fraction += patch.fluxFraction();
            }
        }
        if (fraction > 1.00000001) {
            throw new IllegalStateException("Terminal receiving patches exceed the incident flux fraction: " + fraction + '.');
        }

        return Math.clamp(fraction, 0, 1);
    }

    public Vec3 worldPoint(Vec3 localPoint) {
        return worldOrigin.add(axisX.scale(localPoint.x)).add(axisY.scale(localPoint.y)).add(axisZ.scale(localPoint.z));
    }

    public record Face(Direction direction, double plane, List<Patch> patches) {
        public Face {
            patches = List.copyOf(patches);
        }
    }
}
