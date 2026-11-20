package net.ty.createcraftedbeginning.gas.network.solver;

import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasConnectionResolver;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology.Snapshot;
import net.ty.createcraftedbeginning.gas.network.solver.GasPipeLinkIndex.Links;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasZoneLayout {
    private final Map<BlockPos, Vertex> vertices = new HashMap<>();
    private final Map<BlockPos, Integer> ordinary = new HashMap<>();
    private final Map<BlockFace, Integer> ports = new HashMap<>();
    private final List<Integer> volumes = new ArrayList<>();
    private final List<BlockFace> internal = new ArrayList<>();
    private final Map<BlockPos, Interior> interiors = new HashMap<>();
    private final List<Vertex> devices = new ArrayList<>();

    private final @Nullable GasRouteIndex routing;
    private @Nullable Construction construction;
    private int lastBuildWork;

    GasZoneLayout(Snapshot topology, Map<BlockPos, Long> limits) {
        routing = new GasRouteIndex(topology.state(), limits, vertices, interiors);
        construction = new Construction(topology, limits);
    }

    GasZoneLayout(Level level, Snapshot topology, Map<BlockPos, Long> limits) {
        routing = null;
        GasNetworkState state = topology.state();
        List<BlockPos> positions = new ArrayList<>(topology.pipePositions());
        positions.sort(GasNetworkTopology::compareBlockPositions);
        indexVertices(state, positions, limits);
        indexCorridors(state, positions, limits);
        indexPorts(level, state);
    }

    private void indexVertices(GasNetworkState state, List<BlockPos> positions, Map<BlockPos, Long> limits) {
        for (BlockPos pos : positions) {
            Links links = state.linksAt(pos);
            if (links == null || limits.getOrDefault(pos, Long.MAX_VALUE) <= 0) {
                continue;
            }

            if (links.ordinary() && !ordinary.containsKey(pos)) {
                collectZone(state, pos, limits);
            }
            int live = links.neighbors();
            for (Direction side : Iterate.directions) {
                if (limits.getOrDefault(pos.relative(side), Long.MAX_VALUE) > 0) {
                    continue;
                }

                live &= ~(1 << side.ordinal());
            }
            if (links.ordinary() && Integer.bitCount(live) == 2 && (links.endpoints() | links.atmosphere()) == 0) {
                continue;
            }

            Vertex vertex = new Vertex(pos, !links.ordinary(), limits.getOrDefault(pos, Long.MAX_VALUE));
            vertices.put(pos, vertex);
            if (!vertex.device) {
                continue;
            }

            devices.add(vertex);
        }
    }

    private void collectZone(GasNetworkState state, BlockPos pos, Map<BlockPos, Long> limits) {
        int index = volumes.size();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        ordinary.put(pos, index);
        queue.add(pos);
        int members = 0;
        while (!queue.isEmpty()) {
            BlockPos here = queue.removeFirst();
            members++;
            int neighbors = state.ordinaryNeighbors(here);
            for (Direction side : Iterate.directions) {
                if ((neighbors & 1 << side.ordinal()) == 0) {
                    continue;
                }
                BlockPos next = here.relative(side);
                Links other = state.linksAt(next);
                if (other == null || !other.ordinary() || limits.getOrDefault(next, Long.MAX_VALUE) <= 0 || ordinary.putIfAbsent(next, index) != null) {
                    continue;
                }

                queue.add(next);
            }
        }
        volumes.add(members);
    }

    private void indexCorridors(GasNetworkState state, List<BlockPos> positions, Map<BlockPos, Long> limits) {
        for (BlockPos pos : positions) {
            Vertex start = vertices.get(pos);
            if (start == null) {
                continue;
            }
            Links links = state.linksAt(pos);
            for (Direction side : Iterate.directions) {
                if ((links.neighbors() & 1 << side.ordinal()) == 0 || start.edges.containsKey(side)) {
                    continue;
                }

                BlockPos here = pos.relative(side);
                Direction incoming = side.getOpposite();
                List<Member> members = new ArrayList<>();
                long capacity = start.capacity;
                while (limits.getOrDefault(here, Long.MAX_VALUE) > 0) {
                    capacity = Math.min(capacity, limits.getOrDefault(here, Long.MAX_VALUE));
                    Vertex end = vertices.get(here);
                    if (end != null) {
                        Link forward = new Link(end, side, incoming, capacity, null);
                        Link reverse = new Link(start, incoming, side, capacity, forward);
                        start.edges.put(side, forward);
                        end.edges.put(incoming, reverse);
                        for (Member member : members) {
                            interiors.put(member.pos, new Interior(forward, reverse, member.entry, member.exit));
                        }
                        break;
                    }

                    int neighbors = state.ordinaryNeighbors(here) & ~(1 << incoming.ordinal());
                    Direction exit = null;
                    for (Direction direction : Iterate.directions) {
                        if ((neighbors & 1 << direction.ordinal()) != 0 && limits.getOrDefault(here.relative(direction), Long.MAX_VALUE) > 0) {
                            exit = direction;
                            break;
                        }
                    }
                    if (exit == null) {
                        break;
                    }
                    members.add(new Member(here, incoming, exit));
                    here = here.relative(exit);
                    incoming = exit.getOpposite();
                }
            }
        }
    }

    private void indexPorts(Level level, GasNetworkState state) {
        for (Vertex device : devices) {
            indexInternal(level, state, device);
            indexPorts(state, device);
        }
    }

    private void indexInternal(Level level, GasNetworkState state, Vertex device) {
        Links links = state.linksAt(device.pos);
        GasTransportBehaviour behaviour = GasConnectionResolver.getTransportBehaviour(level, device.pos);
        for (Direction side : Iterate.directions) {
            int bit = 1 << side.ordinal();
            if (((links.neighbors() | links.endpoints() | links.atmosphere()) & bit) == 0) {
                continue;
            }
            if (behaviour != null && behaviour.getInternalPressureBoundary(side) != null) {
                internal.add(new BlockFace(device.pos, side));
            }
        }
    }

    private void indexPorts(GasNetworkState state, Vertex device) {
        Links links = state.linksAt(device.pos);
        for (Direction side : Iterate.directions) {
            int bit = 1 << side.ordinal();
            if (((links.neighbors() | links.endpoints() | links.atmosphere()) & bit) == 0) {
                continue;
            }

            BlockFace port = new BlockFace(device.pos, side);
            if (ports.containsKey(port)) {
                continue;
            }
            BlockPos neighbor = device.pos.relative(side);
            Integer id = ordinary.get(neighbor);
            if (id == null || (links.neighbors() & bit) == 0) {
                id = volumes.size();
                volumes.add(0);
            }
            ports.put(port, id);
            Vertex adjacent = vertices.get(neighbor);
            if ((links.neighbors() & bit) == 0 || adjacent == null || !adjacent.device) {
                continue;
            }

            ports.put(new BlockFace(neighbor, side.getOpposite()), id);
        }
    }

    int build(Level level, int budget) {
        lastBuildWork = 0;
        while (construction != null && lastBuildWork < budget) {
            lastBuildWork++;
            if (construction.step(level)) {
                construction = null;
            }
        }
        return lastBuildWork;
    }

    int lastBuildWork() {
        return lastBuildWork;
    }

    boolean complete() {
        return construction == null;
    }

    void connect(Vertex vertex) {
        if (routing != null) {
            routing.connect(vertex);
        }
    }

    @Nullable
    Vertex vertex(BlockPos pos) {
        if (routing == null) {
            return vertices.get(pos);
        }
        return routing.vertex(pos);
    }

    @Nullable
    Interior interior(BlockPos pos) {
        return interiors.get(pos);
    }

    int node(BlockFace face) {
        Integer zone = ordinary.get(face.getPos());
        if (zone == null) {
            return ports.getOrDefault(face, -1);
        }

        return zone;
    }

    int volume(int node) {
        return volumes.get(node);
    }

    int nodes() {
        return volumes.size();
    }

    List<Vertex> devices() {
        return devices;
    }

    List<BlockFace> internal() {
        return internal;
    }

    static final class Vertex {
        final BlockPos pos;
        boolean indexed;
        final boolean device;
        final long capacity;
        final Map<Direction, Link> edges = new EnumMap<>(Direction.class);

        Vertex(BlockPos pos, boolean device, long capacity) {
            this.pos = pos;
            this.device = device;
            this.capacity = capacity;
        }
    }

    static final class Link {
        final Vertex end;
        final Direction exit;
        final Direction entry;
        final long capacity;
        final Link shared;
        long turns;

        Link(Vertex end, Direction exit, Direction entry, long capacity, @Nullable Link forward) {
            shared = forward == null ? this : forward;
            this.end = end;
            this.exit = exit;
            this.entry = entry;
            this.capacity = capacity;
        }
    }

    private final class Construction {
        private final GasNetworkState state;
        private final Map<BlockPos, Long> limits;
        private final Iterator<BlockPos> positions;
        private final ArrayDeque<BlockPos> zone = new ArrayDeque<>();
        private final Map<BlockPos, Integer> anchors = new HashMap<>();
        private int zoneId;
        private int internalIndex;
        private int deviceIndex;

        private Construction(Snapshot topology, Map<BlockPos, Long> limits) {
            state = topology.state();
            this.limits = limits;
            positions = topology.pipePositions().iterator();
        }

        private boolean step(Level level) {
            List<BlockPos> positionsWithDevices = state.devicePositions();
            if (internalIndex < positionsWithDevices.size()) {
                BlockPos pos = positionsWithDevices.get(internalIndex++);
                Vertex device = vertex(pos);
                if (device != null) {
                    indexInternal(level, state, device);
                    devices.add(device);
                }
                return false;
            }
            if (!zone.isEmpty()) {
                BlockPos pos = zone.removeFirst();
                int neighbors = state.ordinaryNeighbors(pos);
                for (Direction side : Iterate.directions) {
                    if ((neighbors & 1 << side.ordinal()) == 0) {
                        continue;
                    }

                    BlockPos next = pos.relative(side);
                    Links links = state.linksAt(next);
                    if (links != null && links.ordinary() && limits.getOrDefault(next, Long.MAX_VALUE) > 0 && ordinary.putIfAbsent(next, zoneId) == null) {
                        zone.addLast(next);
                    }
                }
                volumes.set(zoneId, volumes.get(zoneId) + 1);
                return false;
            }
            if (positions.hasNext()) {
                BlockPos pos = positions.next();
                Links links = state.linksAt(pos);
                if (!links.ordinary() || limits.getOrDefault(pos, Long.MAX_VALUE) <= 0) {
                    return false;
                }
                if (limits.isEmpty()) {
                    BlockPos anchor = state.pressureZoneAt(pos);
                    Integer id = anchors.putIfAbsent(anchor, volumes.size());
                    if (id == null) {
                        id = volumes.size();
                        volumes.add(0);
                    }
                    ordinary.put(pos, id);
                    volumes.set(id, volumes.get(id) + 1);
                }
                else if (!ordinary.containsKey(pos)) {
                    zoneId = volumes.size();
                    volumes.add(0);
                    ordinary.put(pos, zoneId);
                    zone.add(pos);
                }
                return false;
            }
            if (deviceIndex < devices.size()) {
                indexPorts(state, devices.get(deviceIndex++));
                return false;
            }
            return true;
        }
    }

    private record Member(BlockPos pos, Direction entry, Direction exit) {}

    record Interior(Link forward, Link reverse, Direction entry, Direction exit) {}
}
