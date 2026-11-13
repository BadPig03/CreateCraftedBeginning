package net.ty.createcraftedbeginning.content.opticalpower.network;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerNetwork.DynamicSourceFailure;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerNetwork.SourceEntry;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerSource.Source;
import net.ty.createcraftedbeginning.content.opticalpower.opticalfiber.OpticalFiberBlock;
import org.jetbrains.annotations.Nullable;

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
    static final int RETRY_DELAY_TICKS = 20;
    private static final int MAX_VISITED_POSITIONS = 4096;
    private static final Comparator<BlockPos> POSITION_ORDER = Comparator.<BlockPos>comparingInt(BlockPos::getX).thenComparingInt(BlockPos::getY).thenComparingInt(BlockPos::getZ);

    private final Map<BlockPos, SourceRetry> sourcesPendingRetry = new HashMap<>();

    private static SourceEntry resolveSourceEntry(Level level, BlockPos pos, BlockState state, boolean verifyDynamic) {
        if (state.getBlock() instanceof OpticalPowerSource sourceProvider) {
            Source source = sourceProvider.resolveOpticalPowerSource(level, pos, state);
            double fluxLm = source.fluxLm();
            if (verifyDynamic && source.dynamic() && source.topologyValid()) {
                fluxLm = OpticalPowerNetwork.readDynamicFlux(level, pos, state, sourceProvider, source);
            }
            return new SourceEntry(pos.immutable(), source, fluxLm, false);
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

    ScanResult scanNetwork(Level level, BlockPos startConsumer) {
        if (!level.isLoaded(startConsumer) || !isConsumer(level.getBlockState(startConsumer))) {
            return new ScanResult(new OpticalPowerNetwork(List.of(), List.of(), Map.of(), Set.of(), false), List.of(), Long.MIN_VALUE);
        }

        long now = level.getGameTime();
        long retryAt = Long.MIN_VALUE;
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
                    SourceRetry retry = sourcesPendingRetry.get(neighbor);
                    Block provider = neighborState.getBlock();
                    if (retry != null && retry.provider != provider) {
                        sourcesPendingRetry.remove(neighbor);
                        retry = null;
                    }
                    if (retry != null && now < retry.tick) {
                        retryAt = Math.max(retryAt, retry.tick);
                        continue;
                    }

                    try {
                        sourceEntry = resolveSourceEntry(level, neighbor, neighborState, retry != null && retry.verifyDynamic);
                    }
                    catch (DynamicSourceFailure failure) {
                        retryAt = Math.max(retryAt, deferDynamicSource(level, failure));
                        continue;
                    }
                    catch (RuntimeException failure) {
                        retryAt = Math.max(retryAt, deferSource(level, neighbor, provider, null, failure));
                        continue;
                    }

                    if (retry != null) {
                        sourcesPendingRetry.remove(neighbor, retry);
                        for (BlockPos dependency : sourceEntry.discoveredSource().dependencies()) {
                            sourcesPendingRetry.remove(dependency, retry);
                        }
                    }
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
                if (isNewLogicalSource || sourceEntry.currentFluxLm() > previous.currentFluxLm()) {
                    sourcesByKey.put(sourceKey, sourceEntry);
                }
                if (!isNewLogicalSource) {
                    continue;
                }

                enqueueFibersConnectedToSource(level, source, pendingPositions, visitedPositions);
            }
        }

        if (retryAt != Long.MIN_VALUE) {
            return new ScanResult(null, List.copyOf(consumers), retryAt);
        }

        consumers.sort(POSITION_ORDER);
        nodes.sort(POSITION_ORDER);
        List<BlockPos> members = List.copyOf(consumers);
        OpticalPowerNetwork network = new OpticalPowerNetwork(List.copyOf(nodes), members, Map.copyOf(sourcesByKey), Set.copyOf(sourceDependencies), scanLimitExceeded);
        return new ScanResult(network, members, Long.MIN_VALUE);
    }

    long deferDynamicSource(Level level, DynamicSourceFailure failure) {
        return deferSource(level, failure.contact, failure.provider, failure.source, failure);
    }

    void invalidateSource(Level level, BlockPos pos) {
        SourceRetry retry = sourcesPendingRetry.get(pos);
        if (retry == null || !level.isLoaded(pos) || level.getBlockState(pos).getBlock() == retry.provider) {
            return;
        }

        sourcesPendingRetry.remove(pos);
    }

    private long deferSource(Level level, BlockPos pos, Block provider, @Nullable Source source, RuntimeException failure) {
        SourceRetry previous = sourcesPendingRetry.get(pos);
        long tick = level.getGameTime() + RETRY_DELAY_TICKS;
        SourceRetry retry = previous;
        if (retry == null || retry.provider != provider) {
            CCBAPI.LOGGER.warn("Failed to read the optical source at {} in dimension '{}'; retrying its network after {} ticks.", pos, level.dimension().location(), RETRY_DELAY_TICKS, failure);
            retry = new SourceRetry(provider);
        }
        retry.tick = tick;
        retry.verifyDynamic |= source != null;
        sourcesPendingRetry.put(pos, retry);
        if (source == null) {
            return tick;
        }

        for (BlockPos dependency : source.dependencies()) {
            sourcesPendingRetry.put(dependency, retry);
        }
        return tick;
    }

    record ScanResult(@Nullable OpticalPowerNetwork network, List<BlockPos> consumers, long retryAt) {}

    private static final class SourceRetry {
        private final Block provider;
        private long tick;
        private boolean verifyDynamic;

        private SourceRetry(Block provider) {
            this.provider = provider;
        }
    }
}
