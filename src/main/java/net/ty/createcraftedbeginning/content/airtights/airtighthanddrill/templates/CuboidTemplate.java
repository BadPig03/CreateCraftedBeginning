package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.templates;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class CuboidTemplate extends BaseTemplate {
    @Override
    public int getMinValue(int index) {
        return 1;
    }

    @Override
    public int getMaxValue(int index) {
        return 8;
    }

    @Override
    Stream<BlockPos> getBaseAreaStream(int @NotNull [] params) {
        BlockPos endPos = new BlockPos(params[0] - 1, params[1] - 1, params[2] - 1);
        return BlockPos.betweenClosedStream(BlockPos.ZERO, endPos);
    }
}
