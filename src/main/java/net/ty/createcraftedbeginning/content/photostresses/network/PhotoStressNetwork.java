package net.ty.createcraftedbeginning.content.photostresses.network;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.photostresses.network.PhotoStressSource.Source;
import net.ty.createcraftedbeginning.content.photostresses.opticalfiber.OpticalFiberBlock;
import net.ty.createcraftedbeginning.content.photostresses.phohostressbearing.PhotoStressBearingBlock;
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
public final class PhotoStressNetwork {
    public static final int MAX_BEARING_STRESS_POINTS = 16;
    private static final int ORDINARY_LIGHT_STRESS_POINTS = 2;

    private static final int FULL_LIGHT_LEVEL = 15;
    private static final int MAX_NETWORK_NODES = 4096;
    private static final int EFFICIENCY_DENOMINATOR = 8;
    private static final int[] COUPLING_EFFICIENCY_EIGHTHS = {0, 8, 7, 6, 5, 4, 3, 2, 1};
    private static final Comparator<BlockPos> POSITION_ORDER = Comparator.<BlockPos>comparingInt(BlockPos::getX).thenComparingInt(BlockPos::getY).thenComparingInt(BlockPos::getZ);

    private final List<BlockPos> nodes;
    private final List<BlockPos> bearings;
    private final Map<BlockPos, SourceEntry> sources;
    private final Set<BlockPos> sourceDependencies;
    private final boolean scanLimitExceeded;
    private int effectiveStressPoints;

    private PhotoStressNetwork(List<BlockPos> nodes, List<BlockPos> bearings, Map<BlockPos, SourceEntry> sources, Set<BlockPos> sourceDependencies, boolean scanLimitExceeded) {
        this.nodes = nodes;
        this.bearings = bearings;
        this.sources = sources;
        this.sourceDependencies = sourceDependencies;
        this.scanLimitExceeded = scanLimitExceeded;
        recalculateEffectiveStressPoints();
    }

    public static PhotoStressNetwork scan(Level level, BlockPos startBearing) {
        if (!level.isLoaded(startBearing) || !isBearing(level.getBlockState(startBearing))) {
            return empty();
        }

        Deque<BlockPos> frontier = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> nodes = new ArrayList<>();
        List<BlockPos> bearings = new ArrayList<>();
        Map<BlockPos, SourceEntry> sources = new LinkedHashMap<>();
        Map<BlockPos, SourceEntry> resolvedSourcesByDependency = new HashMap<>();
        Set<BlockPos> sourceDependencies = new LinkedHashSet<>();
        frontier.add(startBearing.immutable());
        while (!frontier.isEmpty()) {
            BlockPos current = frontier.removeFirst();
            if (!visited.add(current)) {
                continue;
            }

            if (visited.size() > MAX_NETWORK_NODES) {
                bearings.sort(POSITION_ORDER);
                nodes.sort(POSITION_ORDER);
                return new PhotoStressNetwork(List.copyOf(nodes), List.copyOf(bearings), Map.copyOf(sources), Set.copyOf(sourceDependencies), true);
            }

            if (!level.isLoaded(current)) {
                continue;
            }

            BlockState currentState = level.getBlockState(current);
            boolean currentIsFiber = isFiber(currentState);
            boolean currentIsBearing = isBearing(currentState);
            if (!currentIsFiber && !currentIsBearing) {
                continue;
            }

            nodes.add(current.immutable());
            if (currentIsBearing) {
                bearings.add(current.immutable());
            }

            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.relative(direction);
                if (!level.isLoaded(neighbor)) {
                    continue;
                }

                BlockState neighborState = level.getBlockState(neighbor);
                if (currentIsBearing) {
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

                if (isBearing(neighborState)) {
                    frontier.addLast(neighbor.immutable());
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
                if (newlyDiscovered || source.stressPoints() > previous.currentStressPoints()) {
                    sources.put(source.key(), sourceEntry);
                }
                if (!newlyDiscovered) {
                    continue;
                }

                sourceDependencies.addAll(source.dependencies());
                enqueueFibersTouchingSource(level, source, frontier, visited);
            }
        }

        bearings.sort(POSITION_ORDER);
        nodes.sort(POSITION_ORDER);
        return new PhotoStressNetwork(List.copyOf(nodes), List.copyOf(bearings), Map.copyOf(sources), Set.copyOf(sourceDependencies), false);
    }

    private static PhotoStressNetwork empty() {
        return new PhotoStressNetwork(List.of(), List.of(), Map.of(), Set.of(), false);
    }

    private static @Nullable SourceEntry resolveSource(Level level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof PhotoStressSource sourceProvider) {
            Source source = sourceProvider.getPhotoStressSource(level, pos, state);
            return new SourceEntry(pos.immutable(), source, source.stressPoints());
        }

        if (state.getLightEmission(level, pos) < FULL_LIGHT_LEVEL || !state.isCollisionShapeFullBlock(level, pos)) {
            return null;
        }

        Source source = new Source(pos, ORDINARY_LIGHT_STRESS_POINTS);
        return new SourceEntry(pos.immutable(), source, source.stressPoints());
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

    private static boolean isBearing(BlockState state) {
        return state.getBlock() instanceof PhotoStressBearingBlock;
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
            if (!(state.getBlock() instanceof PhotoStressSource sourceProvider)) {
                return RefreshResult.STALE;
            }

            int stressPoints = Math.max(0, sourceProvider.getCurrentPhotoStressPoints(level, contactPos, state, entry.source()));
            if (stressPoints == entry.currentStressPoints()) {
                continue;
            }

            entry.setCurrentStressPoints(stressPoints);
            changed = true;
        }

        if (!changed) {
            return RefreshResult.UNCHANGED;
        }

        int previousEffectiveStressPoints = effectiveStressPoints;
        recalculateEffectiveStressPoints();
        return effectiveStressPoints == previousEffectiveStressPoints ? RefreshResult.UNCHANGED : RefreshResult.CHANGED;
    }

    public int getAllocatedStressPoints(BlockPos bearingPos) {
        int bearingIndex = bearings.indexOf(bearingPos);
        return bearingIndex < 0 ? 0 : getAllocatedStressPoints(bearingIndex);
    }

    public int getAllocatedStressPoints(int bearingIndex) {
        if (scanLimitExceeded || bearings.isEmpty() || bearingIndex < 0 || bearingIndex >= bearings.size()) {
            return 0;
        }

        int distributableStressPoints = Math.min(effectiveStressPoints, bearings.size() * MAX_BEARING_STRESS_POINTS);
        int sharedStressPoints = distributableStressPoints / bearings.size();
        int remainder = distributableStressPoints % bearings.size();
        int allocatedStressPoints = sharedStressPoints + (bearingIndex < remainder ? 1 : 0);
        return Math.min(allocatedStressPoints, MAX_BEARING_STRESS_POINTS);
    }

    public List<BlockPos> getNodes() {
        return nodes;
    }

    public List<BlockPos> getBearings() {
        return bearings;
    }

    public Set<BlockPos> getSourceDependencies() {
        return sourceDependencies;
    }

    public boolean hasDynamicSources() {
        return sources.values().stream().anyMatch(source -> source.source().dynamic());
    }

    private int getSourceCount() {
        return (int) sources.values().stream().filter(source -> source.currentStressPoints() > 0).count();
    }

    private int getBearingCount() {
        return bearings.size();
    }

    private boolean isOverloaded() {
        return scanLimitExceeded || getSourceCount() >= COUPLING_EFFICIENCY_EIGHTHS.length;
    }

    private void recalculateEffectiveStressPoints() {
        if (scanLimitExceeded) {
            effectiveStressPoints = 0;
            return;
        }

        int sourceCount = 0;
        int rawStressPoints = 0;
        for (SourceEntry source : sources.values()) {
            int stressPoints = source.currentStressPoints();
            if (stressPoints <= 0) {
                continue;
            }

            sourceCount++;
            rawStressPoints += stressPoints;
        }

        if (sourceCount == 0 || sourceCount >= COUPLING_EFFICIENCY_EIGHTHS.length) {
            effectiveStressPoints = 0;
            return;
        }

        effectiveStressPoints = rawStressPoints * COUPLING_EFFICIENCY_EIGHTHS[sourceCount] / EFFICIENCY_DENOMINATOR;
    }

    public enum RefreshResult {
        UNCHANGED,
        CHANGED,
        STALE
    }

    private static final class SourceEntry {
        private final BlockPos contactPos;
        private final Source source;
        private int currentStressPoints;

        private SourceEntry(BlockPos contactPos, Source source, int currentStressPoints) {
            this.contactPos = contactPos;
            this.source = source;
            this.currentStressPoints = currentStressPoints;
        }

        private BlockPos contactPos() {
            return contactPos;
        }

        private Source source() {
            return source;
        }

        private int currentStressPoints() {
            return currentStressPoints;
        }

        private void setCurrentStressPoints(int currentStressPoints) {
            this.currentStressPoints = currentStressPoints;
        }
    }
}
