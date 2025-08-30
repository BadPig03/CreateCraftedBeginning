package net.ty.createcraftedbeginning.registry;

import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.BiFunction;

public class CCBShapes {
    public static final VoxelShaper AIRTIGHT_PUMP = shape(2, 0, 2, 14, 16, 14).forDirectional(Direction.UP);
    public static final VoxelShaper AIRTIGHT_INTAKE_PORT = shape(0, 0, 0, 16, 11, 16).forDirectional(Direction.UP);
    public static final VoxelShaper AIRTIGHT_ENGINE = shape(0, 0, 0, 16, 2, 16).add(2, 2, 2, 14, 6, 14).add(0, 6, 0, 16, 10, 16).add(2, 10, 2, 14, 14, 14).forDirectional(Direction.UP);
    public static final VoxelShaper CONDENSATE_DRAIN = shape(0, 0, 0, 16, 2, 16).add(3, 2, 3, 13, 13, 13).forDirectional(Direction.UP);

    public static final VoxelShape CRATE = shape(1, 0, 1, 15, 14, 15).build();
    public static final VoxelShape CHAMBER_BLOCK_SHAPE = shape(1, 0, 1, 15, 14, 15).build();
    public static final VoxelShape CHAMBER_BLOCK_COOLER_SHAPE = shape(1, 0, 1, 15, 12, 15).add(0, 12, 0, 16, 16, 16).build();
    public static final VoxelShape CHAMBER_BLOCK_SPECIAL_COLLISION_SHAPE = shape(0, 0, 0, 16, 4, 16).build();
    public static final VoxelShape GAS_INJECTION_CHAMBER_SHAPE = shape(1, 1, 1, 15, 15, 15).add(2, 15, 2, 14, 16, 14).build();

    private static Builder shape(VoxelShape shape) {
        return new Builder(shape);
    }

    private static Builder shape(double x1, double y1, double z1, double x2, double y2, double z2) {
        return shape(cuboid(x1, y1, z1, x2, y2, z2));
    }

    private static VoxelShape cuboid(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Block.box(x1, y1, z1, x2, y2, z2);
    }

    public static class Builder {
        private VoxelShape shape;

        public Builder(VoxelShape shape) {
            this.shape = shape;
        }

        public Builder add(VoxelShape shape) {
            this.shape = Shapes.or(this.shape, shape);
            return this;
        }

        public Builder add(double x1, double y1, double z1, double x2, double y2, double z2) {
            return add(cuboid(x1, y1, z1, x2, y2, z2));
        }

        public VoxelShape build() {
            return shape;
        }

        public VoxelShaper build(BiFunction<VoxelShape, Direction, VoxelShaper> factory, Direction direction) {
            return factory.apply(shape, direction);
        }

        public VoxelShaper build(BiFunction<VoxelShape, Direction.Axis, VoxelShaper> factory, Direction.Axis axis) {
            return factory.apply(shape, axis);
        }

        public VoxelShaper forDirectional(Direction direction) {
            return build(VoxelShaper::forDirectional, direction);
        }
    }
}
