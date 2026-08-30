package net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.templates;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.AirtightHandheldDrillUtils;
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

    private static BlockPos applyOffset(BlockPos position, Direction direction, BlockPos relativeOffset) {
        return rotate(position, direction).offset(relativeOffset);
    }

    private static BlockPos getRelativeOffset(Direction direction, int @NotNull [] relativePosition) {
        return rotate(new BlockPos(-relativePosition[0], -relativePosition[1], -relativePosition[2]), direction);
    }

    @Contract("_, _ -> new")
    private static BlockPos rotate(BlockPos position, Direction direction) {
        int x = position.getX();
        int y = position.getY();
        int z = position.getZ();
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
        int[] miningSize = AirtightHandheldDrillUtils.getMiningSizeParams(drill);
        int[] relativePosition = AirtightHandheldDrillUtils.getRelativePositionParams(drill);
        Direction miningDirection = AirtightHandheldDrillUtils.getMiningDirection(drill);
        BlockPos relativeOffset = getRelativeOffset(miningDirection, relativePosition);
        return getBaseAreaStream(miningSize).map(position -> applyOffset(position, miningDirection, relativeOffset)).map(basePos::offset).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public boolean usesSpatialParameters() {
        return true;
    }

    public abstract int getMinValue(int index);

    public abstract int getMaxValue(int index);

    abstract Stream<BlockPos> getBaseAreaStream(int[] miningSize);

    public Set<BlockPos> getOffset(int[] miningSize, Direction direction, int[] relativePosition) {
        BlockPos relativeOffset = getRelativeOffset(direction, relativePosition);
        return getBaseAreaStream(miningSize).map(position -> applyOffset(position, direction, relativeOffset)).collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
