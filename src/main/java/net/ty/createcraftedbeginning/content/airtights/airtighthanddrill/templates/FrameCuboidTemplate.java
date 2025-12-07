package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.templates;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class FrameCuboidTemplate extends BaseTemplate {
    @Override
    public int[] getDefaultRelativePosition() {
        return new int[]{1, 1, 1};
    }

    @Override
    public Set<BlockPos> getBaseArea(int @NotNull [] params) {
        int sizeX = params[0];
        int sizeY = params[1];
        int sizeZ = params[2];
        BlockPos endPos = new BlockPos(sizeX - 1, sizeY - 1, sizeZ - 1);
        Set<BlockPos> frameCuboid = new HashSet<>();
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.ZERO, endPos)) {
            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();

            boolean isOnXBound = x == 0 || x == sizeX - 1;
            boolean isOnYBound = y == 0 || y == sizeY - 1;
            boolean isOnZBound = z == 0 || z == sizeZ - 1;
            if (isOnXBound && isOnYBound || isOnXBound && isOnZBound || isOnYBound && isOnZBound) {
                continue;
            }

            frameCuboid.add(new BlockPos(x, y, z));
        }
        return frameCuboid;
    }

    @Override
    public int getMinValue(int index) {
        return 3;
    }
}
