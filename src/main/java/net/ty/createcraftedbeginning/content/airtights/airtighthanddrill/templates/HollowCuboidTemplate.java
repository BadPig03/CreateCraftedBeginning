package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.templates;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class HollowCuboidTemplate extends BaseTemplate {
    @Override
    protected Stream<BlockPos> getBaseAreaStream(int @NotNull [] params) {
        int sizeX = params[0];
        int sizeY = params[1];
        int sizeZ = params[2];
        BlockPos endPos = new BlockPos(sizeX - 1, sizeY - 1, sizeZ - 1);
        return BlockPos.betweenClosedStream(BlockPos.ZERO, endPos).filter(pos -> {
            int x = pos.getX();
            int y = pos.getY();
            int z = pos.getZ();
            return x == 0 || x == sizeX - 1 || y == 0 || y == sizeY - 1 || z == 0 || z == sizeZ - 1;
        });
    }

    @Override
    public int getMinValue(int index) {
        return 1;
    }

    @Override
    public int getMaxValue(int index) {
        return 8;
    }
}
