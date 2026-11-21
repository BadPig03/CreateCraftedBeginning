package net.ty.createcraftedbeginning.gas.network.solver.routing;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneLayout.Link;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneLayout.Vertex;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasMeshBlocks {
    private final GasZoneLayout layout;
    private final Map<Link, Block> owners = new HashMap<>();
    private final ArrayDeque<Frame> stack = new ArrayDeque<>();
    private final Map<Vertex, Frame> discovery = new HashMap<>();

    GasMeshBlocks(GasZoneLayout layout, Vertex start) {
        this.layout = layout;
        visit(start, null);
    }

    @Nullable
    Block block(Link link) {
        Block owner = owners.get(link.shared);
        if (owner == null) {
            return null;
        }

        return owner.root();
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

            if (neighbor.index < frame.index) {
                owners.put(link.shared, frame.block);
            }
            frame.low = Math.min(frame.low, neighbor.index);
        }

        stack.removeLast();
        if (frame.parent != null) {
            Frame parent = stack.getLast();
            parent.low = Math.min(parent.low, frame.low);
            if (frame.low > parent.index) {
                owners.remove(frame.parent.shared);
            }
            else if (frame.low < parent.index) {
                parent.block.join(frame.block);
            }
        }
        return stack.isEmpty();
    }

    private void visit(Vertex vertex, @Nullable Link parent) {
        int index = discovery.size();
        Block block = new Block();
        if (parent != null) {
            owners.put(parent.shared, block);
        }
        Frame frame = new Frame(vertex, parent, index, block);
        discovery.put(vertex, frame);
        layout.connect(vertex);
        stack.addLast(frame);
    }

    private static final class Frame {
        private final Vertex vertex;
        private final @Nullable Link parent;
        private final int index;
        private final Block block;
        private int low;
        private int side;

        private Frame(Vertex vertex, @Nullable Link parent, int index, Block block) {
            this.vertex = vertex;
            this.parent = parent;
            this.index = index;
            this.block = block;
            low = index;
        }
    }

    final class Block {
        private Block leader = this;
        private int members = 1;
        private @Nullable GasMeshEquation equation;

        boolean contains(Link link) {
            return block(link) == this;
        }

        GasMeshEquation equation(Vertex start, Vertex end) {
            if (GasMeshEquation.contracts(start) || GasMeshEquation.contracts(end)) {
                return new GasMeshEquation(this, start, end);
            }

            if (equation == null) {
                equation = new GasMeshEquation(this, start, end);
            }
            return equation;
        }

        private Block root() {
            Block block = this;
            while (block.leader != block) {
                block.leader = block.leader.leader;
                block = block.leader;
            }
            return block;
        }

        private void join(Block other) {
            Block first = root();
            Block second = other.root();
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
