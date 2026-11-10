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
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasFlowRouting.Route;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasFlowRouting.RoutedPlan;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution.PipeSegment;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasPipeFlowAccumulator {
    private final Set<BlockPos> telemetryTargetPositions;
    private final Map<BlockPos, MutablePipeFlow> pipeFlows = new HashMap<>();
    private final Set<BlockPos> collidedPositions = new HashSet<>();

    private boolean collision;

    GasPipeFlowAccumulator(GasPipeTelemetryTargets telemetryTargets) {
        telemetryTargetPositions = telemetryTargets.flowTargetPositions();
    }

    boolean hasCollision() {
        return collision;
    }

    boolean ensureCompatible(Level level, RoutedPlan plan, GasStack gas) {
        for (Route route : plan.routes()) {
            long amount = route.amount();
            if (amount <= 0) {
                continue;
            }

            for (PipeSegment segment : route.segments()) {
                MutablePipeFlow pipeFlow = pipeFlows.get(segment.pos());
                GasStack existingGas = pipeFlow == null ? null : pipeFlow.gas;
                if (existingGas == null || existingGas.isEmpty() || GasStack.isSameGasSameComponents(existingGas, gas)) {
                    continue;
                }

                collision = true;
                if (collidedPositions.add(segment.pos())) {
                    GasCollisionEvent.handleCollision(level, segment.pos(), existingGas.copyWithAmount(pipeFlow.amount), gas.copyWithAmount(amount));
                }
                return false;
            }
        }
        return true;
    }

    void recordPath(List<PipeSegment> segments, GasStack gas, long amount) {
        if (gas.isEmpty() || amount <= 0) {
            return;
        }

        GasStack flowGas = gas.copyWithAmount(1);
        for (PipeSegment segment : segments) {
            MutablePipeFlow pipeFlow = pipeFlows.computeIfAbsent(segment.pos(), ignored -> new MutablePipeFlow());
            Direction entryFace = segment.entryFace();
            Direction exitFace = segment.exitFace();
            if (entryFace != null) {
                if (pipeFlow.gas == null) {
                    pipeFlow.gas = flowGas;
                }
                pipeFlow.amount = BoundedMath.saturatedAdd(pipeFlow.amount, amount);
                pipeFlow.add(entryFace, true, flowGas, amount);
            }
            if (exitFace == null) {
                continue;
            }

            pipeFlow.add(exitFace, false, flowGas, amount);
        }
    }

    void apply(Level level, Set<BlockPos> pipePositions) {
        for (BlockPos pipePos : pipePositions) {
            GasTransportBehaviour behaviour = GasConnectionResolver.getTransportBehaviour(level, pipePos);
            if (behaviour == null) {
                continue;
            }

            MutablePipeFlow pipeFlow = pipeFlows.get(pipePos);
            boolean changed = false;
            for (Direction side : Iterate.directions) {
                GasPipeConnection connection = behaviour.getConnection(side);
                if (connection == null) {
                    continue;
                }

                MutableFaceFlow faceFlow = pipeFlow == null ? null : pipeFlow.faces[side.ordinal()];
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

    private static final class MutablePipeFlow {
        private final MutableFaceFlow[] faces = new MutableFaceFlow[Iterate.directions.length];
        @Nullable
        private GasStack gas;
        private long amount;

        private void add(Direction face, boolean inbound, GasStack gas, long flowRate) {
            int index = face.ordinal();
            MutableFaceFlow flow = faces[index];
            if (flow == null) {
                flow = new MutableFaceFlow(gas);
                faces[index] = flow;
            }
            flow.add(inbound, flowRate);
        }
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
