package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.templates;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.AirtightHandheldDrillUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.stream.Collectors;

public abstract class BaseTemplate {
    public abstract Set<BlockPos> getBaseArea(int[] params);

    public int[] getDefaultRelativePosition() {
        return new int[]{0, 0, 0};
    }

    public Set<BlockPos> getFinalOffset(ItemStack drill) {
        int[] params = AirtightHandheldDrillUtils.getMiningSizeParams(drill);
        int[] relativePosition = AirtightHandheldDrillUtils.getRelativePositionParams(drill);
        Direction direction = AirtightHandheldDrillUtils.getMiningDirection(drill);
        return getOffset(params, direction, relativePosition);
    }

    public int getMinValue(int index) {
        return 1;
    }

    @SuppressWarnings("unused")
    public int getMaxValue(int index) {
        return 8;
    }

    public Set<BlockPos> getOffset(int[] params, Direction direction, int[] relativeParams) {
        return getRotatedArea(params, direction).stream().map(pos -> offset(pos, direction, relativeParams)).collect(Collectors.toSet());
    }

    protected Set<BlockPos> getRotatedArea(int[] params, Direction direction) {
        return getBaseArea(params).stream().map(pos -> rotate(pos, direction)).collect(Collectors.toSet());
    }

    @Contract("_, _ -> new")
    protected @NotNull BlockPos rotate(@NotNull BlockPos pos, @NotNull Direction direction) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        int newX = x;
        int newY = y;
        int newZ = z;

        switch (direction) {
            case SOUTH:
                break;
            case NORTH:
                newX = -x;
                newZ = -z;
                break;
            case EAST:
                newX = z;
                newZ = -x;
                break;
            case WEST:
                newX = -z;
                newZ = x;
                break;
            case UP:
                newY = z;
                newZ = -y;
                break;
            case DOWN:
                newY = -z;
                newZ = y;
                break;
        }
        return new BlockPos(newX, newY, newZ);
    }

    @Contract("_, _, _ -> new")
    protected @NotNull BlockPos offset(@NotNull BlockPos pos, Direction direction, int @NotNull [] relativeParams) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        return new BlockPos(x, y, z).offset(rotate(new BlockPos(-relativeParams[0], -relativeParams[1], -relativeParams[2]), direction));
    }
}