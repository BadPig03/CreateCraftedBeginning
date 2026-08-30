package net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.templates;

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
class ChainMiningTemplate extends BaseTemplate {
    private static Set<BlockPos> collect(Level level, BlockPos origin, BlockState originState, int requestedLimit) {
        if (originState.isAir()) {
            return Set.of();
        }

        int blockLimit = Math.max(1, requestedLimit);
        Block targetBlock = originState.getBlock();
        LongOpenHashSet inspectedPositions = new LongOpenHashSet();
        LongArrayList acceptedPositions = new LongArrayList(Math.min(blockLimit, 256));
        LongArrayList currentLayer = new LongArrayList(1);
        LongArrayList nextLayer = new LongArrayList();

        long originPosition = origin.asLong();
        inspectedPositions.add(originPosition);
        acceptedPositions.add(originPosition);
        currentLayer.add(originPosition);
        MutableBlockPos neighborPos = new MutableBlockPos();

        while (!currentLayer.isEmpty() && acceptedPositions.size() < blockLimit) {
            nextLayer.clear();
            for (int layerIndex = 0; layerIndex < currentLayer.size() && acceptedPositions.size() < blockLimit; layerIndex++) {
                BlockPos currentPos = BlockPos.of(currentLayer.getLong(layerIndex));
                for (Direction direction : Iterate.directions) {
                    neighborPos.set(currentPos.getX() + direction.getStepX(), currentPos.getY() + direction.getStepY(), currentPos.getZ() + direction.getStepZ());
                    long neighborPosition = neighborPos.asLong();
                    if (!inspectedPositions.add(neighborPosition)) {
                        continue;
                    }

                    int neighborY = neighborPos.getY();
                    if (neighborY < level.getMinBuildHeight() || neighborY >= level.getMaxBuildHeight() || !level.isLoaded(neighborPos)) {
                        continue;
                    }

                    if (level.getBlockState(neighborPos).getBlock() != targetBlock) {
                        continue;
                    }

                    acceptedPositions.add(neighborPosition);
                    if (acceptedPositions.size() >= blockLimit) {
                        return toPositions(acceptedPositions);
                    }

                    nextLayer.add(neighborPosition);
                }
            }

            LongArrayList previousLayer = currentLayer;
            currentLayer = nextLayer;
            nextLayer = previousLayer;
        }

        return toPositions(acceptedPositions);
    }

    private static Set<BlockPos> toPositions(LongArrayList packedPositions) {
        Set<BlockPos> positions = new LinkedHashSet<>(packedPositions.size());
        for (int index = 0; index < packedPositions.size(); index++) {
            positions.add(BlockPos.of(packedPositions.getLong(index)));
        }
        return positions;
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

    @Override
    Stream<BlockPos> getBaseAreaStream(int @NotNull [] miningSize) {
        return Stream.of(BlockPos.ZERO);
    }
}
