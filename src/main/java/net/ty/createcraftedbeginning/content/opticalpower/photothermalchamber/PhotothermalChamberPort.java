package net.ty.createcraftedbeginning.content.opticalpower.photothermalchamber;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.foundation.block.CCBShapes;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Internal
public enum PhotothermalChamberPort {
    NORTH(Direction.NORTH, 6, 6, -0.5, 10, 10, 0.5),
    SOUTH(Direction.SOUTH, 6, 6, 15.5, 10, 10, 16.5),
    WEST(Direction.WEST, -0.5, 6, 6, 0.5, 10, 10),
    EAST(Direction.EAST, 15.5, 6, 6, 16.5, 10, 10),
    DOWN(Direction.DOWN, 6, 0, 6, 10, 1, 10);

    private static final List<PhotothermalChamberPort> PORTS = List.of(values());
    private static final VoxelShape LASER_SHAPE = createLaserShape();

    private final Direction direction;
    private final VoxelShape shape;

    PhotothermalChamberPort(Direction direction, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.direction = direction;
        shape = Block.box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    static VoxelShape getLaserShape() {
        return LASER_SHAPE;
    }

    static List<PhotothermalChamberPort> getPorts() {
        return PORTS;
    }

    private static VoxelShape createLaserShape() {
        VoxelShape shape = CCBShapes.PHOTOTHERMAL_CHAMBER;
        for (PhotothermalChamberPort port : PORTS) {
            shape = Shapes.or(shape, port.shape);
        }
        return shape;
    }

    Direction getDirection() {
        return direction;
    }

    VoxelShape getReceivingShape() {
        return shape;
    }

    int getMask() {
        return 1 << ordinal();
    }
}
