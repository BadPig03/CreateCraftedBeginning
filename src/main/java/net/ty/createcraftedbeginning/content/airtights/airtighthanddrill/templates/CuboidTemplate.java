package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.templates;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.stream.Collectors;

public class CuboidTemplate extends BaseTemplate {
    @Override
    public Set<BlockPos> getBaseArea(int @NotNull [] params) {
        return BlockPos.betweenClosedStream(BlockPos.ZERO, new BlockPos(params[0] - 1, params[1] - 1, params[2] - 1)).map(BlockPos::new).collect(Collectors.toSet());
    }
}