package net.ty.createcraftedbeginning.gas.network.solver.routing;

import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasMeshBlocks.Block;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasMeshPlan.BranchBuilder;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneLayout.Link;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneLayout.MeshWork;
import net.ty.createcraftedbeginning.gas.network.solver.routing.GasZoneRoute.Pass;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasMeshSections implements MeshWork {
    private final GasZoneLayout layout;
    private final GasZoneRoute route;
    private final List<Pass> original;
    private final List<Pass> restricted = new ArrayList<>();
    private final GasMeshPlan result = new GasMeshPlan();
    private final List<Section> sections = new ArrayList<>();
    private final Set<Integer> visitedZones = new HashSet<>();
    private @Nullable GasMeshBlocks blocks;
    private @Nullable Block currentBlock;
    private int cursor;
    private int coreStart;
    private int sectionIndex;
    private int linkIndex;
    private Direction entry;
    private double lower;
    private double upper = 1;
    private boolean divided;
    private boolean originalBranch = true;
    private boolean comparing;
    private BranchBuilder branch;
    private Phase phase = Phase.PARTITION;

    GasMeshSections(GasZoneLayout layout, GasZoneRoute route) {
        this.layout = layout;
        this.route = route;
        original = route.passes();
        entry = original.getFirst().entry;
    }

    @Override
    public GasMeshPlan result() {
        return result;
    }

    @Override
    public boolean step() {
        switch (phase) {
            case PARTITION -> partition();
            case SOLVE -> solve();
            case WEIGH -> weigh();
            case EMIT -> emit();
            case REUSE -> {
                branch.add(restricted.get(cursor++));
                if (cursor == restricted.size()) {
                    finish(original.getLast());
                }
            }
        }
        if (phase != Phase.DONE) {
            return false;
        }

        result.finish();
        return true;
    }

    private void partition() {
        Pass pass = original.get(cursor);
        if (!pass.vertex.device && blocks == null) {
            int zone = layout.node(new BlockFace(pass.position(), pass.entry));
            if (zone < 0) {
                return;
            }

            if (!visitedZones.add(zone)) {
                phase = Phase.DONE;
                return;
            }

            blocks = layout.meshBlocks(zone, pass.vertex);
        }

        Link link = pass.next;
        if (blocks != null && link != null && !link.end.device && !blocks.ready()) {
            return;
        }

        Block nextBlock = blocks == null || link == null ? null : blocks.block(link);
        if (nextBlock != currentBlock) {
            if (currentBlock != null) {
                GasMeshRatios core = layout.meshCore(currentBlock, original.get(coreStart).vertex, pass.vertex);
                sections.add(new Section(coreStart, cursor, core));
            }
            currentBlock = nextBlock;
            coreStart = cursor;
        }
        if (pass.vertex.device || link == null || link.end.device) {
            blocks = null;
        }
        if (pass.vertexCapacity() != Long.MAX_VALUE || link != null && link.capacity != Long.MAX_VALUE) {
            restricted.add(pass);
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

    private void weigh() {
        Section section = sections.get(sectionIndex);
        GasMeshRatios core = section.core;
        if (core.size() > 1) {
            if (!comparing) {
                if (core.fraction(section.choice) <= lower) {
                    section.choice++;
                    section.matched = 0;
                    section.different = false;
                }
                upper = Math.min(upper, core.fraction(section.choice));
            }
            if (originalBranch && !section.checkOriginal(original)) {
                comparing = true;
                return;
            }

            comparing = false;
            originalBranch &= !section.different;
        }
        if (++sectionIndex != sections.size()) {
            return;
        }

        sectionIndex = 0;
        branch = result.new BranchBuilder(route, upper - lower, originalBranch);
        if (!originalBranch) {
            phase = Phase.EMIT;
            return;
        }

        if (!restricted.isEmpty()) {
            phase = Phase.REUSE;
            return;
        }

        finish(original.getLast());
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
            branch.add(pass.entry == entry && pass.next == link ? pass : link.meshPass(entry));
            entry = link.entry;
            return;
        }

        Pass last = pass.entry == entry ? pass : new Pass(pass.vertex, entry, pass.exit, null);
        branch.add(last);
        finish(last);
    }

    private void finish(Pass last) {
        branch.finish(last);
        originalBranch = true;
        sectionIndex = 0;
        cursor = 0;
        entry = original.getFirst().entry;
        lower = upper;
        upper = 1;
        phase = lower == 1 ? Phase.DONE : Phase.WEIGH;
    }

    private enum Phase {
        PARTITION, SOLVE, WEIGH, EMIT, REUSE, DONE
    }

    private static final class Section {
        private final int start;
        private final int end;
        private final GasMeshRatios core;
        private int choice;
        private int matched;
        private boolean different;

        private Section(int start, int end, GasMeshRatios core) {
            this.start = start;
            this.end = end;
            this.core = core;
        }

        private boolean checkOriginal(List<Pass> original) {
            int length = end - start;
            if (different || matched == length) {
                return true;
            }

            List<Link> links = core.links(choice);
            if (links.size() != length || links.get(matched) != original.get(start + matched).next) {
                different = true;
                return true;
            }

            return ++matched == length;
        }
    }
}
