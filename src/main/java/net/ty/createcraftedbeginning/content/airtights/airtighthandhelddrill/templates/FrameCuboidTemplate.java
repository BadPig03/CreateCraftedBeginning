package net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.templates;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class FrameCuboidTemplate extends BaseTemplate {
    @Override
    public int[] getDefaultRelativePosition() {
        return new int[]{1, 1, 1};
    }

    @Override
    public int getMinValue(int index) {
        return 3;
    }

    @Override
    public int getMaxValue(int index) {
        return 8;
    }

    @Override
    Stream<BlockPos> getBaseAreaStream(int @NotNull [] miningSize) {
        int sizeX = miningSize[0];
        int sizeY = miningSize[1];
        int sizeZ = miningSize[2];
        BlockPos maxPos = new BlockPos(sizeX - 1, sizeY - 1, sizeZ - 1);
        return BlockPos.betweenClosedStream(BlockPos.ZERO, maxPos).filter(blockPos -> {
            int x = blockPos.getX();
            int y = blockPos.getY();
            int z = blockPos.getZ();
            boolean isOnXBound = x == 0 || x == sizeX - 1;
            boolean isOnYBound = y == 0 || y == sizeY - 1;
            boolean isOnZBound = z == 0 || z == sizeZ - 1;
            return !(isOnXBound && isOnYBound || isOnXBound && isOnZBound || isOnYBound && isOnZBound);
        });
    }
}
