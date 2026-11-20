package net.ty.createcraftedbeginning.gas.network.solver;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.ty.createcraftedbeginning.gas.network.solver.GasPipeLinkIndex.Links;
import net.ty.createcraftedbeginning.gas.network.solver.GasZoneLayout.Interior;
import net.ty.createcraftedbeginning.gas.network.solver.GasZoneLayout.Link;
import net.ty.createcraftedbeginning.gas.network.solver.GasZoneLayout.Vertex;
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
        this.interiors = interiors;
        this.state = state;
        this.limits = limits;
        this.vertices = vertices;
    }

    @Nullable
    Vertex vertex(BlockPos pos) {
        Vertex existing = vertices.get(pos);
        if (existing != null) {
            return existing;
        }
        Links links = state.linksAt(pos);
        long capacity = limits.getOrDefault(pos, Long.MAX_VALUE);
        if (links == null || capacity <= 0) {
            return null;
        }

        Vertex created = new Vertex(pos, !links.ordinary(), capacity);
        vertices.put(pos, created);
        return created;
    }

    void connect(Vertex start) {
        if (start.indexed) {
            return;
        }

        start.indexed = true;
        int mask = state.linksAt(start.pos).neighbors();
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
                Links links = state.linksAt(pos);
                capacity = Math.min(capacity, limits.getOrDefault(pos, Long.MAX_VALUE));
                if (links == null || capacity <= 0 || (links.neighbors() & 1 << entry.ordinal()) == 0) {
                    break;
                }
                int coordinate = exit.getAxis().choose(pos.getX(), pos.getY(), pos.getZ());
                if (links.ordinary() && links.neighbors() == straight && (links.endpoints() | links.atmosphere()) == 0 && Math.floorMod(coordinate, STRAIGHT_SPAN) != 0) {
                    continue;
                }

                Vertex end = vertex(pos);
                Link forward = new Link(end, exit, entry, capacity, null);
                Link reverse = new Link(start, entry, exit, capacity, forward);
                start.edges.put(exit, forward);
                end.edges.put(entry, reverse);
                Interior span = new Interior(forward, reverse, entry, exit);
                for (BlockPos inner = start.pos.relative(exit); !inner.equals(pos); inner = inner.relative(exit)) {
                    interiors.put(inner, span);
                }
                break;
            }
        }
    }
}
