package net.ty.createcraftedbeginning.content.opticalpower.amethystlens;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.content.opticalpower.laser.OpticalApertureFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.geometry.OpticalAperture;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystLensGeometry {
    static final double LENS_HALF_THICKNESS = 0.03125;
    private static final double APERTURE_HALF_SIZE = 0.375;
    private static final double FRAME_HALF_SIZE = 0.5;
    private static final double FRAME_HALF_THICKNESS = 0.125;
    private static final Map<Direction, AmethystLensGeometry> GEOMETRIES = createGeometries();

    private final Vec3 normal;
    private final Vec3 horizontal;
    private final Vec3 vertical;
    private final VoxelShape physicalShape;
    private final VoxelShape opticalShape;

    private AmethystLensGeometry(Direction facing) {
        normal = Vec3.atLowerCornerOf(facing.getNormal());
        Vec3 axis = new Vec3(1, 0, 0);
        if (facing.getAxis() == Axis.X) {
            axis = new Vec3(0, 0, 1);
        }
        horizontal = axis;
        vertical = normal.cross(horizontal);
        double aperture = APERTURE_HALF_SIZE;
        double outer = FRAME_HALF_SIZE;
        double thickness = FRAME_HALF_THICKNESS;
        VoxelShape frame = Shapes.or(box(-outer, -outer, -thickness, -aperture, outer, thickness), box(aperture, -outer, -thickness, outer, outer, thickness), box(-aperture, -outer, -thickness, aperture, -aperture, thickness), box(-aperture, aperture, -thickness, aperture, outer, thickness));
        physicalShape = Shapes.or(frame, box(-aperture, -aperture, -LENS_HALF_THICKNESS, aperture, aperture, LENS_HALF_THICKNESS));
        opticalShape = new LensPlaneShape(this, frame);
    }

    static AmethystLensGeometry forFacing(Direction facing) {
        return GEOMETRIES.get(facing);
    }

    private static @Unmodifiable Map<Direction, AmethystLensGeometry> createGeometries() {
        Map<Direction, AmethystLensGeometry> geometries = new EnumMap<>(Direction.class);
        for (Direction facing : Iterate.directions) {
            geometries.put(facing, new AmethystLensGeometry(facing));
        }
        return Map.copyOf(geometries);
    }

    Vec3 normal() {
        return normal;
    }

    VoxelShape physicalShape() {
        return physicalShape;
    }

    VoxelShape opticalShape() {
        return opticalShape;
    }

    OpticalApertureFrame apertureFrame(Vec3 center) {
        return new OpticalApertureFrame(new OpticalAperture(center, horizontal, vertical, APERTURE_HALF_SIZE), FRAME_HALF_SIZE, FRAME_HALF_THICKNESS);
    }

    boolean isOutsideAperture(Vec3 offset) {
        return Math.abs(offset.dot(horizontal)) >= APERTURE_HALF_SIZE || Math.abs(offset.dot(vertical)) >= APERTURE_HALF_SIZE;
    }

    private VoxelShape box(double x1, double y1, double z1, double x2, double y2, double z2) {
        Vec3 first = horizontal.scale(x1).add(vertical.scale(y1)).add(normal.scale(z1)).add(0.5, 0.5, 0.5);
        Vec3 second = horizontal.scale(x2).add(vertical.scale(y2)).add(normal.scale(z2)).add(0.5, 0.5, 0.5);
        return Shapes.create(new AABB(first, second));
    }

    private static final class LensPlaneShape extends VoxelShape {
        private static final double PARALLEL_PLANE_TOLERANCE = 1.0E-12;

        private final AmethystLensGeometry geometry;
        private final List<AABB> frame;
        private final VoxelShape bounds;
        private final List<AABB> boxes;

        private LensPlaneShape(AmethystLensGeometry geometry, VoxelShape frame) {
            super(new BitSetDiscreteVoxelShape(1, 1, 1));
            shape.fill(0, 0, 0);
            this.geometry = geometry;
            this.frame = List.copyOf(frame.toAabbs());
            bounds = Shapes.create(geometry.physicalShape.bounds());
            List<AABB> parts = new ArrayList<>(this.frame);
            Vec3 diagonal = geometry.horizontal.scale(APERTURE_HALF_SIZE).add(geometry.vertical.scale(APERTURE_HALF_SIZE));
            parts.add(new AABB(new Vec3(0.5, 0.5, 0.5).subtract(diagonal), new Vec3(0.5, 0.5, 0.5).add(diagonal)));
            boxes = List.copyOf(parts);
        }

        @Override
        public DoubleList getCoords(Axis axis) {
            return bounds.getCoords(axis);
        }

        @Override
        public List<AABB> toAabbs() {
            return boxes;
        }

        @Override
        public @Nullable BlockHitResult clip(Vec3 start, Vec3 end, BlockPos pos) {
            Vec3 delta = end.subtract(start);
            BlockHitResult obstruction = AABB.clip(frame, start, end, pos);
            Vec3 localStart = start.subtract(Vec3.atLowerCornerOf(pos));
            for (AABB box : frame) {
                if (!box.contains(localStart)) {
                    continue;
                }

                obstruction = new BlockHitResult(start, Direction.getNearest(delta.x, delta.y, delta.z).getOpposite(), pos.immutable(), true);
                break;
            }
            Vec3 normal = geometry.normal;
            double denominator = delta.dot(normal);
            if (Math.abs(denominator) < PARALLEL_PLANE_TOLERANCE) {
                return obstruction;
            }

            Vec3 center = Vec3.atCenterOf(pos);
            double segmentFraction = center.subtract(start).dot(normal) / denominator;
            if (segmentFraction <= 0 || segmentFraction > 1) {
                return obstruction;
            }

            Vec3 contact = start.lerp(end, segmentFraction);
            Vec3 offset = contact.subtract(center);
            if (geometry.isOutsideAperture(offset) || obstruction != null && start.distanceToSqr(obstruction.getLocation()) <= start.distanceToSqr(contact)) {
                return obstruction;
            }

            Direction side = Direction.getNearest(normal.x, normal.y, normal.z);
            if (denominator > 0) {
                side = side.getOpposite();
            }
            return new BlockHitResult(contact, side, pos.immutable(), false);
        }
    }
}
