package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.templates;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class HollowCuboidTemplate extends BaseTemplate {
    @Override
    public Set<BlockPos> getBaseArea(int @NotNull [] params) {
        int sizeX = params[0];
        int sizeY = params[1];
        int sizeZ = params[2];
        BlockPos endPos = new BlockPos(sizeX - 1, sizeY - 1, sizeZ - 1);
        Set<BlockPos> hollowCuboid = new HashSet<>();
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.ZERO, endPos)) {
            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();

            boolean isOnSurface = x == 0 || x == sizeX - 1 || y == 0 || y == sizeY - 1 || z == 0 || z == sizeZ - 1;
            if (!isOnSurface) {
                continue;
            }

            hollowCuboid.add(new BlockPos(x, y, z));
        }
        return hollowCuboid;
    }

}
