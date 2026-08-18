package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import net.createmod.catnip.lang.Lang;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum AirtightForgingPressStructuralPosition implements StringRepresentable {
    TOP_LEFT_UP(1, -1, 1, false, Axis.Y, AxisDirection.POSITIVE, Direction.UP),
    TOP_MID_UP(0, -1, 1, true, Axis.Z, AxisDirection.NEGATIVE, Direction.UP),
    TOP_RIGHT_UP(-1, -1, 1, false, Axis.Y, AxisDirection.POSITIVE, Direction.UP),
    TOP_LEFT_MID(1, -1, 0, true, Axis.X, AxisDirection.NEGATIVE, Direction.UP),
    TOP_CENTER(0, -1, 0, true, Axis.Y, AxisDirection.POSITIVE, Direction.UP),
    TOP_RIGHT_MID(-1, -1, 0, true, Axis.X, AxisDirection.POSITIVE, Direction.UP),
    TOP_LEFT_DOWN(1, -1, -1, false, Axis.Y, AxisDirection.POSITIVE, Direction.UP),
    TOP_MID_DOWN(0, -1, -1, true, Axis.Z, AxisDirection.POSITIVE, Direction.UP),
    TOP_RIGHT_DOWN(-1, -1, -1, false, Axis.Y, AxisDirection.POSITIVE, Direction.UP),
    MID_LEFT_UP(1, 0, 1, false, Axis.Y, AxisDirection.POSITIVE, Direction.UP),
    MID_MID_UP(0, 0, 1, false, Axis.Y, AxisDirection.POSITIVE, Direction.UP),
    MID_RIGHT_UP(-1, 0, 1, false, Axis.Y, AxisDirection.POSITIVE, Direction.UP),
    MID_LEFT_MID(1, 0, 0, false, Axis.Y, AxisDirection.POSITIVE, Direction.UP),
    MID_RIGHT_MID(-1, 0, 0, false, Axis.Y, AxisDirection.POSITIVE, Direction.UP),
    MID_LEFT_DOWN(1, 0, -1, false, Axis.Y, AxisDirection.POSITIVE, Direction.UP),
    MID_MID_DOWN(0, 0, -1, false, Axis.Y, AxisDirection.POSITIVE, Direction.UP),
    MID_RIGHT_DOWN(-1, 0, -1, false, Axis.Y, AxisDirection.POSITIVE, Direction.UP),
    BOTTOM_LEFT_UP(1, 1, 1, false, Axis.Y, AxisDirection.POSITIVE, Direction.UP),
    BOTTOM_MID_UP(0, 1, 1, false, Axis.Y, AxisDirection.POSITIVE, Direction.NORTH),
    BOTTOM_RIGHT_UP(-1, 1, 1, false, Axis.Y, AxisDirection.POSITIVE, Direction.UP),
    BOTTOM_LEFT_MID(1, 1, 0, false, Axis.Y, AxisDirection.POSITIVE, Direction.WEST),
    BOTTOM_CENTER(0, 1, 0, false, Axis.Y, AxisDirection.POSITIVE, Direction.UP),
    BOTTOM_RIGHT_MID(-1, 1, 0, false, Axis.Y, AxisDirection.POSITIVE, Direction.EAST),
    BOTTOM_LEFT_DOWN(1, 1, -1, false, Axis.Y, AxisDirection.POSITIVE, Direction.UP),
    BOTTOM_MID_DOWN(0, 1, -1, false, Axis.Y, AxisDirection.POSITIVE, Direction.SOUTH),
    BOTTOM_RIGHT_DOWN(-1, 1, -1, false, Axis.Y, AxisDirection.POSITIVE, Direction.UP);

    private static final List<AirtightForgingPressStructuralPosition> ALL = List.of(values());

    private final Axis axis;
    private final AxisDirection axisDirection;
    private final boolean isShaft;
    private final Direction direction;
    private final BlockPos masterOffset;
    private final BlockPos structureOffset;

    AirtightForgingPressStructuralPosition(int masterOffsetX, int masterOffsetY, int masterOffsetZ, boolean isShaft, Axis axis, AxisDirection axisDirection, Direction direction) {
        masterOffset = new BlockPos(masterOffsetX, masterOffsetY, masterOffsetZ);
        structureOffset = new BlockPos(-masterOffsetX, -masterOffsetY, -masterOffsetZ);
        this.isShaft = isShaft;
        this.axis = axis;
        this.axisDirection = axisDirection;
        this.direction = direction;
    }

    public static List<AirtightForgingPressStructuralPosition> all() {
        return ALL;
    }

    @Contract(pure = true)
    @Override
    public String getSerializedName() {
        return Lang.asId(name());
    }

    public BlockPos getStructureOffset() {
        return structureOffset;
    }

    public boolean isShaft() {
        return isShaft;
    }

    boolean isLowerStore() {
        return masterOffset.getY() == 1;
    }

    boolean isUpperStore() {
        return masterOffset.getY() == -1;
    }

    boolean isFilter() {
        return direction != Direction.UP;
    }

    Direction getDirection() {
        return direction;
    }

    Axis getAxis() {
        return axis;
    }

    AxisDirection getAxisDirection() {
        return axisDirection;
    }

    BlockPos getMasterOffset() {
        return masterOffset;
    }
}
