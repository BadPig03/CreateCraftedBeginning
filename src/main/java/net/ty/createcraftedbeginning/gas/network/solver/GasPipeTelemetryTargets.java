package net.ty.createcraftedbeginning.gas.network.solver;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology.Snapshot;
import net.ty.createcraftedbeginning.gas.telemetry.GasFlowTelemetryTarget;
import net.ty.createcraftedbeginning.gas.telemetry.GasPressureTelemetryTarget;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Internal
public final class GasPipeTelemetryTargets {
    private static final Object CACHE_KEY = new Object();

    private final List<BlockPos> pressureTargetPositions;
    private final Map<BlockPos, Integer> pressureTargetIndicesByPosition;
    private final Set<BlockPos> flowTargetPositions;

    private GasPipeTelemetryTargets(List<BlockPos> pressureTargetPositions, Map<BlockPos, Integer> pressureTargetIndicesByPosition, Set<BlockPos> flowTargetPositions) {
        this.pressureTargetPositions = pressureTargetPositions;
        this.pressureTargetIndicesByPosition = pressureTargetIndicesByPosition;
        this.flowTargetPositions = flowTargetPositions;
    }

    public static GasPipeTelemetryTargets get(Level level, Snapshot topology) {
        return topology.cachedArtifact(CACHE_KEY, () -> discover(level, topology));
    }

    List<BlockPos> pressureTargetPositions() {
        return pressureTargetPositions;
    }

    Map<BlockPos, Integer> pressureTargetIndicesByPosition() {
        return pressureTargetIndicesByPosition;
    }

    Set<BlockPos> flowTargetPositions() {
        return flowTargetPositions;
    }

    int pressureTargetCount() {
        return pressureTargetPositions.size();
    }

    int flowTargetCount() {
        return flowTargetPositions.size();
    }

    private static GasPipeTelemetryTargets discover(Level level, Snapshot topology) {
        List<BlockPos> orderedPositions = topology.pipePositions().stream().sorted(GasNetworkTopology::compareBlockPositions).map(BlockPos::immutable).toList();
        if (orderedPositions.isEmpty()) {
            return new GasPipeTelemetryTargets(List.of(), Map.of(), Set.of());
        }

        List<BlockPos> pressureTargets = new ArrayList<>();
        Set<BlockPos> flowTargets = new HashSet<>();
        for (BlockPos pipePos : orderedPositions) {
            BlockEntity blockEntity = level.getBlockEntity(pipePos);
            if (blockEntity instanceof GasPressureTelemetryTarget) {
                pressureTargets.add(pipePos);
            }
            if (!(blockEntity instanceof GasFlowTelemetryTarget)) {
                continue;
            }

            flowTargets.add(pipePos);
        }

        Map<BlockPos, Integer> pressureIndices = new HashMap<>();
        for (int index = 0; index < pressureTargets.size(); index++) {
            pressureIndices.put(pressureTargets.get(index), index);
        }
        return new GasPipeTelemetryTargets(List.copyOf(pressureTargets), Map.copyOf(pressureIndices), Set.copyOf(flowTargets));
    }
}
