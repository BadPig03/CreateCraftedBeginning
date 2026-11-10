package net.ty.createcraftedbeginning.content.opticalpower.amethystmirror;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.Shapes.DoubleLineConsumer;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

import static net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorGeometry.FRAME_HALF_SIZE;
import static net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorGeometry.HALF_THICKNESS;
import static net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorGeometry.MIRROR_HALF_SIZE;
import static net.ty.createcraftedbeginning.content.opticalpower.amethystmirror.AmethystMirrorGeometry.MIRROR_HALF_THICKNESS;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystMirrorShape extends VoxelShape {
    private static final List<AABB> SURFACE_BOXES = List.of(
        new AABB(-FRAME_HALF_SIZE, -FRAME_HALF_SIZE, -HALF_THICKNESS, -MIRROR_HALF_SIZE, FRAME_HALF_SIZE, HALF_THICKNESS),
        new AABB(MIRROR_HALF_SIZE, -FRAME_HALF_SIZE, -HALF_THICKNESS, FRAME_HALF_SIZE, FRAME_HALF_SIZE, HALF_THICKNESS),
        new AABB(-MIRROR_HALF_SIZE, -FRAME_HALF_SIZE, -HALF_THICKNESS, MIRROR_HALF_SIZE, -MIRROR_HALF_SIZE, HALF_THICKNESS),
        new AABB(-MIRROR_HALF_SIZE, MIRROR_HALF_SIZE, -HALF_THICKNESS, MIRROR_HALF_SIZE, FRAME_HALF_SIZE, HALF_THICKNESS),
        new AABB(-MIRROR_HALF_SIZE, -MIRROR_HALF_SIZE, -MIRROR_HALF_THICKNESS, MIRROR_HALF_SIZE, MIRROR_HALF_SIZE, MIRROR_HALF_THICKNESS)
    );
    private static final VoxelShape PANEL = Shapes.or(Shapes.create(SURFACE_BOXES.getFirst()), SURFACE_BOXES.subList(1, SURFACE_BOXES.size()).stream().map(Shapes::create).toArray(VoxelShape[]::new));
    private static final List<AABB> BOXES = List.copyOf(PANEL.toAabbs());
    private static final double MIN_RAY_LENGTH_SQR = 1.0E-7;
    private final AmethystMirrorGeometry geometry;
    private final VoxelShape bounds;
    private @Nullable List<Edge> outline;

    public AmethystMirrorShape(AmethystMirrorGeometry geometry) {
        super(new BitSetDiscreteVoxelShape(1, 1, 1));
        shape.fill(0, 0, 0);
        this.geometry = geometry;
        Vec3 horizontal = geometry.horizontal().scale(FRAME_HALF_SIZE);
        Vec3 vertical = geometry.vertical().scale(FRAME_HALF_SIZE);
        Vec3 normal = geometry.normal().scale(HALF_THICKNESS);
        double x = Math.abs(horizontal.x) + Math.abs(vertical.x) + Math.abs(normal.x);
        double y = Math.abs(horizontal.y) + Math.abs(vertical.y) + Math.abs(normal.y);
        double z = Math.abs(horizontal.z) + Math.abs(vertical.z) + Math.abs(normal.z);
        bounds = Shapes.box(0.5 - x, 0.5 - y, 0.5 - z, 0.5 + x, 0.5 + y, 0.5 + z);
    }

    @Override
    public DoubleList getCoords(Axis axis) {
        return bounds.getCoords(axis);
    }

    @Override
    public void forAllEdges(DoubleLineConsumer action) {
        List<Edge> edges = outline;
        if (edges == null) {
            List<Edge> rotated = new ArrayList<>();
            PANEL.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
                Vec3 start = geometry.toWorld(new Vec3(x1, y1, z1)).add(0.5, 0.5, 0.5);
                Vec3 end = geometry.toWorld(new Vec3(x2, y2, z2)).add(0.5, 0.5, 0.5);
                rotated.add(new Edge(start, end));
            });
            edges = List.copyOf(rotated);
            outline = edges;
        }
        for (Edge edge : edges) {
            Vec3 start = edge.start();
            Vec3 end = edge.end();
            action.consume(start.x, start.y, start.z, end.x, end.y, end.z);
        }
    }

    @Override
    public @Nullable BlockHitResult clip(Vec3 start, Vec3 end, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        Vec3 localStart = geometry.toLocal(start.subtract(center));
        Vec3 localEnd = geometry.toLocal(end.subtract(center));
        Vec3 direction = localEnd.subtract(localStart);
        if (direction.lengthSqr() < MIN_RAY_LENGTH_SQR) {
            return null;
        }

        double nearest = Double.POSITIVE_INFINITY;
        Direction incomingNormal = Direction.getNearest(direction.x, direction.y, direction.z).getOpposite();
        Direction nearestNormal = incomingNormal;
        boolean inside = false;
        for (AABB box : BOXES) {
            double entry = Double.NEGATIVE_INFINITY;
            double exit = Double.POSITIVE_INFINITY;
            Direction normal = incomingNormal;
            for (Axis axis : Axis.values()) {
                double origin = axis.choose(localStart.x, localStart.y, localStart.z);
                double velocity = axis.choose(direction.x, direction.y, direction.z);
                double minimum = axis.choose(box.minX, box.minY, box.minZ);
                double maximum = axis.choose(box.maxX, box.maxY, box.maxZ);
                if (velocity == 0) {
                    if (origin < minimum || origin >= maximum) {
                        exit = Double.NEGATIVE_INFINITY;
                        break;
                    }

                    continue;
                }

                double first = (minimum - origin) / velocity;
                double last = (maximum - origin) / velocity;
                if (velocity < 0) {
                    double swap = first;
                    first = last;
                    last = swap;
                }
                if (first > entry) {
                    entry = first;
                    normal = Direction.fromAxisAndDirection(axis, velocity > 0 ? AxisDirection.NEGATIVE : AxisDirection.POSITIVE);
                }
                exit = Math.min(exit, last);
            }
            double distance = Math.max(0, entry);
            if (exit <= distance || distance > 1 || distance >= nearest) {
                continue;
            }

            nearest = distance;
            inside = entry < 0;
            nearestNormal = normal;
            if (!inside) {
                continue;
            }

            nearestNormal = incomingNormal;
        }
        if (!Double.isFinite(nearest)) {
            return null;
        }

        Vec3 point = start.lerp(end, nearest);
        Vec3 normal = geometry.toWorld(Vec3.atLowerCornerOf(nearestNormal.getNormal()));
        return new BlockHitResult(point, Direction.getNearest(normal.x, normal.y, normal.z), pos.immutable(), inside);
    }

    public Vec3 toPanelCoordinates(Vec3 point) {
        return geometry.toLocal(point.subtract(0.5, 0.5, 0.5));
    }

    public List<AABB> getPanelBoxes() {
        return BOXES;
    }

    public List<AABB> getSurfaceBoxes() {
        return SURFACE_BOXES;
    }

    private record Edge(Vec3 start, Vec3 end) {}
}
