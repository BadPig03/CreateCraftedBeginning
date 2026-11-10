package net.ty.createcraftedbeginning.content.opticalpower.amethystmirror;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalGeometry;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalHit;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalInteraction;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalInteractor;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalSurface;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFootprint;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamRegion.Point;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.OpticalAperture;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashMap;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystMirrorOptics implements OpticalGeometry, OpticalInteractor {
    private static final int MAX_MOUNTED_GEOMETRIES = 256;
    private static final LinkedHashMap<Rotation, AmethystMirrorOptics> mounted = new LinkedHashMap<>(16, 0.75F, true);
    private final AmethystMirrorGeometry geometry;
    private final AmethystMirrorShape shape;

    AmethystMirrorOptics(AmethystMirrorGeometry geometry) {
        this.geometry = geometry;
        shape = new AmethystMirrorShape(geometry);
    }

    static synchronized AmethystMirrorOptics mounted(Quaternionf rotation) {
        Rotation key = new Rotation(Float.floatToRawIntBits(rotation.x), Float.floatToRawIntBits(rotation.y), Float.floatToRawIntBits(rotation.z), Float.floatToRawIntBits(rotation.w));
        AmethystMirrorOptics cached = mounted.get(key);
        if (cached != null) {
            return cached;
        }

        AmethystMirrorOptics result = new AmethystMirrorOptics(AmethystMirrorGeometry.create(rotation));
        if (mounted.size() >= MAX_MOUNTED_GEOMETRIES) {
            mounted.pollFirstEntry();
        }
        mounted.put(key, result);
        return result;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof AmethystMirrorOptics optics && geometry.equals(optics.geometry);
    }

    @Override
    public int hashCode() {
        return geometry.hashCode();
    }

    @Override
    public VoxelShape getCollisionShape() {
        return shape;
    }

    @Override
    public List<Double> displayKey() {
        Vec3 normal = geometry.normal();
        Vec3 horizontal = geometry.horizontal();
        Vec3 vertical = geometry.vertical();
        return List.of(normal.x, normal.y, normal.z, horizontal.x, horizontal.y, horizontal.z, vertical.x, vertical.y, vertical.z);
    }

    @Override
    public OpticalSurface getSurface(BlockHitResult hit, Vec3 localDirection) {
        Vec3 center = Vec3.atCenterOf(hit.getBlockPos());
        Vec3 fromCenter = hit.getLocation().subtract(center);
        Vec3 normal = geometry.normal();
        if (geometry.isReflectivePoint(fromCenter)) {
            Vec3 surfaceNormal = normal.scale(Math.signum(fromCenter.dot(normal)));
            if (localDirection.dot(surfaceNormal) < -AmethystMirrorGeometry.HIT_TOLERANCE) {
                Vec3 surfaceCenter = center.add(surfaceNormal.scale(AmethystMirrorGeometry.MIRROR_HALF_THICKNESS));
                return new OpticalSurface(surfaceNormal, new OpticalAperture(surfaceCenter, geometry.horizontal(), geometry.vertical(), AmethystMirrorGeometry.MIRROR_HALF_SIZE), this);
            }
        }

        Vec3 local = geometry.toLocal(fromCenter);
        Vec3 direction = geometry.toLocal(localDirection);
        double xDistance = Math.min(Math.abs(Math.abs(local.x) - AmethystMirrorGeometry.FRAME_HALF_SIZE), Math.abs(Math.abs(local.x) - AmethystMirrorGeometry.MIRROR_HALF_SIZE));
        double yDistance = Math.min(Math.abs(Math.abs(local.y) - AmethystMirrorGeometry.FRAME_HALF_SIZE), Math.abs(Math.abs(local.y) - AmethystMirrorGeometry.MIRROR_HALF_SIZE));
        double zDistance = Math.min(Math.abs(Math.abs(local.z) - AmethystMirrorGeometry.HALF_THICKNESS), Math.abs(Math.abs(local.z) - AmethystMirrorGeometry.MIRROR_HALF_THICKNESS));
        Vec3 faceNormal = normal;
        if (xDistance < yDistance && xDistance < zDistance) {
            faceNormal = geometry.horizontal().scale(-Math.signum(direction.x));
        }
        else if (yDistance < zDistance) {
            faceNormal = geometry.vertical().scale(-Math.signum(direction.y));
        }
        else if (direction.z < 0) {
            faceNormal = normal.scale(-1);
        }
        return new OpticalSurface(faceNormal, null, ABSORBING);
    }

    @Override
    public boolean supportsDisplayMeshReuse() {
        return true;
    }

    @Override
    public OpticalInteraction interact(BeamState incident, OpticalHit hit) {
        BeamFrame frame = incident.frame();
        Vec3 direction = frame.direction();
        Vec3 normal = hit.surface().normal();
        double incidence = direction.dot(normal);
        if (incidence >= -AmethystMirrorGeometry.HIT_TOLERANCE) {
            return ABSORBING.interact(incident, hit);
        }

        Vec3 reflectedDirection = direction.subtract(normal.scale(2 * incidence));
        Vec3 transverseAxis = frame.transverseAxis();
        Vec3 reflectedAxis = transverseAxis.subtract(normal.scale(2 * transverseAxis.dot(normal)));
        BeamFootprint footprint = incident.footprint();
        BeamRegion region = footprint.region();
        BeamRegion reflectedRegion = null;
        if (region != null) {
            reflectedRegion = new BeamRegion(region.vertices().reversed().stream().map(point -> new Point(point.u(), -point.v())).toList());
        }
        BeamFootprint reflectedFootprint = new BeamFootprint(footprint.horizontalScale(), footprint.verticalScale(), footprint.horizontalOffset(), -footprint.verticalOffset(), reflectedRegion);
        BeamState outgoing = new BeamState(new BeamFrame(hit.position(), reflectedDirection, reflectedAxis), incident.profile(), incident.fluxLm(), reflectedFootprint);
        return new OpticalInteraction(outgoing, 0);
    }

    AmethystMirrorGeometry geometry() {
        return geometry;
    }

    private record Rotation(int x, int y, int z, int w) {}
}
