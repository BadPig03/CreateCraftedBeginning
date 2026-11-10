package net.ty.createcraftedbeginning.content.opticalpower.amethystmirror;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3d;
import org.joml.Quaterniond;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystMirrorCollisionShape extends VoxelShape {
    private static final double CONTACT_EPSILON = 1.0E-7;
    private static final double MIN_AXIS_LENGTH_SQR = 1.0E-12;
    private static final List<Vec3> WORLD_AXES = List.of(new Vec3(1, 0, 0), new Vec3(0, 1, 0), new Vec3(0, 0, 1));
    private final AmethystMirrorGeometry geometry;
    private final Quaterniond localRotation;
    private final List<Projection> projections;
    private final Vec3 center;
    private final AABB bounds;
    private final DoubleList xCoordinates;
    private final DoubleList yCoordinates;
    private final DoubleList zCoordinates;
    private @Nullable AmethystMirrorCollisionShape translated;

    public AmethystMirrorCollisionShape(AmethystMirrorGeometry geometry) {
        super(new BitSetDiscreteVoxelShape(1, 1, 1));
        shape.fill(0, 0, 0);
        this.geometry = geometry;
        Vec3 horizontal = geometry.horizontal();
        Vec3 vertical = geometry.vertical();
        Vec3 normal = geometry.normal();
        Matrix3d basis = new Matrix3d(horizontal.x, horizontal.y, horizontal.z, vertical.x, vertical.y, vertical.z, -normal.x, -normal.y, -normal.z);
        localRotation = new Quaterniond().setFromNormalized(basis);
        center = new Vec3(0.5, 0.5, 0.5);
        List<Vec3> localAxes = List.of(horizontal, vertical, normal);
        List<Vec3> axes = new ArrayList<>(WORLD_AXES);
        axes.addAll(localAxes);
        for (Vec3 worldAxis : WORLD_AXES) {
            for (Vec3 localAxis : localAxes) {
                Vec3 cross = worldAxis.cross(localAxis);
                if (cross.lengthSqr() < MIN_AXIS_LENGTH_SQR) {
                    continue;
                }

                axes.add(cross.normalize());
            }
        }
        List<Projection> computed = new ArrayList<>(axes.size());
        for (Vec3 axis : axes) {
            computed.add(new Projection(axis, projectedRadius(axis)));
        }
        projections = List.copyOf(computed);
        double x = projections.get(0).radius();
        double y = projections.get(1).radius();
        double z = projections.get(2).radius();
        bounds = new AABB(0.5 - x, 0.5 - y, 0.5 - z, 0.5 + x, 0.5 + y, 0.5 + z);
        xCoordinates = DoubleList.of(bounds.minX, bounds.maxX);
        yCoordinates = DoubleList.of(bounds.minY, bounds.maxY);
        zCoordinates = DoubleList.of(bounds.minZ, bounds.maxZ);
    }

    private AmethystMirrorCollisionShape(AmethystMirrorCollisionShape source, double x, double y, double z) {
        super(source.shape);
        geometry = source.geometry;
        localRotation = source.localRotation;
        projections = source.projections;
        center = source.center.add(x, y, z);
        bounds = source.bounds.move(x, y, z);
        xCoordinates = DoubleList.of(bounds.minX, bounds.maxX);
        yCoordinates = DoubleList.of(bounds.minY, bounds.maxY);
        zCoordinates = DoubleList.of(bounds.minZ, bounds.maxZ);
    }

    @Override
    public DoubleList getCoords(Axis axis) {
        return switch (axis) {
            case X -> xCoordinates;
            case Y -> yCoordinates;
            case Z -> zCoordinates;
        };
    }

    @Override
    public AABB bounds() {
        return bounds;
    }

    @Override
    public VoxelShape move(double x, double y, double z) {
        if (x == 0 && y == 0 && z == 0) {
            return this;
        }

        AmethystMirrorCollisionShape moved = translated;
        if (moved == null || moved.center.x != center.x + x || moved.center.y != center.y + y || moved.center.z != center.z + z) {
            moved = new AmethystMirrorCollisionShape(this, x, y, z);
            translated = moved;
        }
        return moved;
    }

    @Override
    public VoxelShape optimize() {
        return this;
    }

    @Override
    public double collide(Axis movementAxis, AABB box, double offset) {
        if (Math.abs(offset) < CONTACT_EPSILON) {
            return 0;
        }

        double dx = (box.minX + box.maxX) * 0.5 - center.x;
        double dy = (box.minY + box.maxY) * 0.5 - center.y;
        double dz = (box.minZ + box.maxZ) * 0.5 - center.z;
        double halfX = box.getXsize() * 0.5;
        double halfY = box.getYsize() * 0.5;
        double halfZ = box.getZsize() * 0.5;
        double entry = Double.NEGATIVE_INFINITY;
        double exit = Double.POSITIVE_INFINITY;
        for (Projection projection : projections) {
            Vec3 axis = projection.axis();
            double distance = axis.x * dx + axis.y * dy + axis.z * dz;
            double radius = projection.radius() + Math.abs(axis.x) * halfX + Math.abs(axis.y) * halfY + Math.abs(axis.z) * halfZ;
            double speed = movementAxis.choose(axis.x, axis.y, axis.z) * offset;
            if (Math.abs(speed) < MIN_AXIS_LENGTH_SQR) {
                if (Math.abs(distance) >= radius - CONTACT_EPSILON) {
                    return offset;
                }

                continue;
            }

            double first = (-radius - distance) / speed;
            double last = (radius - distance) / speed;
            entry = Math.max(entry, Math.min(first, last));
            exit = Math.min(exit, Math.max(first, last));
            if (entry >= exit) {
                return offset;
            }
        }
        if (exit <= 0 || entry > 1 || entry * Math.abs(offset) < -CONTACT_EPSILON) {
            return offset;
        }

        return offset * Math.max(0, entry);
    }

    public AmethystMirrorGeometry geometry() {
        return geometry;
    }

    public void applyRotation(Quaterniond orientation) {
        orientation.mul(localRotation);
    }

    public CubeIntersection prepareCubeIntersection(double halfSize) {
        Projection[] prepared = new Projection[projections.size()];
        for (int i = 0; i < prepared.length; i++) {
            Projection projection = projections.get(i);
            Vec3 axis = projection.axis();
            double radius = projection.radius() + Math.abs(axis.x) * halfSize + Math.abs(axis.y) * halfSize + Math.abs(axis.z) * halfSize;
            prepared[i] = new Projection(axis, radius - CONTACT_EPSILON);
        }
        return new CubeIntersection(center, prepared);
    }

    public boolean intersects(VoxelShape other) {
        if (other.isEmpty()) {
            return false;
        }

        AABB otherBounds = other.bounds();
        if (!bounds.intersects(otherBounds)) {
            return false;
        }

        if (other instanceof AmethystMirrorCollisionShape mirror) {
            Vec3 delta = mirror.center.subtract(center);
            List<Vec3> firstAxes = List.of(geometry.horizontal(), geometry.vertical(), geometry.normal());
            List<Vec3> secondAxes = List.of(mirror.geometry.horizontal(), mirror.geometry.vertical(), mirror.geometry.normal());
            List<Vec3> axes = new ArrayList<>(firstAxes);
            axes.addAll(secondAxes);
            for (Vec3 first : firstAxes) {
                for (Vec3 second : secondAxes) {
                    Vec3 cross = first.cross(second);
                    if (cross.lengthSqr() < MIN_AXIS_LENGTH_SQR) {
                        continue;
                    }

                    axes.add(cross.normalize());
                }
            }
            for (Vec3 axis : axes) {
                if (Math.abs(delta.dot(axis)) >= projectedRadius(axis) + mirror.projectedRadius(axis) - CONTACT_EPSILON) {
                    return false;
                }
            }
            return true;
        }

        if (other.getCoords(Axis.X).size() == 2 && other.getCoords(Axis.Y).size() == 2 && other.getCoords(Axis.Z).size() == 2) {
            return intersects(otherBounds);
        }

        for (AABB box : other.toAabbs()) {
            if (!intersects(box)) {
                continue;
            }

            return true;
        }
        return false;
    }

    public boolean intersects(AABB box) {
        double dx = (box.minX + box.maxX) * 0.5 - center.x;
        double dy = (box.minY + box.maxY) * 0.5 - center.y;
        double dz = (box.minZ + box.maxZ) * 0.5 - center.z;
        double halfX = box.getXsize() * 0.5;
        double halfY = box.getYsize() * 0.5;
        double halfZ = box.getZsize() * 0.5;
        for (Projection projection : projections) {
            Vec3 axis = projection.axis();
            double distance = axis.x * dx + axis.y * dy + axis.z * dz;
            double radius = projection.radius() + Math.abs(axis.x) * halfX + Math.abs(axis.y) * halfY + Math.abs(axis.z) * halfZ;
            if (Math.abs(distance) >= radius - CONTACT_EPSILON) {
                return false;
            }
        }
        return true;
    }

    private double projectedRadius(Vec3 axis) {
        return AmethystMirrorGeometry.FRAME_HALF_SIZE * (Math.abs(geometry.horizontal().dot(axis)) + Math.abs(geometry.vertical().dot(axis))) + AmethystMirrorGeometry.HALF_THICKNESS * Math.abs(geometry.normal().dot(axis));
    }

    private record Projection(Vec3 axis, double radius) {
    }

    public static final class CubeIntersection {
        private final Vec3 center;
        private final Projection[] projections;

        private CubeIntersection(Vec3 center, Projection[] projections) {
            this.center = center;
            this.projections = projections;
        }

        public boolean intersects(double x, double y, double z) {
            double dx = x - center.x;
            double dy = y - center.y;
            double dz = z - center.z;
            for (Projection projection : projections) {
                Vec3 axis = projection.axis();
                double distance = axis.x * dx + axis.y * dy + axis.z * dz;
                if (Math.abs(distance) >= projection.radius()) {
                    return false;
                }
            }
            return true;
        }
    }
}
