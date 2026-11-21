package net.ty.createcraftedbeginning.gas.network.solver.routing;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasMeshBlocks.Block;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneLayout.Link;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneLayout.Vertex;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasMeshRatios {
    private final List<Branch> branches = new ArrayList<>();
    private @Nullable GasMeshSearch search;
    private double totalWeight;

    GasMeshRatios(Block block, Vertex start, Vertex end) {
        search = new GasMeshSearch(block, start, end);
    }

    boolean advance() {
        if (search != null && !search.step(this)) {
            return false;
        }

        search = null;
        return true;
    }

    void add(List<Link> links, double weight) {
        totalWeight += weight;
        branches.add(new Branch(links, totalWeight));
    }

    int size() {
        return branches.size();
    }

    double fraction(int index) {
        return branches.get(index).cumulative / totalWeight;
    }

    List<Link> links(int index) {
        return branches.get(index).links;
    }

    private record Branch(List<Link> links, double cumulative) {}
}
