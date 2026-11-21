package net.ty.createcraftedbeginning.gas.network.solver.routing;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneLayout.Link;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneLayout.Vertex;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasMeshEquation {
    final List<Node> nodes = new ArrayList<>();
    final List<Edge> edges = new ArrayList<>();
    final GasMeshFactor factor = new GasMeshFactor();
    private final Map<Vertex, Node> junctions = new HashMap<>();
    private final Set<Link> seen = new HashSet<>();
    private final GasMeshBridges bridges;
    private final Vertex start;
    private final Vertex target;
    private List<Link> corridor = new ArrayList<>();
    private @Nullable Link next;
    private int cursor;
    private int side;
    private boolean built;

    GasMeshEquation(GasMeshBridges bridges, Vertex start, Vertex target) {
        this.bridges = bridges;
        this.start = start;
        this.target = target;
        node(start);
    }

    static boolean contracts(Vertex vertex) {
        return vertex.edges.size() == 2 && (vertex.links.endpoints() | vertex.links.atmosphere()) == 0;
    }

    Node node(Vertex vertex) {
        return junctions.computeIfAbsent(vertex, key -> {
            Node node = new Node(nodes.size(), key);
            nodes.add(node);
            factor.addRow();
            return node;
        });
    }

    boolean ready() {
        if (built) {
            return factor.ready();
        }
        if (cursor == nodes.size()) {
            built = true;
            return false;
        }

        Node here = nodes.get(cursor);
        if (next == null) {
            while (side < Iterate.directions.length) {
                Link link = here.vertex.edges.get(Iterate.directions[side++]);
                if (link != null && !link.end.device && !bridges.contains(link) && !seen.contains(link.shared)) {
                    next = link;
                    break;
                }
            }
            if (next == null) {
                cursor++;
                side = 0;
                return false;
            }
        }

        Link link = next;
        next = null;
        seen.add(link.shared);
        corridor.add(link);
        Vertex end = link.end;
        if (end != start && end != target && contracts(end)) {
            for (Link continuation : end.edges.values()) {
                if (continuation != link.opposite) {
                    next = continuation;
                    return false;
                }
            }
        }

        Node other = node(end);
        if (other != here) {
            Edge edge = new Edge(edges.size(), here, other, corridor);
            edges.add(edge);
            here.arcs.put(corridor.getFirst().exit, new Arc(edge, other, true));
            other.arcs.put(link.entry, new Arc(edge, here, false));
            factor.connect(here.index, other.index);
        }
        corridor = new ArrayList<>();
        return false;
    }

    static final class Node {
        final int index;
        final Vertex vertex;
        final Map<Direction, Arc> arcs = new EnumMap<>(Direction.class);

        private Node(int index, Vertex vertex) {
            this.index = index;
            this.vertex = vertex;
        }
    }

    record Edge(int index, Node start, Node end, List<Link> links) {}

    record Arc(Edge edge, Node end, boolean forward) {}
}
