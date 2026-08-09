package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.templates;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.config.CCBConfig;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ChainMiningTemplate extends BaseTemplate {
    private static Set<BlockPos> collect(Level level, BlockPos origin, BlockState originState, int requestedLimit) {
        if (originState.isAir()) {
            return Set.of();
        }

        int limit = Math.max(1, requestedLimit);
        Block targetBlock = originState.getBlock();
        LongOpenHashSet inspected = new LongOpenHashSet();
        LongArrayList accepted = new LongArrayList(Math.min(limit, 256));
        LongArrayList currentLayer = new LongArrayList(1);
        LongArrayList nextLayer = new LongArrayList();

        long originKey = origin.asLong();
        inspected.add(originKey);
        accepted.add(originKey);
        currentLayer.add(originKey);
        MutableBlockPos probe = new MutableBlockPos();

        while (!currentLayer.isEmpty() && accepted.size() < limit) {
            nextLayer.clear();
            for (int index = 0; index < currentLayer.size() && accepted.size() < limit; index++) {
                BlockPos current = BlockPos.of(currentLayer.getLong(index));
                for (Direction face : Iterate.directions) {
                    probe.set(current.getX() + face.getStepX(), current.getY() + face.getStepY(), current.getZ() + face.getStepZ());
                    long candidateKey = probe.asLong();
                    if (!inspected.add(candidateKey)) {
                        continue;
                    }

                    int y = probe.getY();
                    if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight() || !level.isLoaded(probe)) {
                        continue;
                    }

                    if (level.getBlockState(probe).getBlock() != targetBlock) {
                        continue;
                    }

                    accepted.add(candidateKey);
                    if (accepted.size() >= limit) {
                        return toPositions(accepted);
                    }

                    nextLayer.add(candidateKey);
                }
            }

            LongArrayList previousLayer = currentLayer;
            currentLayer = nextLayer;
            nextLayer = previousLayer;
        }

        return toPositions(accepted);
    }

    private static Set<BlockPos> toPositions(LongArrayList packedPositions) {
        Set<BlockPos> positions = new LinkedHashSet<>(packedPositions.size());
        for (int index = 0; index < packedPositions.size(); index++) {
            positions.add(BlockPos.of(packedPositions.getLong(index)));
        }
        return positions;
    }

    @Override
    protected Stream<BlockPos> getBaseAreaStream(int @NotNull [] params) {
        return Stream.of(BlockPos.ZERO);
    }

    @Override
    public Set<BlockPos> getTargetPositions(ItemStack drill, BlockPos basePos, Level level, BlockState baseState) {
        int blockLimit = CCBConfig.server().equipments.chainMiningMaxBlocks.get();
        return collect(level, basePos, baseState, blockLimit);
    }

    @Override
    public boolean usesSpatialParameters() {
        return false;
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
