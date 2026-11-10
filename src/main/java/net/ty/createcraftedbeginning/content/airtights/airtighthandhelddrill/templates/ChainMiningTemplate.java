package net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.templates;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongComparator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags.Blocks;
import net.ty.createcraftedbeginning.config.CCBConfig;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class ChainMiningTemplate extends BaseTemplate {
    private static final String ORE_TYPE_PREFIX = "ores/";
    private static final List<BlockPos> FACE_NEIGHBORS;
    private static final List<BlockPos> ORE_NEIGHBORS;

    static {
        List<BlockPos> faceNeighbors = new ArrayList<>(6);
        for (Direction direction : Iterate.directions) {
            faceNeighbors.add(new BlockPos(direction.getStepX(), direction.getStepY(), direction.getStepZ()));
        }
        FACE_NEIGHBORS = List.copyOf(faceNeighbors);
        List<BlockPos> oreNeighbors = new ArrayList<>(faceNeighbors);
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (Math.abs(x) + Math.abs(y) + Math.abs(z) <= 1) {
                        continue;
                    }

                    oreNeighbors.add(new BlockPos(x, y, z));
                }
            }
        }
        ORE_NEIGHBORS = List.copyOf(oreNeighbors);
    }

    private static Set<BlockPos> collect(Level level, BlockPos origin, BlockState originState, int requestedLimit, BiPredicate<BlockPos, BlockState> canTraverse) {
        if (originState.isAir()) {
            return Set.of();
        }

        int blockLimit = Math.max(1, requestedLimit);
        Block targetBlock = originState.getBlock();
        int oreTypePrefixLength = ORE_TYPE_PREFIX.length();
        List<TagKey<Block>> oreTypes = originState.getTags().filter(tag -> {
            ResourceLocation location = tag.location();
            String path = location.getPath();
            return "c".equals(location.getNamespace()) && path.startsWith(ORE_TYPE_PREFIX) && path.length() > oreTypePrefixLength && path.indexOf('/', oreTypePrefixLength) < 0;
        }).toList();
        List<BlockPos> neighbors = FACE_NEIGHBORS;
        if (!oreTypes.isEmpty() || originState.is(Blocks.ORES)) {
            neighbors = ORE_NEIGHBORS;
        }
        LongOpenHashSet inspectedPositions = new LongOpenHashSet();
        LongArrayList acceptedPositions = new LongArrayList(Math.min(blockLimit, 256));
        LongArrayList currentLayer = new LongArrayList(1);
        LongArrayList nextLayer = new LongArrayList();

        long originPosition = origin.asLong();
        inspectedPositions.add(originPosition);
        acceptedPositions.add(originPosition);
        if (!canTraverse.test(origin, originState)) {
            return toPositions(acceptedPositions);
        }

        currentLayer.add(originPosition);
        MutableBlockPos neighborPos = new MutableBlockPos();
        int originX = origin.getX();
        int originY = origin.getY();
        int originZ = origin.getZ();
        int minBuildHeight = level.getMinBuildHeight();
        int maxBuildHeight = level.getMaxBuildHeight();
        LongComparator layerOrder = (firstPosition, secondPosition) -> {
            int firstX = BlockPos.getX(firstPosition);
            int firstY = BlockPos.getY(firstPosition);
            int firstZ = BlockPos.getZ(firstPosition);
            int secondX = BlockPos.getX(secondPosition);
            int secondY = BlockPos.getY(secondPosition);
            int secondZ = BlockPos.getZ(secondPosition);
            long firstOffsetX = (long) firstX - originX;
            long firstOffsetY = (long) firstY - originY;
            long firstOffsetZ = (long) firstZ - originZ;
            long secondOffsetX = (long) secondX - originX;
            long secondOffsetY = (long) secondY - originY;
            long secondOffsetZ = (long) secondZ - originZ;
            long firstDistanceSquared = firstOffsetX * firstOffsetX + firstOffsetY * firstOffsetY + firstOffsetZ * firstOffsetZ;
            long secondDistanceSquared = secondOffsetX * secondOffsetX + secondOffsetY * secondOffsetY + secondOffsetZ * secondOffsetZ;
            int distanceOrder = Long.compare(firstDistanceSquared, secondDistanceSquared);
            if (distanceOrder != 0) {
                return distanceOrder;
            }

            int xOrder = Integer.compare(firstX, secondX);
            if (xOrder != 0) {
                return xOrder;
            }

            int yOrder = Integer.compare(firstY, secondY);
            if (yOrder != 0) {
                return yOrder;
            }

            return Integer.compare(firstZ, secondZ);
        };

        while (!currentLayer.isEmpty() && acceptedPositions.size() < blockLimit) {
            nextLayer.clear();
            int currentLayerSize = currentLayer.size();
            for (int layerIndex = 0; layerIndex < currentLayerSize; layerIndex++) {
                long currentPosition = currentLayer.getLong(layerIndex);
                int currentX = BlockPos.getX(currentPosition);
                int currentY = BlockPos.getY(currentPosition);
                int currentZ = BlockPos.getZ(currentPosition);
                for (BlockPos offset : neighbors) {
                    neighborPos.set(currentX + offset.getX(), currentY + offset.getY(), currentZ + offset.getZ());
                    long neighborPosition = neighborPos.asLong();
                    if (!inspectedPositions.add(neighborPosition)) {
                        continue;
                    }

                    int neighborY = neighborPos.getY();
                    if (neighborY < minBuildHeight || neighborY >= maxBuildHeight || !level.isLoaded(neighborPos)) {
                        continue;
                    }

                    BlockState neighborState = level.getBlockState(neighborPos);
                    if (neighborState.getBlock() != targetBlock && oreTypes.stream().noneMatch(neighborState::is)) {
                        continue;
                    }

                    if (!canTraverse.test(neighborPos, neighborState)) {
                        continue;
                    }

                    nextLayer.add(neighborPosition);
                }
            }

            nextLayer.sort(layerOrder);
            int selectedCount = Math.min(blockLimit - acceptedPositions.size(), nextLayer.size());
            for (int index = 0; index < selectedCount; index++) {
                acceptedPositions.add(nextLayer.getLong(index));
            }
            if (acceptedPositions.size() >= blockLimit) {
                return toPositions(acceptedPositions);
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
    public Set<BlockPos> getTargetPositions(ItemStack drill, BlockPos basePos, Level level, BlockState baseState, Direction miningDirection, BiPredicate<BlockPos, BlockState> canTraverse) {
        int blockLimit = CCBConfig.server().equipment.airtightHandheldDrill.chainMiningMaxBlocks.get();
        return collect(level, basePos, baseState, blockLimit, canTraverse);
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
