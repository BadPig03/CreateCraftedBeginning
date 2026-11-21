package net.ty.createcraftedbeginning.gas.network.solver.routing;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneLayout.Link;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneLayout.Vertex;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasMeshSearch {
    private static final double PRECISION = 1e-12;
    private final GasMeshBridges bridges;
    private final Vertex start;
    private final Map<Vertex, Node> junctions = new HashMap<>();
    private final List<Node> nodes = new ArrayList<>();
    private final List<Node> freeRows = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private final Set<Link> seen = new HashSet<>();
    private final List<Arc> path = new ArrayList<>();
    private final Node source;
    private final Vertex target;
    private Node here;
    private int cursor;
    private int side;
    private double residual;
    private double product;
    private double nextResidual;
    private double scale;
    private double weight;
    private @Nullable Link next;
    private List<Link> corridor = new ArrayList<>();
    private int arcIndex;
    private int linkIndex;
    private List<Link> branch = List.of();
    private Phase phase = Phase.BUILD;

    GasMeshSearch(GasMeshBridges bridges, Vertex start, Vertex target) {
        this.bridges = bridges;
        this.start = start;
        this.target = target;
        source = node(start);
        source.potential = 1;
        here = source;
    }

    boolean step(GasMeshRatios ratios) {
        switch (phase) {
            case BUILD -> build();
            case INITIALIZE, MULTIPLY, UPDATE, DIRECTION -> balance();
            case ORIENT -> orient();
            case TRACE -> trace();
            case EMIT -> emit(ratios);
        }
        return phase == Phase.DONE;
    }

    private Node node(Vertex vertex) {
        return junctions.computeIfAbsent(vertex, key -> {
            Node node = new Node(key);
            nodes.add(node);
            if (key != start && key != target) {
                freeRows.add(node);
            }
            return node;
        });
    }

    private void build() {
        if (next == null) {
            if (cursor == nodes.size()) {
                phase = edges.size() >= nodes.size() && junctions.containsKey(target) ? (freeRows.isEmpty() ? Phase.ORIENT : Phase.INITIALIZE) : Phase.DONE;
                cursor = 0;
                return;
            }

            here = nodes.get(cursor);
            next = here.vertex.edges.get(Iterate.directions[side++]);
            if (side == Iterate.directions.length) {
                side = 0;
                cursor++;
            }
            if (next == null || seen.contains(next.shared) || bridges.contains(next) || next.end.device) {
                next = null;
                return;
            }
        }

        Link link = next;
        seen.add(link.shared);
        corridor.add(link);
        Vertex end = link.end;
        next = null;
        if (end != source.vertex && end != target && end.edges.size() == 2 && (end.links.endpoints() | end.links.atmosphere()) == 0) {
            for (Link continuation : end.edges.values()) {
                if (continuation != link.opposite) {
                    next = continuation;
                    return;
                }
            }
        }
        Node other = node(end);
        if (other != here) {
            Edge edge = new Edge(here, other, corridor);
            edges.add(edge);
            here.arcs.add(new Arc(edge, other, true));
            other.arcs.add(new Arc(edge, here, false));
        }
        corridor = new ArrayList<>();
    }

    private void balance() {
        Node node = freeRows.get(cursor++);
        switch (phase) {
            case INITIALIZE -> {
                for (Arc arc : node.arcs) {
                    if (arc.end == source) {
                        node.error++;
                    }
                }
                node.direction = node.error / node.arcs.size();
                residual += node.error * node.direction;
            }
            case MULTIPLY -> {
                double value = node.arcs.size() * node.direction;
                for (Arc arc : node.arcs) {
                    value -= arc.end.direction;
                }
                node.product = value;
                product += value * node.direction;
            }
            case UPDATE -> {
                node.potential += scale * node.direction;
                node.error -= scale * node.product;
                nextResidual += node.error * node.error / node.arcs.size();
            }
            case DIRECTION -> node.direction = node.error / node.arcs.size() + scale * node.direction;
        }
        if (cursor < freeRows.size()) {
            return;
        }

        cursor = 0;
        switch (phase) {
            case INITIALIZE -> phase = residual == 0 ? Phase.ORIENT : Phase.MULTIPLY;
            case MULTIPLY -> {
                scale = residual / product;
                product = 0;
                phase = Phase.UPDATE;
            }
            case UPDATE -> {
                scale = nextResidual / residual;
                residual = nextResidual;
                nextResidual = 0;
                phase = residual < PRECISION * PRECISION ? Phase.ORIENT : Phase.DIRECTION;
            }
            case DIRECTION -> phase = Phase.MULTIPLY;
        }
    }

    private void orient() {
        Edge edge = edges.get(cursor++);
        edge.flow = edge.start.potential - edge.end.potential;
        if (cursor == edges.size()) {
            here = source;
            weight = 1;
            phase = Phase.TRACE;
        }
    }

    private void trace() {
        if (here.vertex == target) {
            branch = new ArrayList<>();
            arcIndex = 0;
            linkIndex = 0;
            phase = Phase.EMIT;
            return;
        }

        for (Arc arc : here.arcs) {
            double residual = arc.forward ? arc.edge.flow : -arc.edge.flow;
            if (residual > PRECISION) {
                path.add(arc);
                here = arc.end;
                weight = Math.min(weight, residual);
                return;
            }
        }
        if (path.isEmpty()) {
            phase = Phase.DONE;
            return;
        }

        Arc tail = path.removeLast();
        tail.edge.flow = 0;
        path.clear();
        here = source;
        weight = 1;
    }

    private void emit(GasMeshRatios ratios) {
        if (arcIndex == path.size()) {
            ratios.add(branch, weight);
            path.clear();
            here = source;
            weight = 1;
            phase = Phase.TRACE;
            return;
        }

        Arc arc = path.get(arcIndex);
        Edge edge = arc.edge;
        List<Link> links = edge.links;
        Link link = arc.forward ? links.get(linkIndex) : links.get(links.size() - 1 - linkIndex).opposite;
        branch.add(link);
        if (++linkIndex == links.size()) {
            edge.flow += arc.forward ? -weight : weight;
            arcIndex++;
            linkIndex = 0;
        }
    }

    private enum Phase {
        BUILD, INITIALIZE, MULTIPLY, UPDATE, DIRECTION, ORIENT, TRACE, EMIT, DONE
    }

    private static final class Node {
        private final Vertex vertex;
        private final List<Arc> arcs = new ArrayList<>();
        private double potential;
        private double direction;
        private double error;
        private double product;

        private Node(Vertex vertex) {
            this.vertex = vertex;
        }
    }

    private static final class Edge {
        private final Node start;
        private final Node end;
        private final List<Link> links;
        private double flow;

        private Edge(Node start, Node end, List<Link> links) {
            this.start = start;
            this.end = end;
            this.links = links;
        }
    }

    private record Arc(Edge edge, Node end, boolean forward) {}
}
