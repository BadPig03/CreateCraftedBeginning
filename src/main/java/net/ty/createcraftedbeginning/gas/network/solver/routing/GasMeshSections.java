package net.ty.createcraftedbeginning.gas.network.solver.routing;

import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneRoute.Pass;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasMeshSections implements GasZoneLayout.MeshWork {
    private final GasZoneLayout layout;
    private final List<Pass> original;
    private final GasMeshPlan result = new GasMeshPlan();
    private final List<Section> sections = new ArrayList<>();
    private final Set<Integer> visitedZones = new HashSet<>();
    private int cursor;
    private int start;
    private int sectionIndex;
    private int passIndex;
    private double lower;
    private double upper = 1;
    private boolean divided;
    private GasMeshPlan.BranchBuilder branch;
    private Phase phase = Phase.PARTITION;

    GasMeshSections(GasZoneLayout layout, GasZoneRoute route) {
        this.layout = layout;
        original = route.passes();
    }

    @Override
    public GasMeshPlan result() {
        return result;
    }

    @Override
    public boolean step(Level level) {
        switch (phase) {
            case PARTITION -> partition();
            case SOLVE -> solve(level);
            case WEIGH -> weigh(level);
            case EMIT -> emit();
        }
        if (phase != Phase.DONE) {
            return false;
        }

        result.finish();
        return true;
    }

    private void partition() {
        Pass first = original.get(start);
        Pass last = original.get(cursor++);
        if (!first.vertex.device && cursor < original.size() && !original.get(cursor).vertex.device) {
            return;
        }

        GasMeshSearch search = null;
        if (!first.vertex.device) {
            BlockFace inlet = new BlockFace(first.position(), first.entry);
            if (!visitedZones.add(layout.node(inlet))) {
                phase = Phase.DONE;
                return;
            }
            if (first != last) {
                search = new GasMeshSearch(layout, inlet, new BlockFace(last.position(), last.exit), first.vertex, last.vertex);
            }
        }

        sections.add(new Section(original.subList(start, cursor), search));
        start = cursor;
        if (cursor == original.size()) {
            phase = Phase.SOLVE;
        }
    }

    private void solve(Level level) {
        Section section = sections.get(sectionIndex);
        if (section.search != null) {
            if (!section.search.step(level)) {
                return;
            }

            GasMeshPlan plan = section.search.result();
            if (plan.size() > 1) {
                section.plan = plan;
                divided = true;
            }
            section.search = null;
        }
        if (++sectionIndex == sections.size()) {
            sectionIndex = 0;
            phase = divided ? Phase.WEIGH : Phase.DONE;
        }
    }

    private void weigh(Level level) {
        Section section = sections.get(sectionIndex);
        GasMeshPlan plan = section.plan;
        if (plan != null) {
            while (section.index + 1 < plan.size() && plan.fraction(section.index) <= lower) {
                section.index++;
            }
            upper = Math.min(upper, plan.fraction(section.index));
        }
        if (++sectionIndex == sections.size()) {
            sectionIndex = 0;
            branch = result.new BranchBuilder(level, upper - lower);
            phase = Phase.EMIT;
        }
    }

    private void emit() {
        Section section = sections.get(sectionIndex);
        List<Pass> segment = section.plan == null ? section.passes : section.plan.route(section.index).passes();
        Pass pass = ++passIndex == segment.size() ? section.passes.getLast() : segment.get(passIndex - 1);
        if (section.plan != null && passIndex == segment.size()) {
            Pass end = segment.getLast();
            pass = new Pass(pass.vertex, end.entry, pass.exit, pass.next);
        }
        branch.add(pass);
        if (passIndex < segment.size()) {
            return;
        }

        passIndex = 0;
        if (++sectionIndex < sections.size()) {
            return;
        }

        branch.finish(pass);
        sectionIndex = 0;
        lower = upper;
        upper = 1;
        phase = lower == 1 ? Phase.DONE : Phase.WEIGH;
    }

    private enum Phase {
        PARTITION, SOLVE, WEIGH, EMIT, DONE
    }

    private static final class Section {
        private final List<Pass> passes;
        private @Nullable GasMeshSearch search;
        private @Nullable GasMeshPlan plan;
        private int index;

        private Section(List<Pass> passes, @Nullable GasMeshSearch search) {
            this.passes = passes;
            this.search = search;
        }
    }
}
