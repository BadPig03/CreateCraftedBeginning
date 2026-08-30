package net.ty.createcraftedbeginning.content.opticalpower.network;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerSource.Source;
import net.ty.createcraftedbeginning.content.opticalpower.opticalfiber.OpticalFiberBlock;
import net.ty.createcraftedbeginning.foundation.CCBMathUtils;
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
public final class OpticalPowerNetwork {
    public static final int MAX_CONSUMER_POWER_POINTS = 16;

    private static final int ORDINARY_LIGHT_POWER_POINTS = 1;
    private static final int DEFAULT_MAX_COHERENT_SOURCES = 8;
    private static final int FULL_LIGHT_LEVEL = 15;
    private static final int MAX_NETWORK_NODES = 4096;
    private static final Comparator<BlockPos> POSITION_ORDER = Comparator.<BlockPos>comparingInt(BlockPos::getX).thenComparingInt(BlockPos::getY).thenComparingInt(BlockPos::getZ);

    private final List<BlockPos> nodes;
    private final List<BlockPos> consumers;
    private final Map<BlockPos, SourceEntry> sources;
    private final Set<BlockPos> sourceDependencies;
    private final boolean scanLimitExceeded;
    private int effectivePowerPoints;

    private OpticalPowerNetwork(List<BlockPos> nodes, List<BlockPos> consumers, Map<BlockPos, SourceEntry> sources, Set<BlockPos> sourceDependencies, boolean scanLimitExceeded) {
        this.nodes = nodes;
        this.consumers = consumers;
        this.sources = sources;
        this.sourceDependencies = sourceDependencies;
        this.scanLimitExceeded = scanLimitExceeded;
        recalculateEffectivePowerPoints();
    }

    public static int getMaxNetworkPowerPoints() {
        return Math.max(1, OpticalPowerUnits.toPowerPoints(CCBConfig.server().opticalPower.maxNetworkPowerSu.get()));
    }

    private static int getMaxCoherentSources() {
        int configuredNetworkLimit = getMaxNetworkPowerPoints();
        int sourcesNeededForConfiguredLimit = (configuredNetworkLimit + MAX_CONSUMER_POWER_POINTS - 1) / MAX_CONSUMER_POWER_POINTS;
        return Math.max(DEFAULT_MAX_COHERENT_SOURCES, sourcesNeededForConfiguredLimit);
    }

    public static OpticalPowerNetwork scan(Level level, BlockPos startConsumer) {
        if (!level.isLoaded(startConsumer) || !isConsumer(level.getBlockState(startConsumer))) {
            return empty();
        }

        Deque<BlockPos> frontier = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> nodes = new ArrayList<>();
        List<BlockPos> consumers = new ArrayList<>();
        Map<BlockPos, SourceEntry> sources = new LinkedHashMap<>();
        Map<BlockPos, SourceEntry> resolvedSourcesByDependency = new HashMap<>();
        Set<BlockPos> sourceDependencies = new LinkedHashSet<>();
        frontier.add(startConsumer.immutable());
        while (!frontier.isEmpty()) {
            BlockPos current = frontier.removeFirst();
            if (!visited.add(current)) {
                continue;
            }

            if (visited.size() > MAX_NETWORK_NODES) {
                consumers.sort(POSITION_ORDER);
                nodes.sort(POSITION_ORDER);
                return new OpticalPowerNetwork(List.copyOf(nodes), List.copyOf(consumers), Map.copyOf(sources), Set.copyOf(sourceDependencies), true);
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

            nodes.add(current.immutable());
            if (currentIsConsumer) {
                consumers.add(current.immutable());
            }

            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.relative(direction);
                if (!level.isLoaded(neighbor)) {
                    continue;
                }

                BlockState neighborState = level.getBlockState(neighbor);
                if (currentIsConsumer) {
                    if (!canConsumerConnect(currentState, direction)) {
                        continue;
                    }
                    if (isFiber(neighborState) && OpticalFiberBlock.isConnected(neighborState, direction.getOpposite())) {
                        frontier.addLast(neighbor.immutable());
                    }
                    continue;
                }

                if (!OpticalFiberBlock.isConnected(currentState, direction)) {
                    continue;
                }

                if (isFiber(neighborState)) {
                    if (OpticalFiberBlock.isConnected(neighborState, direction.getOpposite())) {
                        frontier.addLast(neighbor.immutable());
                    }
                    continue;
                }

                if (isConsumer(neighborState)) {
                    if (canConsumerConnect(neighborState, direction.getOpposite())) {
                        frontier.addLast(neighbor.immutable());
                    }
                    continue;
                }

                SourceEntry sourceEntry = resolvedSourcesByDependency.get(neighbor);
                if (sourceEntry == null) {
                    sourceEntry = resolveSource(level, neighbor, neighborState);
                    if (sourceEntry == null) {
                        continue;
                    }

                    for (BlockPos dependency : sourceEntry.source().dependencies()) {
                        resolvedSourcesByDependency.put(dependency, sourceEntry);
                    }
                }

                Source source = sourceEntry.source();
                SourceEntry previous = sources.get(source.key());
                boolean newlyDiscovered = previous == null;
                if (newlyDiscovered || source.powerPoints() > previous.currentPowerPoints()) {
                    sources.put(source.key(), sourceEntry);
                }
                if (!newlyDiscovered) {
                    continue;
                }

                sourceDependencies.addAll(source.dependencies());
                enqueueFibersTouchingSource(level, source, frontier, visited);
            }
        }

        consumers.sort(POSITION_ORDER);
        nodes.sort(POSITION_ORDER);
        return new OpticalPowerNetwork(List.copyOf(nodes), List.copyOf(consumers), Map.copyOf(sources), Set.copyOf(sourceDependencies), false);
    }

    private static OpticalPowerNetwork empty() {
        return new OpticalPowerNetwork(List.of(), List.of(), Map.of(), Set.of(), false);
    }

    private static @Nullable SourceEntry resolveSource(Level level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof OpticalPowerSource sourceProvider) {
            Source source = sourceProvider.getOpticalPowerSource(level, pos, state);
            return new SourceEntry(pos.immutable(), source, source.powerPoints(), false);
        }

        if (state.getLightEmission(level, pos) < FULL_LIGHT_LEVEL || !state.isCollisionShapeFullBlock(level, pos)) {
            return null;
        }

        Source source = new Source(pos, ORDINARY_LIGHT_POWER_POINTS);
        return new SourceEntry(pos.immutable(), source, source.powerPoints(), true);
    }

    private static void enqueueFibersTouchingSource(Level level, Source source, Deque<BlockPos> frontier, Set<BlockPos> visited) {
        for (BlockPos dependency : source.dependencies()) {
            for (Direction direction : Direction.values()) {
                BlockPos fiberPos = dependency.relative(direction);
                if (visited.contains(fiberPos) || !level.isLoaded(fiberPos)) {
                    continue;
                }

                BlockState fiberState = level.getBlockState(fiberPos);
                if (!isFiber(fiberState) || !OpticalFiberBlock.isConnected(fiberState, direction.getOpposite())) {
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

    private static boolean canConsumerConnect(BlockState state, Direction side) {
        return state.getBlock() instanceof OpticalPowerConsumer consumer && consumer.canConnectOpticalPower(state, side);
    }

    public RefreshResult refreshDynamicSources(Level level) {
        boolean changed = false;
        for (SourceEntry entry : sources.values()) {
            if (!entry.source().dynamic()) {
                continue;
            }

            BlockPos contactPos = entry.contactPos();
            if (!level.isLoaded(contactPos)) {
                return RefreshResult.STALE;
            }

            BlockState state = level.getBlockState(contactPos);
            if (!(state.getBlock() instanceof OpticalPowerSource sourceProvider)) {
                return RefreshResult.STALE;
            }

            int powerPoints = Math.max(0, sourceProvider.getCurrentOpticalPowerPoints(level, contactPos, state, entry.source()));
            if (powerPoints == entry.currentPowerPoints()) {
                continue;
            }

            entry.setCurrentPowerPoints(powerPoints);
            changed = true;
        }

        if (!changed) {
            return RefreshResult.UNCHANGED;
        }

        int previousEffectivePowerPoints = effectivePowerPoints;
        recalculateEffectivePowerPoints();
        if (effectivePowerPoints == previousEffectivePowerPoints) {
            return RefreshResult.UNCHANGED;
        }
        return RefreshResult.CHANGED;
    }

    public void refreshConfiguredLimits() {
        recalculateEffectivePowerPoints();
    }

    public int getAllocatedPowerPoints(BlockPos consumerPos) {
        int consumerIndex = consumers.indexOf(consumerPos);
        if (consumerIndex < 0) {
            return 0;
        }
        return getAllocatedPowerPoints(consumerIndex);
    }

    public int getAllocatedPowerPoints(int consumerIndex) {
        if (scanLimitExceeded || consumers.isEmpty() || consumerIndex < 0 || consumerIndex >= consumers.size()) {
            return 0;
        }

        int distributablePowerPoints = Math.min(Math.min(effectivePowerPoints, getMaxNetworkPowerPoints()), consumers.size() * MAX_CONSUMER_POWER_POINTS);
        int sharedPowerPoints = distributablePowerPoints / consumers.size();
        int remainder = distributablePowerPoints % consumers.size();
        int allocatedPowerPoints = sharedPowerPoints + (consumerIndex < remainder ? 1 : 0);
        return Math.min(allocatedPowerPoints, MAX_CONSUMER_POWER_POINTS);
    }

    public List<BlockPos> getNodes() {
        return nodes;
    }

    public List<BlockPos> getConsumers() {
        return consumers;
    }

    public Set<BlockPos> getSourceDependencies() {
        return sourceDependencies;
    }

    public boolean hasDynamicSources() {
        return sources.values().stream().anyMatch(source -> source.source().dynamic());
    }

    private void recalculateEffectivePowerPoints() {
        if (scanLimitExceeded) {
            effectivePowerPoints = 0;
            return;
        }

        int ordinarySource = sources.values().stream().anyMatch(SourceEntry::ordinary) ? ORDINARY_LIGHT_POWER_POINTS : 0;
        long coherentSources = sources.values().stream().filter(source -> !source.ordinary()).map(SourceEntry::currentPowerPoints).filter(points -> points > 0).sorted(Comparator.reverseOrder()).limit(getMaxCoherentSources()).mapToLong(Integer::longValue).sum();

        effectivePowerPoints = CCBMathUtils.clampToNonNegativeInt(ordinarySource + coherentSources);
    }

    public enum RefreshResult {
        UNCHANGED,
        CHANGED,
        STALE
    }

    private static final class SourceEntry {
        private final BlockPos contactPos;
        private final Source source;
        private final boolean ordinary;
        private int currentPowerPoints;

        private SourceEntry(BlockPos contactPos, Source source, int currentPowerPoints, boolean ordinary) {
            this.contactPos = contactPos;
            this.source = source;
            this.currentPowerPoints = currentPowerPoints;
            this.ordinary = ordinary;
        }

        private BlockPos contactPos() {
            return contactPos;
        }

        private Source source() {
            return source;
        }

        private int currentPowerPoints() {
            return currentPowerPoints;
        }

        private boolean ordinary() {
            return ordinary;
        }

        private void setCurrentPowerPoints(int currentPowerPoints) {
            this.currentPowerPoints = currentPowerPoints;
        }
    }
}
