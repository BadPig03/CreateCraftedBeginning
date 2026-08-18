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
public abstract class BaseTemplate {
    BaseTemplate() {
    }

    private static BlockPos applyOffset(BlockPos pos, Direction direction, BlockPos relativeOffset) {
        return rotate(pos, direction).offset(relativeOffset);
    }

    private static BlockPos getRelativeOffset(Direction direction, int @NotNull [] relativeParams) {
        return rotate(new BlockPos(-relativeParams[0], -relativeParams[1], -relativeParams[2]), direction);
    }

    @Contract("_, _ -> new")
    private static BlockPos rotate(BlockPos pos, Direction direction) {
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

    public int[] getDefaultRelativePosition() {
        return new int[]{0, 0, 0};
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

    public abstract int getMinValue(int index);

    public abstract int getMaxValue(int index);

    abstract Stream<BlockPos> getBaseAreaStream(int[] params);

    public Set<BlockPos> getOffset(int[] params, Direction direction, int[] relativeParams) {
        BlockPos relativeOffset = getRelativeOffset(direction, relativeParams);
        return getBaseAreaStream(params).map(pos -> applyOffset(pos, direction, relativeOffset)).collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
