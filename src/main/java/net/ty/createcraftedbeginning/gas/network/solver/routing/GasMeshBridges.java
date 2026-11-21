package net.ty.createcraftedbeginning.gas.network.solver.routing;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneLayout.Link;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneLayout.Vertex;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasMeshBridges {
    private final GasZoneLayout layout;
    private final Set<Link> bridges = new HashSet<>();
    private final ArrayDeque<Frame> stack = new ArrayDeque<>();
    private final Map<Vertex, Frame> discovery = new HashMap<>();

    GasMeshBridges(GasZoneLayout layout, Vertex start) {
        this.layout = layout;
        visit(start, null);
    }

    boolean contains(Link link) {
        return bridges.contains(link.shared);
    }

    boolean ready() {
        if (stack.isEmpty()) {
            return true;
        }

        Frame frame = stack.getLast();
        while (frame.side < Iterate.directions.length) {
            Link link = frame.vertex.edges.get(Iterate.directions[frame.side++]);
            if (link == null || link.end.device || link == frame.parent) {
                continue;
            }

            Frame neighbor = discovery.get(link.end);
            if (neighbor == null) {
                visit(link.end, link.opposite);
                return false;
            }

            frame.low = Math.min(frame.low, neighbor.index);
        }

        stack.removeLast();
        if (frame.parent != null) {
            Frame parent = stack.getLast();
            parent.low = Math.min(parent.low, frame.low);
            if (frame.low > parent.index) {
                bridges.add(frame.parent.shared);
            }
            else {
                parent.join(frame);
            }
        }
        return stack.isEmpty();
    }

    GasMeshEquation equation(Vertex start, Vertex end) {
        if (GasMeshEquation.contracts(start) || GasMeshEquation.contracts(end)) {
            return new GasMeshEquation(this, start, end);
        }

        Frame region = discovery.get(start).root();
        if (region.equation == null) {
            region.equation = new GasMeshEquation(this, start, end);
        }
        return region.equation;
    }

    private void visit(Vertex vertex, @Nullable Link parent) {
        int index = discovery.size();
        Frame frame = new Frame(vertex, parent, index);
        discovery.put(vertex, frame);
        layout.connect(vertex);
        stack.addLast(frame);
    }

    private static final class Frame {
        private final Vertex vertex;
        private final @Nullable Link parent;
        private final int index;
        private Frame leader = this;
        private int members = 1;
        private @Nullable GasMeshEquation equation;
        private int low;
        private int side;

        private Frame(Vertex vertex, @Nullable Link parent, int index) {
            this.vertex = vertex;
            this.parent = parent;
            this.index = index;
            low = index;
        }

        private Frame root() {
            Frame frame = this;
            while (frame.leader != frame) {
                frame.leader = frame.leader.leader;
                frame = frame.leader;
            }
            return frame;
        }

        private void join(Frame other) {
            Frame first = root();
            Frame second = other.root();
            if (first.members < second.members) {
                first.leader = second;
                second.members += first.members;
            }
            else {
                second.leader = first;
                first.members += second.members;
            }
        }
    }
}
