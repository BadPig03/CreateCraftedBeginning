package net.ty.createcraftedbeginning.content.opticalpower.network;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.opticalpower.network.OpticalPowerSource.Source;
import net.ty.createcraftedbeginning.content.opticalpower.opticalfiber.OpticalFiberBlock;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
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
    public static final int MAX_CONSUMER_POWER_LP = 48;

    private static final int ORDINARY_LIGHT_POWER_LP = 1;
    private static final int FULL_LIGHT_LEVEL = 15;
    private static final int MAX_NETWORK_NODES = 4096;
    private static final Comparator<BlockPos> POSITION_ORDER = Comparator.<BlockPos>comparingInt(BlockPos::getX).thenComparingInt(BlockPos::getY).thenComparingInt(BlockPos::getZ);

    private final List<BlockPos> nodes;
    private final List<BlockPos> consumers;
    private final Map<BlockPos, SourceEntry> sources;
    private final Set<BlockPos> sourceDependencies;
    private final boolean scanLimitExceeded;
    private int effectivePowerLp;

    private OpticalPowerNetwork(List<BlockPos> nodes, List<BlockPos> consumers, Map<BlockPos, SourceEntry> sources, Set<BlockPos> sourceDependencies, boolean scanLimitExceeded) {
        this.nodes = nodes;
        this.consumers = consumers;
        this.sources = sources;
        this.sourceDependencies = sourceDependencies;
        this.scanLimitExceeded = scanLimitExceeded;
        recalculateEffectivePowerLp();
    }

    public static int getMaxNetworkPowerLp() {
        return CCBConfig.server().opticalPower.network.maxNetworkPowerLp.get();
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

            for (Direction direction : Iterate.directions) {
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
                sourceDependencies.addAll(source.dependencies());
                if (!source.topologyValid()) {
                    continue;
                }

                SourceEntry previous = sources.get(source.key());
                boolean newlyDiscovered = previous == null;
                if (newlyDiscovered || source.powerLp() > previous.currentPowerLp()) {
                    sources.put(source.key(), sourceEntry);
                }
                if (!newlyDiscovered) {
                    continue;
                }

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
            return new SourceEntry(pos.immutable(), source, source.powerLp(), false);
        }

        if (state.getLightEmission(level, pos) < FULL_LIGHT_LEVEL || !state.isCollisionShapeFullBlock(level, pos)) {
            return null;
        }

        Source source = new Source(pos, ORDINARY_LIGHT_POWER_LP);
        return new SourceEntry(pos.immutable(), source, source.powerLp(), true);
    }

    private static void enqueueFibersTouchingSource(Level level, Source source, Deque<BlockPos> frontier, Set<BlockPos> visited) {
        for (BlockPos dependency : source.dependencies()) {
            for (Direction direction : Iterate.directions) {
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

            int powerLp = Math.max(0, sourceProvider.getCurrentOpticalPowerLp(level, contactPos, state, entry.source()));
            if (powerLp == entry.currentPowerLp()) {
                continue;
            }

            entry.setCurrentPowerLp(powerLp);
            changed = true;
        }

        if (!changed) {
            return RefreshResult.UNCHANGED;
        }

        int previousEffectivePowerLp = effectivePowerLp;
        recalculateEffectivePowerLp();
        if (effectivePowerLp == previousEffectivePowerLp) {
            return RefreshResult.UNCHANGED;
        }

        return RefreshResult.CHANGED;
    }

    public int getAllocatedPowerLp(BlockPos consumerPos) {
        int consumerIndex = consumers.indexOf(consumerPos);
        if (consumerIndex < 0) {
            return 0;
        }

        return getAllocatedPowerLp(consumerIndex);
    }

    public int getAllocatedPowerLp(int consumerIndex) {
        if (scanLimitExceeded || consumers.isEmpty() || consumerIndex < 0 || consumerIndex >= consumers.size()) {
            return 0;
        }

        int distributablePowerLp = Math.min(Math.min(effectivePowerLp, getMaxNetworkPowerLp()), consumers.size() * MAX_CONSUMER_POWER_LP);
        int sharedPowerLp = distributablePowerLp / consumers.size();
        int remainder = distributablePowerLp % consumers.size();
        int allocatedPowerLp = sharedPowerLp + (consumerIndex < remainder ? 1 : 0);
        return Math.min(allocatedPowerLp, MAX_CONSUMER_POWER_LP);
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

    private void recalculateEffectivePowerLp() {
        if (scanLimitExceeded) {
            effectivePowerLp = 0;
            return;
        }

        int ordinarySource = sources.values().stream().anyMatch(SourceEntry::ordinary) ? ORDINARY_LIGHT_POWER_LP : 0;
        long coherentSources = sources.values().stream().filter(source -> !source.ordinary()).map(SourceEntry::currentPowerLp).filter(powerLp -> powerLp > 0).mapToLong(Integer::longValue).sum();

        effectivePowerLp = BoundedMath.clampToNonNegativeInt(ordinarySource + coherentSources);
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
        private int currentPowerLp;

        private SourceEntry(BlockPos contactPos, Source source, int currentPowerLp, boolean ordinary) {
            this.contactPos = contactPos;
            this.source = source;
            this.currentPowerLp = currentPowerLp;
            this.ordinary = ordinary;
        }

        private BlockPos contactPos() {
            return contactPos;
        }

        private Source source() {
            return source;
        }

        private int currentPowerLp() {
            return currentPowerLp;
        }

        private boolean ordinary() {
            return ordinary;
        }

        private void setCurrentPowerLp(int currentPowerLp) {
            this.currentPowerLp = currentPowerLp;
        }
    }
}
