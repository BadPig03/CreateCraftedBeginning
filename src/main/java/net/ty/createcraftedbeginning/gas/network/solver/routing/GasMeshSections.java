package net.ty.createcraftedbeginning.gas.network.solver.routing;

import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneLayout.Link;
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
    private @Nullable GasMeshBridges bridges;
    private int cursor;
    private int coreStart = -1;
    private int sectionIndex;
    private int linkIndex;
    private Direction entry;
    private double lower;
    private double upper = 1;
    private boolean divided;
    private GasMeshPlan.BranchBuilder branch;
    private Phase phase = Phase.PARTITION;

    GasMeshSections(GasZoneLayout layout, GasZoneRoute route) {
        this.layout = layout;
        original = route.passes();
        entry = original.getFirst().entry;
    }

    @Override
    public GasMeshPlan result() {
        return result;
    }

    @Override
    public boolean step(Level level) {
        switch (phase) {
            case PARTITION -> partition();
            case SOLVE -> solve();
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
        Pass pass = original.get(cursor);
        if (!pass.vertex.device && bridges == null) {
            int zone = layout.node(new BlockFace(pass.position(), pass.entry));
            if (zone < 0) {
                return;
            }
            if (!visitedZones.add(zone)) {
                phase = Phase.DONE;
                return;
            }
            bridges = layout.meshBridges(zone, pass.vertex);
        }

        Link link = pass.next;
        if (bridges != null && link != null && !link.end.device && !bridges.ready()) {
            return;
        }

        if (bridges != null && link != null && !link.end.device && !bridges.contains(link)) {
            if (coreStart < 0) {
                coreStart = cursor;
            }
        }
        else if (coreStart >= 0 && bridges != null) {
            GasMeshRatios core = layout.meshCore(bridges, original.get(coreStart).vertex, pass.vertex);
            sections.add(new Section(coreStart, cursor, core));
            coreStart = -1;
        }
        if (pass.vertex.device || link == null || link.end.device) {
            bridges = null;
        }
        if (++cursor == original.size()) {
            cursor = 0;
            phase = sections.isEmpty() ? Phase.DONE : Phase.SOLVE;
        }
    }

    private void solve() {
        GasMeshRatios core = sections.get(sectionIndex).core;
        if (!core.advance()) {
            return;
        }

        divided |= core.size() > 1;
        if (++sectionIndex == sections.size()) {
            sectionIndex = 0;
            phase = divided ? Phase.WEIGH : Phase.DONE;
        }
    }

    private void weigh(Level level) {
        Section section = sections.get(sectionIndex);
        GasMeshRatios core = section.core;
        if (core.size() > 1) {
            if (core.fraction(section.choice) <= lower) {
                section.choice++;
            }
            upper = Math.min(upper, core.fraction(section.choice));
        }
        if (++sectionIndex == sections.size()) {
            sectionIndex = 0;
            branch = result.new BranchBuilder(level, upper - lower);
            phase = Phase.EMIT;
        }
    }

    private void emit() {
        Pass pass = original.get(cursor);
        Link link = pass.next;
        if (sectionIndex < sections.size() && cursor == sections.get(sectionIndex).start) {
            Section section = sections.get(sectionIndex);
            if (section.core.size() > 1) {
                List<Link> links = section.core.links(section.choice);
                link = links.get(linkIndex++);
                if (linkIndex == links.size()) {
                    sectionIndex++;
                    cursor = section.end;
                    linkIndex = 0;
                }
            }
            else {
                sectionIndex++;
                cursor++;
            }
        }
        else {
            cursor++;
        }
        if (link != null) {
            branch.add(new Pass(link.opposite.end, entry, link.exit, link));
            entry = link.entry;
            return;
        }

        Pass last = new Pass(pass.vertex, entry, pass.exit, null);
        branch.add(last);
        branch.finish(last);
        sectionIndex = 0;
        cursor = 0;
        entry = original.getFirst().entry;
        lower = upper;
        upper = 1;
        phase = lower == 1 ? Phase.DONE : Phase.WEIGH;
    }

    private enum Phase {
        PARTITION, SOLVE, WEIGH, EMIT, DONE
    }

    private static final class Section {
        private final int start;
        private final int end;
        private final GasMeshRatios core;
        private int choice;

        private Section(int start, int end, GasMeshRatios core) {
            this.start = start;
            this.end = end;
            this.core = core;
        }
    }
}
