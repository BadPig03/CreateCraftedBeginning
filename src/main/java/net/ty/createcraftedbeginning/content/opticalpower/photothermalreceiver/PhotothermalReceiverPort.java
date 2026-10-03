package net.ty.createcraftedbeginning.content.opticalpower.photothermalreceiver;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.foundation.block.CCBShapes;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum PhotothermalReceiverPort {
    NORTH(Direction.NORTH, 6, 6, -0.5, 10, 10, 0.5),
    SOUTH(Direction.SOUTH, 6, 6, 15.5, 10, 10, 16.5),
    WEST(Direction.WEST, -0.5, 6, 6, 0.5, 10, 10),
    EAST(Direction.EAST, 15.5, 6, 6, 16.5, 10, 10),
    DOWN(Direction.DOWN, 6, 0, 6, 10, 1, 10);

    private static final double HIT_TOLERANCE = 1.0E-5;
    private static final VoxelShape LASER_SHAPE = createLaserShape();

    private final Direction direction;
    private final VoxelShape shape;
    private final AABB hitBounds;

    PhotothermalReceiverPort(Direction direction, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.direction = direction;
        shape = Block.box(minX, minY, minZ, maxX, maxY, maxZ);
        hitBounds = shape.bounds().inflate(HIT_TOLERANCE);
    }

    public static VoxelShape getLaserShape() {
        return LASER_SHAPE;
    }

    public static @Nullable PhotothermalReceiverPort findHit(BlockHitResult hit) {
        Vec3 localHit = hit.getLocation().subtract(Vec3.atLowerCornerOf(hit.getBlockPos()));
        for (PhotothermalReceiverPort port : values()) {
            if (!port.hitBounds.contains(localHit)) {
                continue;
            }

            return port;
        }
        return null;
    }

    private static VoxelShape createLaserShape() {
        VoxelShape shape = CCBShapes.PHOTOTHERMAL_RECEIVER;
        for (PhotothermalReceiverPort port : values()) {
            shape = Shapes.or(shape, port.shape);
        }
        return shape;
    }

    public Direction getDirection() {
        return direction;
    }

    public int getMask() {
        return 1 << ordinal();
    }
}
