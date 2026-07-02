package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import net.createmod.catnip.lang.Lang;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.Vec3i;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;

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

    private final Axis axis;
    private final AxisDirection axisDirection;
    private final boolean isShaft;
    private final Direction direction;
    private final int x;
    private final int y;
    private final int z;

    AirtightForgingPressStructuralPosition(int x, int y, int z, boolean isShaft, Axis axis, AxisDirection axisDirection, Direction direction) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.isShaft = isShaft;
        this.axis = axis;
        this.axisDirection = axisDirection;
        this.direction = direction;
    }

    public static AirtightForgingPressStructuralPosition fromOffset(int x, int y, int z) {
        if (x == -1 && y == 1 && z == -1) {
            return TOP_LEFT_UP;
        }
        else if (x == 0 && y == 1 && z == -1) {
            return TOP_MID_UP;
        }
        else if (x == 1 && y == 1 && z == -1) {
            return TOP_RIGHT_UP;
        }
        else if (x == -1 && y == 1 && z == 0) {
            return TOP_LEFT_MID;
        }
        else if (x == 1 && y == 1 && z == 0) {
            return TOP_RIGHT_MID;
        }
        else if (x == -1 && y == 1 && z == 1) {
            return TOP_LEFT_DOWN;
        }
        else if (x == 0 && y == 1 && z == 1) {
            return TOP_MID_DOWN;
        }
        else if (x == 1 && y == 1 && z == 1) {
            return TOP_RIGHT_DOWN;
        }
        else if (x == -1 && y == 0 && z == -1) {
            return MID_LEFT_UP;
        }
        else if (x == 0 && y == 0 && z == -1) {
            return MID_MID_UP;
        }
        else if (x == 1 && y == 0 && z == -1) {
            return MID_RIGHT_UP;
        }
        else if (x == -1 && y == 0 && z == 0) {
            return MID_LEFT_MID;
        }
        else if (x == 1 && y == 0 && z == 0) {
            return MID_RIGHT_MID;
        }
        else if (x == -1 && y == 0 && z == 1) {
            return MID_LEFT_DOWN;
        }
        else if (x == 0 && y == 0 && z == 1) {
            return MID_MID_DOWN;
        }
        else if (x == 1 && y == 0 && z == 1) {
            return MID_RIGHT_DOWN;
        }
        if (x == -1 && y == -1 && z == -1) {
            return BOTTOM_LEFT_UP;
        }
        else if (x == 0 && y == -1 && z == -1) {
            return BOTTOM_MID_UP;
        }
        else if (x == 1 && y == -1 && z == -1) {
            return BOTTOM_RIGHT_UP;
        }
        else if (x == -1 && y == -1 && z == 0) {
            return BOTTOM_LEFT_MID;
        }
        else if (x == 0 && y == -1 && z == 0){
            return BOTTOM_CENTER;
        }
        else if (x == 1 && y == -1 && z == 0) {
            return BOTTOM_RIGHT_MID;
        }
        else if (x == -1 && y == -1 && z == 1) {
            return BOTTOM_LEFT_DOWN;
        }
        else if (x == 0 && y == -1 && z == 1) {
            return BOTTOM_MID_DOWN;
        }
        else if (x == 1 && y == -1 && z == 1) {
            return BOTTOM_RIGHT_DOWN;
        }
        else {
            return TOP_CENTER;
        }
    }

    public boolean isShaft() {
        return isShaft;
    }

    public boolean isLowerStore() {
        return y == 1;
    }

    public boolean isUpperStore() {
        return y == -1;
    }

    public boolean isFilter() {
        return direction != Direction.UP;
    }

    public Direction getDirection() {
        return direction;
    }

    public Axis getAxis() {
        return axis;
    }

    public AxisDirection getAxisDirection() {
        return axisDirection;
    }

    @Contract(value = " -> new", pure = true)
    public Vec3i getPosition() {
        return new Vec3i(x, y, z);
    }

    @Contract(pure = true)
    @Override
    public String getSerializedName() {
        return Lang.asId(name());
    }
}
