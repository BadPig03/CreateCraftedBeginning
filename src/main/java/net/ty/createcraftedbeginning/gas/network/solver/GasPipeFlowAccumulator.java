package net.ty.createcraftedbeginning.gas.network.solver;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.events.GasCollisionEvent;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasConnectionResolver;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection;
import net.ty.createcraftedbeginning.gas.network.math.GasFlowMath;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution.PipeFlow;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution.PipeSegment;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasPipeFlowAccumulator {
    private final Set<BlockPos> telemetryTargetPositions;
    private final Map<BlockPos, GasStack> gasByPipe = new HashMap<>();
    private final Map<BlockPos, EnumMap<Direction, MutableFaceFlow>> faceFlows = new HashMap<>();
    private final Set<BlockPos> collidedPositions = new HashSet<>();

    private boolean collision;

    GasPipeFlowAccumulator(GasPipeTelemetryTargets telemetryTargets) {
        telemetryTargetPositions = telemetryTargets.flowTargetPositions();
    }

    boolean hasCollision() {
        return collision;
    }

    boolean ensureCompatible(Level level, List<PipeFlow> flows, GasStack gas, double scale) {
        for (PipeFlow solved : flows) {
            long amount = GasFlowMath.amountForScale(solved.flowRate(), scale);
            if (amount <= 0) {
                continue;
            }

            PipeSegment segment = solved.segment();
            GasStack existingGas = gasByPipe.get(segment.pos());
            if (existingGas == null || existingGas.isEmpty() || GasStack.isSameGasSameComponents(existingGas, gas)) {
                continue;
            }

            collision = true;
            if (collidedPositions.add(segment.pos())) {
                GasCollisionEvent.handleCollision(level, segment.pos(), existingGas, gas.copyWithAmount(amount));
            }
            return false;
        }
        return true;
    }

    void recordPath(List<PipeSegment> segments, GasStack gas, long amount) {
        if (gas.isEmpty() || amount <= 0) {
            return;
        }

        for (PipeSegment segment : segments) {
            GasStack flowGas = gas.copyWithAmount(amount);
            Direction entryFace = segment.entryFace();
            Direction exitFace = segment.exitFace();
            if (entryFace != null) {
                GasStack existingGas = gasByPipe.get(segment.pos());
                if (existingGas == null) {
                    gasByPipe.put(segment.pos(), flowGas.copy());
                }
                else {
                    existingGas.grow(amount);
                }
                addMutableFaceFlow(segment.pos(), entryFace, true, flowGas);
            }
            if (exitFace == null) {
                continue;
            }

            addMutableFaceFlow(segment.pos(), exitFace, false, flowGas);
        }
    }

    void apply(Level level, Set<BlockPos> pipePositions) {
        for (BlockPos pipePos : pipePositions) {
            GasTransportBehaviour behaviour = GasConnectionResolver.getTransportBehaviour(level, pipePos);
            if (behaviour == null) {
                continue;
            }

            EnumMap<Direction, MutableFaceFlow> byFace = faceFlows.get(pipePos);
            boolean changed = false;
            for (Direction side : Iterate.directions) {
                GasPipeConnection connection = behaviour.getConnection(side);
                if (connection == null) {
                    continue;
                }

                MutableFaceFlow faceFlow = byFace == null ? null : byFace.get(side);
                if (faceFlow == null) {
                    changed |= connection.clearFlowState();
                    continue;
                }

                long inbound = faceFlow.inboundFlowRate;
                long outbound = faceFlow.outboundFlowRate;
                if (inbound == outbound) {
                    changed |= connection.clearFlowState();
                    continue;
                }

                boolean isInbound = inbound > outbound;
                long netFlowRate = isInbound ? BoundedMath.saturatedSubtract(inbound, outbound) : BoundedMath.saturatedSubtract(outbound, inbound);
                changed |= connection.setFlowState(faceFlow.gas, isInbound, netFlowRate);
            }
            if (!changed || !telemetryTargetPositions.contains(pipePos)) {
                continue;
            }

            if (!behaviour.publishFlowTelemetry()) {
                continue;
            }

            GasSolverProfiler.recordFlowTelemetryUpdate();
        }
    }

    private void addMutableFaceFlow(BlockPos pos, Direction face, boolean inbound, GasStack gas) {
        EnumMap<Direction, MutableFaceFlow> byFace = faceFlows.computeIfAbsent(pos, ignored -> new EnumMap<>(Direction.class));
        MutableFaceFlow flow = byFace.computeIfAbsent(face, ignored -> new MutableFaceFlow(gas.copyWithAmount(1)));
        flow.add(inbound, gas.getAmount());
    }

    private static final class MutableFaceFlow {
        private final GasStack gas;
        private long inboundFlowRate;
        private long outboundFlowRate;

        private MutableFaceFlow(GasStack gas) {
            this.gas = gas;
        }

        private void add(boolean inbound, long flowRate) {
            if (inbound) {
                inboundFlowRate = BoundedMath.saturatedAdd(inboundFlowRate, flowRate);
                return;
            }

            outboundFlowRate = BoundedMath.saturatedAdd(outboundFlowRate, flowRate);
        }
    }
}
