package net.ty.createcraftedbeginning.gas.network.solver.routing;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkState;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkState.PipeLinks;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneLayout.Interior;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneLayout.Link;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneLayout.Vertex;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasRouteIndex {
    private static final int STRAIGHT_SPAN = 8;
    private final GasNetworkState state;
    private final Map<BlockPos, Long> limits;
    private final Map<BlockPos, Vertex> vertices;
    private final Map<BlockPos, Interior> interiors;

    GasRouteIndex(GasNetworkState state, Map<BlockPos, Long> limits, Map<BlockPos, Vertex> vertices, Map<BlockPos, Interior> interiors) {
        this.state = state;
        this.limits = limits;
        this.vertices = vertices;
        this.interiors = interiors;
    }

    @Nullable
    Vertex vertex(BlockPos pos) {
        Vertex existing = vertices.get(pos);
        if (existing != null) {
            return existing;
        }
        PipeLinks links = state.linksAt(pos);
        long capacity = limits.getOrDefault(pos, Long.MAX_VALUE);
        if (links == null || capacity <= 0) {
            return null;
        }

        return addVertex(pos, links, capacity);
    }

    void connect(Vertex start) {
        if (start.indexed) {
            return;
        }

        start.indexed = true;
        int mask = start.links.neighbors();
        for (Direction exit : Iterate.directions) {
            if ((mask & 1 << exit.ordinal()) == 0 || start.edges.containsKey(exit)) {
                continue;
            }

            Direction entry = exit.getOpposite();
            int straight = 1 << exit.ordinal() | 1 << entry.ordinal();
            BlockPos pos = start.pos;
            long capacity = start.capacity;
            while (true) {
                pos = pos.relative(exit);
                PipeLinks links = state.linksAt(pos);
                long pipeCapacity = limits.getOrDefault(pos, Long.MAX_VALUE);
                capacity = Math.min(capacity, pipeCapacity);
                if (links == null || capacity <= 0 || (links.neighbors() & 1 << entry.ordinal()) == 0) {
                    break;
                }
                int coordinate = exit.getAxis().choose(pos.getX(), pos.getY(), pos.getZ());
                if (links.ordinary() && links.neighbors() == straight && (links.endpoints() | links.atmosphere()) == 0 && Math.floorMod(coordinate, STRAIGHT_SPAN) != 0) {
                    continue;
                }

                Vertex end = vertices.get(pos);
                if (end == null) {
                    end = addVertex(pos, links, pipeCapacity);
                }

                Link forward = Link.connect(start, end, exit, entry, capacity);
                Interior span = new Interior(forward, forward.opposite, entry, exit);
                for (BlockPos inner = start.pos.relative(exit); !inner.equals(pos); inner = inner.relative(exit)) {
                    interiors.put(inner, span);
                }
                break;
            }
        }
    }

    private Vertex addVertex(BlockPos pos, PipeLinks links, long capacity) {
        Vertex vertex = new Vertex(pos, links, capacity);
        vertices.put(pos, vertex);
        return vertex;
    }
}
