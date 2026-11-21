package net.ty.createcraftedbeginning.gas.network.solver;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.ty.createcraftedbeginning.gas.network.solver.GasPipeLinkIndex.Links;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasNetworkState {
    private final long version;
    private final Map<BlockPos, Links> pipes;
    private final Map<BlockPos, BlockPos> zones = new HashMap<>();
    private final List<BlockPos> devicePositions;
    private long lastSimulationTick;
    private boolean current = true;
    private final boolean cycle;
    private final boolean assistancePump;

    GasNetworkState(long version, Set<BlockPos> positions, Map<BlockPos, Links> pipes, long lastSimulationTick) {
        this.version = version;
        this.pipes = pipes;
        this.lastSimulationTick = lastSimulationTick;
        List<BlockPos> devices = new ArrayList<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> members = new ArrayList<>();
        long edges = 0;
        boolean pump = false;
        for (BlockPos pos : positions) {
            Links links = pipes.get(pos);
            if (links != null) {
                edges += Integer.bitCount(links.neighbors());
                pump |= links.assistancePump();
            }
            if (links == null || !links.ordinary()) {
                devices.add(pos);
                continue;
            }

            if (zones.containsKey(pos)) {
                continue;
            }

            BlockPos anchor = pos;
            queue.add(pos);
            zones.put(pos, pos);
            members.clear();
            while (!queue.isEmpty()) {
                BlockPos member = queue.removeFirst();
                members.add(member);
                if (GasNetworkTopology.compareBlockPositions(member, anchor) < 0) {
                    anchor = member;
                }
                int neighbors = pipes.get(member).neighbors();
                for (Direction side : Iterate.directions) {
                    if ((neighbors & 1 << side.ordinal()) == 0) {
                        continue;
                    }

                    BlockPos next = member.relative(side);
                    Links other = pipes.get(next);
                    if (other == null || !other.ordinary() || (other.neighbors() & 1 << side.getOpposite().ordinal()) == 0 || zones.putIfAbsent(next, pos) != null) {
                        continue;
                    }

                    queue.addLast(next);
                }
            }
            for (BlockPos member : members) {
                zones.put(member, anchor);
            }
        }
        cycle = edges / 2 >= positions.size();
        assistancePump = pump;
        devicePositions = List.copyOf(devices);
    }

    public List<BlockPos> devicePositions() {
        return devicePositions;
    }

    public boolean hasCycle() {
        return cycle;
    }

    public boolean hasAssistancePump() {
        return assistancePump;
    }

    @Nullable
    public BlockPos pressureZoneAt(BlockPos pos) {
        return zones.get(pos);
    }

    public boolean isCurrent() {
        return current;
    }

    @Nullable
    public PipeLinks linksAt(BlockPos pos) {
        return pipes.get(pos);
    }

    public int ordinaryNeighbors(BlockPos pos) {
        Links links = pipes.get(pos);
        if (links == null || !links.ordinary()) {
            return 0;
        }

        return links.neighbors();
    }

    long version() {
        return version;
    }

    boolean simulatedAt(long tick) {
        return lastSimulationTick == tick;
    }

    void markSimulated(long tick) {
        lastSimulationTick = tick;
    }

    void retire() {
        current = false;
    }

    public interface PipeLinks {
        int neighbors();

        int endpoints();

        int atmosphere();

        boolean ordinary();
    }
}
