package net.ty.createcraftedbeginning.content.opticalpower.network;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerNetwork.SourceEntry;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerSource.Source;
import net.ty.createcraftedbeginning.content.opticalpower.opticalfiber.OpticalFiberBlock;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class OpticalPowerNetworkScanner {
    private static final int MAX_VISITED_POSITIONS = 4096;
    private static final Comparator<BlockPos> POSITION_ORDER = Comparator.<BlockPos>comparingInt(BlockPos::getX).thenComparingInt(BlockPos::getY).thenComparingInt(BlockPos::getZ);

    private OpticalPowerNetworkScanner() {
    }

    static OpticalPowerNetwork scanNetwork(Level level, BlockPos startConsumer) {
        if (!level.isLoaded(startConsumer) || !isConsumer(level.getBlockState(startConsumer))) {
            return new OpticalPowerNetwork(List.of(), List.of(), Map.of(), Set.of(), false);
        }

        Deque<BlockPos> pendingPositions = new ArrayDeque<>();
        Set<BlockPos> visitedPositions = new HashSet<>();
        List<BlockPos> nodes = new ArrayList<>();
        List<BlockPos> consumers = new ArrayList<>();
        Map<BlockPos, SourceEntry> sourcesByKey = new LinkedHashMap<>();
        Map<BlockPos, SourceEntry> sourceEntriesByDependency = new HashMap<>();
        Set<BlockPos> sourceDependencies = new LinkedHashSet<>();
        boolean scanLimitExceeded = false;
        pendingPositions.add(startConsumer.immutable());
        while (!pendingPositions.isEmpty()) {
            BlockPos current = pendingPositions.removeFirst();
            if (!visitedPositions.add(current)) {
                continue;
            }

            if (visitedPositions.size() > MAX_VISITED_POSITIONS) {
                scanLimitExceeded = true;
                break;
            }

            if (!level.isLoaded(current)) {
                continue;
            }

            BlockState currentState = level.getBlockState(current);
            boolean currentIsFiber = isFiber(currentState);
            boolean currentIsConsumer = isConsumer(currentState);
            if (!currentIsFiber && !currentIsConsumer) {
                continue;
            }

            BlockPos node = current.immutable();
            nodes.add(node);
            if (currentIsConsumer) {
                consumers.add(node);
            }

            for (Direction direction : Iterate.directions) {
                if (currentIsConsumer && !canConsumerConnectOnSide(currentState, direction)) {
                    continue;
                }

                if (!currentIsConsumer && !OpticalFiberBlock.isConnected(currentState, direction)) {
                    continue;
                }

                BlockPos neighbor = current.relative(direction);
                if (!level.isLoaded(neighbor)) {
                    continue;
                }

                BlockState neighborState = level.getBlockState(neighbor);
                if (currentIsConsumer) {
                    if (OpticalFiberBlock.isConnected(neighborState, direction.getOpposite())) {
                        pendingPositions.addLast(neighbor.immutable());
                    }
                    continue;
                }

                if (isFiber(neighborState)) {
                    if (OpticalFiberBlock.isConnected(neighborState, direction.getOpposite())) {
                        pendingPositions.addLast(neighbor.immutable());
                    }
                    continue;
                }

                if (isConsumer(neighborState)) {
                    if (canConsumerConnectOnSide(neighborState, direction.getOpposite())) {
                        pendingPositions.addLast(neighbor.immutable());
                    }
                    continue;
                }

                if (!OpticalFiberBlock.canConnectTo(level, neighbor, neighborState, direction.getOpposite())) {
                    continue;
                }

                SourceEntry sourceEntry = sourceEntriesByDependency.get(neighbor);
                boolean isNewlyResolved = sourceEntry == null;
                if (isNewlyResolved) {
                    sourceEntry = resolveSourceEntry(level, neighbor, neighborState);
                }

                Source source = sourceEntry.discoveredSource();
                List<BlockPos> dependencies = source.dependencies();
                if (isNewlyResolved) {
                    for (BlockPos dependency : dependencies) {
                        sourceEntriesByDependency.put(dependency, sourceEntry);
                    }
                }

                sourceDependencies.addAll(dependencies);
                if (!source.topologyValid()) {
                    continue;
                }

                BlockPos sourceKey = source.key();
                SourceEntry previous = sourcesByKey.get(sourceKey);
                boolean isNewLogicalSource = previous == null;
                if (isNewLogicalSource || source.fluxLm() > previous.currentFluxLm()) {
                    sourcesByKey.put(sourceKey, sourceEntry);
                }
                if (!isNewLogicalSource) {
                    continue;
                }

                enqueueFibersConnectedToSource(level, source, pendingPositions, visitedPositions);
            }
        }

        consumers.sort(POSITION_ORDER);
        nodes.sort(POSITION_ORDER);
        return new OpticalPowerNetwork(List.copyOf(nodes), List.copyOf(consumers), Map.copyOf(sourcesByKey), Set.copyOf(sourceDependencies), scanLimitExceeded);
    }

    private static SourceEntry resolveSourceEntry(Level level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof OpticalPowerSource sourceProvider) {
            Source source = sourceProvider.resolveOpticalPowerSource(level, pos, state);
            return new SourceEntry(pos.immutable(), source, source.fluxLm(), false);
        }

        Source source = new Source(pos, OpticalPowerNetwork.ORDINARY_LIGHT_FLUX_LM);
        return new SourceEntry(pos.immutable(), source, source.fluxLm(), true);
    }

    private static void enqueueFibersConnectedToSource(Level level, Source source, Deque<BlockPos> frontier, Set<BlockPos> visited) {
        for (BlockPos dependency : source.dependencies()) {
            if (!level.isLoaded(dependency)) {
                continue;
            }

            BlockState sourceState = level.getBlockState(dependency);
            for (Direction direction : Iterate.directions) {
                if (!OpticalFiberBlock.canConnectTo(level, dependency, sourceState, direction)) {
                    continue;
                }

                BlockPos fiberPos = dependency.relative(direction);
                if (visited.contains(fiberPos) || !level.isLoaded(fiberPos)) {
                    continue;
                }

                BlockState fiberState = level.getBlockState(fiberPos);
                if (!OpticalFiberBlock.isConnected(fiberState, direction.getOpposite())) {
                    continue;
                }

                frontier.addLast(fiberPos.immutable());
            }
        }
    }

    private static boolean isFiber(BlockState state) {
        return state.getBlock() instanceof OpticalFiberBlock;
    }

    private static boolean isConsumer(BlockState state) {
        return state.getBlock() instanceof OpticalPowerConsumer;
    }

    private static boolean canConsumerConnectOnSide(BlockState state, Direction side) {
        return state.getBlock() instanceof OpticalPowerConsumer consumer && consumer.canConnectOpticalPower(state, side);
    }
}
