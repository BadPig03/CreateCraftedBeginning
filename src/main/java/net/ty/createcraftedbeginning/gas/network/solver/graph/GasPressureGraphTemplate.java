package net.ty.createcraftedbeginning.gas.network.solver.graph;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasConnectionResolver;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology.Snapshot;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution.PressureLayout;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
record GasPressureGraphTemplate(int staticNodeCount, List<PortKey> ports, Map<PortKey, Integer> portNodes, boolean[] observablePressurePorts, PressureLayout pressureLayout, List<TransportEdgeTemplate> transportEdges, List<ManifoldSpokeTemplate> manifoldSpokes, List<AdjacentEdgeTemplate> adjacentEdges) {
    static GasPressureGraphTemplate create(Level level, Snapshot topology) {
        List<BlockPos> orderedPipes = new ArrayList<>(topology.pipePositions());
        orderedPipes.sort(GasNetworkTopology::compareBlockPositions);
        List<PortKey> ports = new ArrayList<>();
        Map<PortKey, Integer> portNodes = new HashMap<>();
        Map<BlockPos, Integer> pipeIndices = new HashMap<>();
        int[] connectedFaceMasks = new int[orderedPipes.size()];
        for (int pipeIndex = 0; pipeIndex < orderedPipes.size(); pipeIndex++) {
            BlockPos pipePos = orderedPipes.get(pipeIndex);
            pipeIndices.put(pipePos, pipeIndex);
            GasTransportBehaviour behaviour = GasConnectionResolver.getTransportBehaviour(level, pipePos);
            if (behaviour == null) {
                continue;
            }

            for (Direction side : Iterate.directions) {
                if (behaviour.getConnection(side) == null) {
                    continue;
                }

                PortKey port = new PortKey(pipePos, side);
                portNodes.put(port, ports.size());
                ports.add(port);
                connectedFaceMasks[pipeIndex] |= 1 << side.ordinal();
            }
        }

        boolean[] observablePressurePorts = new boolean[ports.size()];
        int[] portPipeIndices = new int[ports.size()];
        byte[] portFaceOrdinals = new byte[ports.size()];
        for (int portIndex = 0; portIndex < ports.size(); portIndex++) {
            PortKey port = ports.get(portIndex);
            Integer pipeIndex = pipeIndices.get(port.pos);
            if (pipeIndex == null) {
                throw new IllegalStateException("Pressure port at " + port.pos + " on face " + port.face + " does not belong to its topology.");
            }

            portPipeIndices[portIndex] = pipeIndex;
            portFaceOrdinals[portIndex] = (byte) port.face.ordinal();
            GasTransportBehaviour behaviour = GasConnectionResolver.getTransportBehaviour(level, port.pos);
            if (behaviour == null) {
                continue;
            }

            observablePressurePorts[portIndex] = behaviour.canObservePressureOnFace(level.getBlockState(port.pos), port.face);
        }

        PressureLayout pressureLayout = new PressureLayout(orderedPipes, portPipeIndices, portFaceOrdinals, connectedFaceMasks);
        int staticNodeCount = ports.size();
        List<TransportEdgeTemplate> transportEdges = new ArrayList<>();
        List<ManifoldSpokeTemplate> manifoldSpokes = new ArrayList<>();
        for (BlockPos pipePos : orderedPipes) {
            List<PortKey> pipePorts = new ArrayList<>();
            for (Direction face : Iterate.directions) {
                PortKey port = new PortKey(pipePos, face);
                if (!portNodes.containsKey(port)) {
                    continue;
                }

                pipePorts.add(port);
            }

            if (pipePorts.size() >= 3) {
                int manifoldNode = staticNodeCount++;
                for (PortKey port : pipePorts) {
                    manifoldSpokes.add(new ManifoldSpokeTemplate(portNodes.get(port), manifoldNode, pipePos, port.face));
                }
                continue;
            }

            for (PortKey entryPort : pipePorts) {
                for (PortKey exitPort : pipePorts) {
                    if (entryPort.face == exitPort.face) {
                        continue;
                    }

                    transportEdges.add(new TransportEdgeTemplate(portNodes.get(entryPort), portNodes.get(exitPort), pipePos, entryPort.face, exitPort.face));
                }
            }
        }

        List<AdjacentEdgeTemplate> adjacentEdges = new ArrayList<>();
        for (int fromNode = 0; fromNode < ports.size(); fromNode++) {
            PortKey source = ports.get(fromNode);
            PortKey target = new PortKey(source.pos.relative(source.face), source.face.getOpposite());
            Integer toNode = portNodes.get(target);
            if (toNode == null) {
                continue;
            }

            adjacentEdges.add(new AdjacentEdgeTemplate(fromNode, toNode, source, target));
        }
        return new GasPressureGraphTemplate(staticNodeCount, List.copyOf(ports), Map.copyOf(portNodes), observablePressurePorts, pressureLayout, List.copyOf(transportEdges), List.copyOf(manifoldSpokes), List.copyOf(adjacentEdges));
    }

    record PortKey(BlockPos pos, Direction face) {
        PortKey {
            pos = pos.immutable();
        }
    }

    record TransportEdgeTemplate(int fromNode, int toNode, BlockPos pipePos, Direction entryFace, Direction exitFace) {
        TransportEdgeTemplate {
            pipePos = pipePos.immutable();
        }
    }

    record ManifoldSpokeTemplate(int portNode, int manifoldNode, BlockPos pipePos, Direction face) {
        ManifoldSpokeTemplate {
            pipePos = pipePos.immutable();
        }
    }

    record AdjacentEdgeTemplate(int fromNode, int toNode, PortKey source, PortKey target) {}
}
