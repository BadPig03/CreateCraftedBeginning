package net.ty.createcraftedbeginning.content.opticalpower.opticalfiber;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.foundation.block.CCBShapes;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.atomic.AtomicReferenceArray;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class OpticalFiberShapes {
    private static final int SHAPE_CACHE_SIZE = 1 << OpticalFiberBlock.CONNECTION_BITS * 2;

    private final AtomicReferenceArray<VoxelShape> deviceShapes = new AtomicReferenceArray<>(SHAPE_CACHE_SIZE);

    static VoxelShape createBaseShape(BlockState state, VoxelShape baseShape) {
        int connections = 0;
        Direction firstDirection = Direction.DOWN;
        boolean sameAxis = true;
        for (Direction direction : Iterate.directions) {
            if (!OpticalFiberBlock.isConnected(state, direction)) {
                continue;
            }

            if (connections == 0) {
                firstDirection = direction;
            }
            sameAxis &= direction.getAxis() == firstDirection.getAxis();
            connections++;
        }

        if (connections == 1) {
            return CCBShapes.OPTICAL_FIBER_END.get(firstDirection);
        }

        if (connections == 2 && sameAxis) {
            return baseShape;
        }

        return Shapes.or(baseShape, CCBShapes.OPTICAL_FIBER_JUNCTION);
    }

    VoxelShape getShape(VoxelShape baseShape, int key) {
        int ports = key >>> OpticalFiberBlock.CONNECTION_BITS;
        if (ports == 0) {
            return baseShape;
        }

        VoxelShape cached = deviceShapes.get(key);
        if (cached != null) {
            return cached;
        }

        VoxelShape shape = baseShape;
        for (Direction direction : Iterate.directions) {
            if ((ports & 1 << direction.get3DDataValue()) == 0) {
                continue;
            }

            shape = Shapes.or(shape, CCBShapes.OPTICAL_FIBER_DEVICE_PORT.get(direction));
        }
        VoxelShape previous = deviceShapes.compareAndExchange(key, null, shape);
        if (previous != null) {
            return previous;
        }

        return shape;
    }
}
