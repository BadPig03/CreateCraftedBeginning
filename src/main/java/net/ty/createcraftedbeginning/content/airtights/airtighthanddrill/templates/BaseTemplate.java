package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.templates;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.AirtightHandheldDrillUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public abstract class BaseTemplate {
    public Set<BlockPos> getBaseArea(int[] params) {
        return getBaseAreaStream(params).map(BlockPos::new).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    protected abstract Stream<BlockPos> getBaseAreaStream(int[] params);

    public int[] getDefaultRelativePosition() {
        return new int[]{0, 0, 0};
    }

    public Set<BlockPos> getFinalOffset(ItemStack drill) {
        int[] params = AirtightHandheldDrillUtils.getMiningSizeParams(drill);
        int[] relativePosition = AirtightHandheldDrillUtils.getRelativePositionParams(drill);
        Direction direction = AirtightHandheldDrillUtils.getMiningDirection(drill);
        return getOffset(params, direction, relativePosition);
    }

    public Set<BlockPos> getTargetPositions(ItemStack drill, BlockPos basePos, Level level) {
        return getTargetPositions(drill, basePos, level, level.getBlockState(basePos));
    }

    public Set<BlockPos> getTargetPositions(ItemStack drill, BlockPos basePos, Level level, BlockState baseState) {
        int[] params = AirtightHandheldDrillUtils.getMiningSizeParams(drill);
        int[] relativePosition = AirtightHandheldDrillUtils.getRelativePositionParams(drill);
        Direction direction = AirtightHandheldDrillUtils.getMiningDirection(drill);
        BlockPos relativeOffset = getRelativeOffset(direction, relativePosition);
        return getBaseAreaStream(params).map(pos -> applyOffset(pos, direction, relativeOffset)).map(basePos::offset).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public boolean usesSpatialParameters() {
        return true;
    }

    public int getMinValue(int index) {
        return 1;
    }

    public int getMaxValue(int index) {
        return 8;
    }

    public Set<BlockPos> getOffset(int[] params, Direction direction, int[] relativeParams) {
        BlockPos relativeOffset = getRelativeOffset(direction, relativeParams);
        return getBaseAreaStream(params).map(pos -> applyOffset(pos, direction, relativeOffset)).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private BlockPos applyOffset(BlockPos pos, Direction direction, BlockPos relativeOffset) {
        return rotate(pos, direction).offset(relativeOffset);
    }

    private BlockPos getRelativeOffset(Direction direction, int @NotNull [] relativeParams) {
        return rotate(new BlockPos(-relativeParams[0], -relativeParams[1], -relativeParams[2]), direction);
    }

    @Contract("_, _ -> new")
    protected BlockPos rotate(BlockPos pos, Direction direction) {
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
}
