package net.ty.createcraftedbeginning.data;

import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

public class CCBShapes {
    public static final VoxelShaper AIRTIGHT_ENGINE = shape(0, 0, 0, 16, 2, 16).add(2, 2, 2, 14, 6, 14).add(0, 6, 0, 16, 10, 16).add(2, 10, 2, 14, 14, 14).forDirectional(Direction.UP);
    public static final VoxelShaper AIRTIGHT_HATCH = shape(2, 2, 0, 14, 14, 1).add(3, 0, 0, 13, 16, 10).forHorizontal(Direction.SOUTH);
    public static final VoxelShaper AIRTIGHT_INTAKE_PORT = shape(0, 0, 0, 16, 11, 16).forDirectional(Direction.UP);
    public static final VoxelShaper AIRTIGHT_PIPE = shape(4, 0, 4, 12, 16, 12).forAxis();
    public static final VoxelShaper AIRTIGHT_PUMP = shape(2, 0, 2, 14, 16, 14).forDirectional(Direction.UP);
    public static final VoxelShaper CHECK_VALVE = shape(4, 0, 4, 12, 3, 12).add(3, 3, 3, 13, 13, 13).add(4, 13, 4, 12, 16, 12).forAxis();
    public static final VoxelShaper CONDENSATE_DRAIN = shape(0, 0, 0, 16, 2, 16).add(3, 2, 3, 13, 13, 13).forDirectional(Direction.UP);
    public static final VoxelShaper PORTABLE_GAS_INTERFACE = shape(0, 0, 0, 16, 14, 16).forDirectional(Direction.UP);
    public static final VoxelShaper SMART_AIRTIGHT_PIPE = shape(4, 0, 4, 12, 3, 12).add(3, 3, 3, 13, 13, 13).add(4, 13, 4, 12, 16, 12).add(4, 4, 1, 12, 12, 3).forAxis();
    public static final VoxelShaper SMART_AIRTIGHT_PIPE_VERTICAL = shape(4, 0, 4, 12, 3, 12).add(3, 3, 3, 13, 13, 13).add(4, 13, 4, 12, 16, 12).add(4, 4, 1, 12, 12, 3).forDirectional(Direction.NORTH);
    public static final VoxelShaper TESLA_TURBINE = shape(0, 1, 0, 16, 15, 16).forAxis();
    public static final VoxelShaper TESLA_TURBINE_NOZZLE = shape(0, -0.1, -0.1, 7, 16.1, 16.1).add(7, 1, 1, 12, 15, 15).add(12, -0.1, -0.1, 19, 16.1, 16.1).forDirectional(Direction.WEST);
    public static final VoxelShaper TESLA_TURBINE_NOZZLE_VERTICAL = shape(-0.1, 0, -0.1, 16.1, 7, 16.1).add(1, 7, 1, 15, 12, 15).add(-0.1, 12, -0.1, 16.1, 19, 16.1).forDirectional(Direction.DOWN);

    public static final VoxelShape CHAMBER_BLOCK_SHAPE = shape(0, 0, 0, 16, 2, 16).add(1, 2, 1, 15, 15, 15).build();
    public static final VoxelShape CHAMBER_BLOCK_SPECIAL_COLLISION_SHAPE = shape(0, 0, 0, 16, 2, 16).build();
    public static final VoxelShape COOLER_BLOCK_COOLER_SHAPE = shape(1, 0, 1, 15, 12, 15).add(0, 12, 0, 16, 16, 16).build();
    public static final VoxelShape COOLER_BLOCK_SHAPE = shape(1, 0, 1, 15, 14, 15).build();
    public static final VoxelShape COOLER_BLOCK_SPECIAL_COLLISION_SHAPE = shape(0, 0, 0, 16, 4, 16).build();
    public static final VoxelShape CRATE = shape(1, 0, 1, 15, 14, 15).build();
    public static final VoxelShape ENCASED_PIPE_SHAPE = shape(0, 0, 0, 16, 16, 16).build();
    public static final VoxelShape GAS_CANISTER_SHAPE = shape(4, 0, 4, 12, 12, 12).add(6, 12, 6, 10, 13, 10).build();
    public static final VoxelShape GAS_INJECTION_CHAMBER_SHAPE = shape(1, 1, 1, 15, 15, 15).add(2, 15, 2, 14, 16, 14).build();

    public static final VoxelShape AIRTIGHT_REACTOR_KETTLE_TOP_CENTER = shape(0, 2, 0, 16, 16, 16).build();
    public static final VoxelShape AIRTIGHT_REACTOR_KETTLE_BOTTOM_CENTER = shape(0, 0, 0, 16, 2, 16).build();

    public static final VoxelShaper AIRTIGHT_REACTOR_KETTLE_TOP_CORNER = shape(0, 0, 0, 16, 16, 16).remove(2, 0, 2, 16, 2, 16).add(4, 0, 4, 8, 2, 8).add(2, 0, 4, 4, 2, 13).add(4, 0, 2, 13, 2, 4).add(2, 0, 13, 3, 2, 16).add(13, 0, 2, 16, 2, 3).forHorizontal(Direction.NORTH);
    public static final VoxelShaper AIRTIGHT_REACTOR_KETTLE_TOP_MID = shape(0, 0, 0, 16, 16, 16).remove(0, 0, 2, 16, 2, 16).forHorizontal(Direction.NORTH);
    public static final VoxelShaper AIRTIGHT_REACTOR_KETTLE_TOP_MID_CLOSED = shape(0, 0, 0, 16, 16, 16).remove(0, 0, 3, 16, 2, 16).forHorizontal(Direction.NORTH);
    public static final VoxelShaper AIRTIGHT_REACTOR_KETTLE_MID_CORNER = shape(4, 0, 4, 8, 16, 8).add(1, 0, 1, 4, 16, 13).add(1, 0, 13, 4, 1, 16).add(4, 0, 1, 13, 16, 4).add(13, 0, 1, 16, 1, 4).add(13, 1, 2, 16, 16, 3).add(2, 1, 13, 3, 16, 16).forHorizontal(Direction.NORTH);
    public static final VoxelShaper AIRTIGHT_REACTOR_KETTLE_MID_MID = shape(0, 0, 1, 16, 1, 4).forHorizontal(Direction.NORTH);
    public static final VoxelShaper AIRTIGHT_REACTOR_KETTLE_MID_MID_CLOSED = shape(0, 0, 1, 16, 1, 4).add(0, 1, 2, 16, 16, 3).forHorizontal(Direction.NORTH);
    public static final VoxelShaper AIRTIGHT_REACTOR_KETTLE_BOTTOM_MID = shape(0, 0, 0, 16, 16, 16).remove(0, 2, 2, 16, 16, 16).add(0, 15, 2, 16, 16, 4).forHorizontal(Direction.NORTH);
    public static final VoxelShaper AIRTIGHT_REACTOR_KETTLE_BOTTOM_CORNER = shape(0, 0, 0, 16, 16, 16).remove(2, 2, 2, 16, 16, 16).add(2, 2, 2, 3, 15, 14).add(3, 2, 2, 4, 15, 13).add(4, 2, 2, 5, 15, 12).add(5, 2, 2, 6, 15, 11).add(6, 2, 2, 7, 15, 10).add(7, 2, 2, 8, 15, 9).add(8, 2, 2, 9, 15, 8).add(9, 2, 2, 10, 15, 7).add(10, 2, 2, 11, 15, 6).add(11, 2, 2, 12, 15, 5).add(12, 2, 2, 13, 15, 4).add(13, 2, 2, 14, 15, 3).add(4, 15, 4, 8, 16, 8).add(2, 15, 4, 4, 16, 16).add(4, 15, 2, 16, 16, 4).forHorizontal(Direction.NORTH);

    @Contract("_, _, _, _, _, _ -> new")
    private static @NotNull Builder shape(double x1, double y1, double z1, double x2, double y2, double z2) {
        return shape(cuboid(x1, y1, z1, x2, y2, z2));
    }

    @Contract(value = "_ -> new", pure = true)
    private static @NotNull Builder shape(VoxelShape shape) {
        return new Builder(shape);
    }

    private static @NotNull VoxelShape cuboid(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Block.box(x1, y1, z1, x2, y2, z2);
    }

    public static class Builder {
        private VoxelShape shape;

        public Builder(VoxelShape shape) {
            this.shape = shape;
        }

        public Builder add(double x1, double y1, double z1, double x2, double y2, double z2) {
            return add(cuboid(x1, y1, z1, x2, y2, z2));
        }

        public Builder add(VoxelShape shape) {
            this.shape = Shapes.or(this.shape, shape);
            return this;
        }

        public Builder remove(double x1, double y1, double z1, double x2, double y2, double z2) {
            return remove(cuboid(x1, y1, z1, x2, y2, z2));
        }

        public Builder remove(VoxelShape shape) {
            this.shape = Shapes.join(this.shape, shape, BooleanOp.ONLY_FIRST);
            return this;
        }

        public VoxelShape build() {
            return shape;
        }

        public VoxelShaper forAxis() {
            return build(VoxelShaper::forAxis, Axis.Y);
        }

        public VoxelShaper build(@NotNull BiFunction<VoxelShape, Axis, VoxelShaper> factory, Axis axis) {
            return factory.apply(shape, axis);
        }

        public VoxelShaper forDirectional(Direction direction) {
            return build(VoxelShaper::forDirectional, direction);
        }

        public VoxelShaper build(@NotNull BiFunction<VoxelShape, Direction, VoxelShaper> factory, Direction direction) {
            return factory.apply(shape, direction);
        }

        public VoxelShaper forHorizontal(Direction direction) {
            return build(VoxelShaper::forHorizontal, direction);
        }
    }
}
