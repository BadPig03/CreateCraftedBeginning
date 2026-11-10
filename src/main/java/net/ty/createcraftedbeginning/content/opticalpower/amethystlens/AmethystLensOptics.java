package net.ty.createcraftedbeginning.content.opticalpower.amethystlens;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalApertureFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalGeometry;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalHit;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalInteraction;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalInteractor;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalSurface;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamProfile;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
record AmethystLensOptics(AmethystLensGeometry geometry, int focalLength, boolean converging) implements OpticalGeometry, OpticalInteractor {
    private static final double LENS_PLANE_TOLERANCE = 1.0E-7;

    @Override
    public @Unmodifiable List<Double> displayKey() {
        if (!converging) {
            return List.of((double) focalLength, 0.0);
        }

        return List.of((double) focalLength, 1.0);
    }

    @Override
    public VoxelShape getCollisionShape() {
        return geometry.opticalShape();
    }

    @Override
    public OpticalSurface getSurface(BlockHitResult hit, Vec3 localDirection) {
        Vec3 center = Vec3.atCenterOf(hit.getBlockPos());
        Vec3 offset = hit.getLocation().subtract(center);
        Vec3 normal = geometry.normal();
        OpticalApertureFrame frame = geometry.apertureFrame(center);
        if (Math.abs(offset.dot(normal)) > LENS_PLANE_TOLERANCE || geometry.isOutsideAperture(offset)) {
            return new OpticalSurface(Vec3.atLowerCornerOf(hit.getDirection().getNormal()), null, ABSORBING, frame);
        }

        if (localDirection.dot(normal) > 0) {
            normal = normal.scale(-1);
        }
        return new OpticalSurface(normal, frame.opening(), this, frame);
    }

    @Override
    public boolean supportsDisplayMeshReuse() {
        return true;
    }

    @Override
    public OpticalInteraction interact(BeamState incident, OpticalHit hit) {
        BeamProfile profile = incident.profile();
        double referenceWidth = profile.referenceWidth();
        double change = referenceWidth / focalLength;
        if (converging) {
            change = -change;
        }
        BeamProfile outgoing = new BeamProfile(referenceWidth, profile.widthSlope() + change);
        return new OpticalInteraction(new BeamState(incident.frame(), outgoing, incident.fluxLm(), incident.footprint()), 0);
    }
}
