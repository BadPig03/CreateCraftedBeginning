package net.ty.createcraftedbeginning.gas.network.solver.routing;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasMeshBlocks.Block;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasMeshEquation.Arc;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasMeshEquation.Edge;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasMeshEquation.Node;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneLayout.Link;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneLayout.Vertex;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasMeshSearch {
    private static final double PRECISION = 1.0E-12;
    private final GasMeshEquation equation;
    private final Vertex start;
    private final Vertex target;
    private final Map<Node, List<Arc>> order = new HashMap<>();
    private final List<Node> queue = new ArrayList<>();
    private final List<Arc> path = new ArrayList<>();
    private List<Link> branch = List.of();
    private GasMeshPotential potential;
    private boolean[] seen;
    private double[] flows;
    private Node source;
    private Node here;
    private int cursor;
    private double weight;
    private int arcIndex;
    private int linkIndex;
    private Phase phase = Phase.EQUATION;

    GasMeshSearch(Block block, Vertex start, Vertex target) {
        equation = block.equation(start, target);
        this.start = start;
        this.target = target;
    }

    boolean step(GasMeshRatios ratios) {
        switch (phase) {
            case EQUATION -> {
                if (equation.ready()) {
                    source = equation.node(start);
                    potential = new GasMeshPotential(equation.factor, source.index, equation.node(target).index);
                    seen = new boolean[equation.edges.size()];
                    flows = new double[seen.length];
                    queue.add(source);
                    order.put(source, new ArrayList<>());
                    phase = Phase.ORDER;
                }
            }
            case ORDER -> order();
            case SOLVE -> {
                if (potential.advance()) {
                    phase = Phase.ORIENT;
                }
            }
            case ORIENT -> orient();
            case TRACE -> trace();
            case EMIT -> emit(ratios);
        }
        return phase == Phase.DONE;
    }

    private void order() {
        Node node = queue.get(cursor++);
        List<Arc> arcs = order.get(node);
        for (Arc arc : node.arcs.values()) {
            int index = arc.edge().index();
            if (seen[index]) {
                continue;
            }
            seen[index] = true;
            arcs.add(arc);
            List<Arc> reverse = order.computeIfAbsent(arc.end(), end -> {
                queue.add(end);
                return new ArrayList<>();
            });
            reverse.add(new Arc(arc.edge(), node, !arc.forward()));
        }
        if (cursor == queue.size()) {
            cursor = 0;
            phase = Phase.SOLVE;
        }
    }

    private void orient() {
        Edge edge = equation.edges.get(cursor++);
        flows[edge.index()] = potential.at(edge.start().index) - potential.at(edge.end().index);
        if (cursor == equation.edges.size()) {
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

        for (Arc arc : order.get(here)) {
            double residual = arc.forward() ? flows[arc.edge().index()] : -flows[arc.edge().index()];
            if (residual > PRECISION) {
                path.add(arc);
                here = arc.end();
                weight = Math.min(weight, residual);
                return;
            }
        }
        if (path.isEmpty()) {
            phase = Phase.DONE;
            return;
        }

        Arc tail = path.removeLast();
        flows[tail.edge().index()] = 0;
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
        Edge edge = arc.edge();
        List<Link> links = edge.links();
        Link link = arc.forward() ? links.get(linkIndex) : links.get(links.size() - 1 - linkIndex).opposite;
        branch.add(link);
        if (++linkIndex == links.size()) {
            flows[edge.index()] += arc.forward() ? -weight : weight;
            arcIndex++;
            linkIndex = 0;
        }
    }

    private enum Phase {
        EQUATION, ORDER, SOLVE, ORIENT, TRACE, EMIT, DONE
    }
}
