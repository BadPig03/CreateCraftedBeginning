package net.ty.createcraftedbeginning.gas.network.solver.graph;

import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.gas.network.math.GasFlowMath;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.RandomAccess;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record GasPressureGraphSolution(List<TransferComponent> transferComponents, List<FacePressure> facePressures, boolean converged) {
    public static final GasPressureGraphSolution EMPTY = new GasPressureGraphSolution(List.of(), List.of());
    public static final GasPressureGraphSolution FAILED = new GasPressureGraphSolution(List.of(), List.of(), false);

    public GasPressureGraphSolution(List<TransferComponent> transferComponents, List<FacePressure> facePressures) {
        this(transferComponents, facePressures, true);
    }

    public GasPressureGraphSolution {
        transferComponents = List.copyOf(transferComponents);
        facePressures = facePressures instanceof FacePressureSamples ? facePressures : List.copyOf(facePressures);
    }

    public boolean isEmpty() {
        return transferComponents.isEmpty();
    }

    public @Unmodifiable List<EndpointFlow> endpointFlows() {
        List<EndpointFlow> flows = new ArrayList<>();
        for (TransferComponent component : transferComponents) {
            flows.addAll(component.endpointFlows());
        }
        return List.copyOf(flows);
    }

    public record TransferComponent(double referenceFlowRate, List<EndpointFlow> endpointFlows, List<PipeFlow> pipeFlows, Map<BlockPos, Double> budgetedFlowRates, GasFlowRouting routing) {
        public TransferComponent {
            endpointFlows = List.copyOf(endpointFlows);
            pipeFlows = List.copyOf(pipeFlows);
            budgetedFlowRates = Map.copyOf(budgetedFlowRates);
        }

        public boolean isEmpty() {
            return !Double.isFinite(referenceFlowRate) || referenceFlowRate <= GasFlowMath.FLOW_RATE_EPSILON || endpointFlows.isEmpty();
        }
    }

    public record EndpointFlow(GasNetworkPressureEndpoint endpoint, double drainFlowRate, double fillFlowRate, double manifoldPressurePa) {}

    public record PipeSegment(BlockPos pos, @Nullable Direction entryFace, @Nullable Direction exitFace) {
        public PipeSegment {
            pos = pos.immutable();
            if (entryFace == null && exitFace == null) {
                throw new IllegalArgumentException("Pipe segment at " + pos + " must reference at least one face.");
            }

            if (entryFace != null && entryFace == exitFace) {
                throw new IllegalArgumentException("Direct pipe segment at " + pos + " cannot enter and exit through the same face; got " + entryFace + '.');
            }
        }

        public static PipeSegment direct(BlockPos pos, Direction entryFace, Direction exitFace) {
            return new PipeSegment(pos, entryFace, exitFace);
        }

        public static PipeSegment inboundSpoke(BlockPos pos, Direction entryFace) {
            return new PipeSegment(pos, entryFace, null);
        }

        public static PipeSegment outboundSpoke(BlockPos pos, Direction exitFace) {
            return new PipeSegment(pos, null, exitFace);
        }
    }

    public record PipeFlow(PipeSegment segment, double flowRate) {}

    public record FacePressure(BlockFace face, double pressurePa) {}

    public static final class PressureLayout {
        private static final PressureLayout EMPTY = new PressureLayout(List.of(), new int[0], new byte[0], new int[0]);

        private final List<BlockPos> pipePositions;
        private final int[] portPipeIndices;
        private final byte[] portFaceOrdinals;
        private final int[] connectedFaceMasks;

        PressureLayout(List<BlockPos> pipePositions, int[] portPipeIndices, byte[] portFaceOrdinals, int[] connectedFaceMasks) {
            if (portPipeIndices.length != portFaceOrdinals.length || pipePositions.size() != connectedFaceMasks.length) {
                throw new IllegalArgumentException("Pressure layout dimensions must match: port indices=" + portPipeIndices.length + ", port faces=" + portFaceOrdinals.length + "; pipe positions=" + pipePositions.size() + ", connected face masks=" + connectedFaceMasks.length + '.');
            }

            this.pipePositions = List.copyOf(pipePositions);
            this.portPipeIndices = portPipeIndices;
            this.portFaceOrdinals = portFaceOrdinals;
            this.connectedFaceMasks = connectedFaceMasks;
        }

        public int pipeCount() {
            return pipePositions.size();
        }

        public int portCount() {
            return portPipeIndices.length;
        }

        public BlockPos pipePos(int pipeIndex) {
            return pipePositions.get(pipeIndex);
        }

        public int pipeIndexForPort(int portIndex) {
            return portPipeIndices[portIndex];
        }

        public int faceOrdinalForPort(int portIndex) {
            return Byte.toUnsignedInt(portFaceOrdinals[portIndex]);
        }

        public int connectedFaceMask(int pipeIndex) {
            return connectedFaceMasks[pipeIndex];
        }
    }

    public static final class FacePressureSamples extends AbstractList<FacePressure> implements RandomAccess {
        public static final FacePressureSamples EMPTY = new FacePressureSamples(PressureLayout.EMPTY, new double[0]);
        private static final Direction[] DIRECTIONS = Direction.values();

        private final PressureLayout layout;
        private final double[] pressureByPort;
        private @Nullable List<FacePressure> materialized;
        private int observedCount = -1;

        FacePressureSamples(PressureLayout layout, double[] pressureByPort) {
            if (layout.portCount() != pressureByPort.length) {
                throw new IllegalArgumentException("Pressure sample count mismatch: expected " + layout.portCount() + ", got " + pressureByPort.length + '.');
            }

            this.layout = layout;
            this.pressureByPort = pressureByPort;
        }

        @Override
        public int size() {
            if (observedCount >= 0) {
                return observedCount;
            }

            int count = 0;
            for (int portIndex = 0; portIndex < pressureByPort.length; portIndex++) {
                if (!hasPressureAtPort(portIndex)) {
                    continue;
                }

                count++;
            }
            observedCount = count;
            return count;
        }

        @Override
        public FacePressure get(int index) {
            Objects.checkIndex(index, size());
            List<FacePressure> pressures = materialized;
            if (pressures == null) {
                pressures = materialize();
                materialized = pressures;
            }
            return pressures.get(index);
        }

        public PressureLayout layout() {
            return layout;
        }

        public int portCount() {
            return pressureByPort.length;
        }

        public boolean hasPressureAtPort(int portIndex) {
            double pressurePa = pressureByPort[portIndex];
            return Double.isFinite(pressurePa) && pressurePa >= GasPressure.VACUUM_PA;
        }

        public double pressurePaAtPort(int portIndex) {
            return pressureByPort[portIndex];
        }

        private @Unmodifiable List<FacePressure> materialize() {
            List<FacePressure> pressures = new ArrayList<>(size());
            for (int portIndex = 0; portIndex < pressureByPort.length; portIndex++) {
                if (!hasPressureAtPort(portIndex)) {
                    continue;
                }

                int pipeIndex = layout.pipeIndexForPort(portIndex);
                Direction face = DIRECTIONS[layout.faceOrdinalForPort(portIndex)];
                pressures.add(new FacePressure(new BlockFace(layout.pipePos(pipeIndex), face), pressureByPort[portIndex]));
            }
            return List.copyOf(pressures);
        }
    }
}
