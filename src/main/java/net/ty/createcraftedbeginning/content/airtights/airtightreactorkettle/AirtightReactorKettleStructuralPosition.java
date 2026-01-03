package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import net.createmod.catnip.lang.Lang;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public enum AirtightReactorKettleStructuralPosition implements StringRepresentable {
    TOP_LEFT_UP(1, -1, 1, false, Direction.UP),
    TOP_MID_UP(0, -1, 1, true, Direction.NORTH),
    TOP_RIGHT_UP(-1, -1, 1, false, Direction.UP),
    TOP_LEFT_MID(1, -1, 0, true, Direction.WEST),
    TOP_CENTER(0, -1, 0, true, Direction.UP),
    TOP_RIGHT_MID(-1, -1, 0, true, Direction.EAST),
    TOP_LEFT_DOWN(1, -1, -1, false, Direction.UP),
    TOP_MID_DOWN(0, -1, -1, true, Direction.SOUTH),
    TOP_RIGHT_DOWN(-1, -1, -1, false, Direction.UP),
    MID_LEFT_UP(1, 0, 1, false, Direction.UP),
    MID_MID_UP(0, 0, 1, false, Direction.NORTH),
    MID_RIGHT_UP(-1, 0, 1, false, Direction.UP),
    MID_LEFT_MID(1, 0, 0, false, Direction.WEST),
    MID_RIGHT_MID(-1, 0, 0, false, Direction.EAST),
    MID_LEFT_DOWN(1, 0, -1, false, Direction.UP),
    MID_MID_DOWN(0, 0, -1, false, Direction.SOUTH),
    MID_RIGHT_DOWN(-1, 0, -1, false, Direction.UP),
    BOTTOM_LEFT_UP(1, 1, 1, false, Direction.UP),
    BOTTOM_MID_UP(0, 1, 1, false, Direction.NORTH),
    BOTTOM_RIGHT_UP(-1, 1, 1, false, Direction.UP),
    BOTTOM_LEFT_MID(1, 1, 0, false, Direction.WEST),
    BOTTOM_CENTER(0, 1, 0, false, Direction.UP),
    BOTTOM_RIGHT_MID(-1, 1, 0, false, Direction.EAST),
    BOTTOM_LEFT_DOWN(1, 1, -1, false, Direction.UP),
    BOTTOM_MID_DOWN(0, 1, -1, false, Direction.SOUTH),
    BOTTOM_RIGHT_DOWN(-1, 1, -1, false, Direction.UP);

    private final boolean isCog;
    private final Direction direction;
    private final int x;
    private final int y;
    private final int z;

    AirtightReactorKettleStructuralPosition(int x, int y, int z, boolean isCog, Direction direction) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.isCog = isCog;
        this.direction = direction;
    }

    public static AirtightReactorKettleStructuralPosition fromOffset(int x, int y, int z) {
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

    public boolean isCog() {
        return isCog;
    }

    public boolean canStore() {
        return y == 1;
    }

    public boolean isWindow(int y) {
        return this.y == y && direction != Direction.UP;
    }

    public boolean isFilter() {
        return y == 1 && direction != Direction.UP;
    }

    public Direction getDirection() {
        return direction;
    }

    @Contract(value = " -> new", pure = true)
    public @NotNull Vec3i getPosition() {
        return new Vec3i(x, y, z);
    }

    @Contract(pure = true)
    @Override
    public @NotNull String getSerializedName() {
        return Lang.asId(name());
    }
}
