package net.ty.createcraftedbeginning.gas.network.solver;

import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasServiceOrder {
    private static final int MAX_SERVICE_HINTS = 1024;
    private static final int MAX_TRACKED_NETWORKS = 16;
    private static final int MAX_TRACKED_OUTLETS = MAX_SERVICE_HINTS / MAX_TRACKED_NETWORKS;
    private final LinkedHashMap<BlockFace, LinkedHashMap<BlockFace, Long>> outletTurns = new LinkedHashMap<>();
    private final LinkedHashMap<BlockFace, BlockFace> targets = new LinkedHashMap<>();
    private final LinkedHashMap<BlockFace, BlockFace> sources = new LinkedHashMap<>();

    private final LinkedHashMap<BlockFace, BlockFace> outlets = new LinkedHashMap<>();
    private final LinkedHashMap<BlockFace, BlockFace> nearby = new LinkedHashMap<>();
    private final LinkedHashMap<BlockFace, BlockFace> supplies = new LinkedHashMap<>();

    private static void remember(LinkedHashMap<BlockFace, BlockFace> order, BlockFace key, BlockFace value) {
        order.putLast(key, value);
        if (order.size() > MAX_SERVICE_HINTS) {
            order.pollFirstEntry();
        }
    }

    @Nullable
    BlockFace lastTarget(BlockFace source) {
        return targets.get(source);
    }

    @Nullable
    BlockFace lastSource(BlockFace anchor) {
        return sources.get(anchor);
    }

    @Nullable
    BlockFace supply(BlockFace anchor) {
        return supplies.get(anchor);
    }

    @Nullable
    BlockFace nextOutlet(BlockFace anchor) {
        return outlets.get(anchor);
    }

    void nextOutlet(BlockFace anchor, BlockFace next) {
        remember(outlets, anchor, next);
    }

    @Nullable
    BlockFace nearby(BlockFace source) {
        return nearby.get(source);
    }

    void servedOutlet(BlockFace anchor, BlockFace target, long tick) {
        LinkedHashMap<BlockFace, Long> turns = outletTurns.computeIfAbsent(anchor, ignored -> new LinkedHashMap<>());
        turns.putLast(target, tick);
        if (turns.size() > MAX_TRACKED_OUTLETS) {
            turns.pollFirstEntry();
        }
        outletTurns.putLast(anchor, turns);
        if (outletTurns.size() <= MAX_TRACKED_NETWORKS) {
            return;
        }

        outletTurns.pollFirstEntry();
    }

    @Nullable
    BlockFace dueOutlet(BlockFace anchor, long tick) {
        LinkedHashMap<BlockFace, Long> turns = outletTurns.get(anchor);
        if (turns == null) {
            return null;
        }

        Entry<BlockFace, Long> first = turns.firstEntry();
        if (first == null || tick - first.getValue() < Math.max(4, turns.size() / 3)) {
            return null;
        }

        BlockFace target = first.getKey();
        turns.putLast(target, tick);
        return target;
    }

    void supply(BlockFace anchor, BlockFace source, BlockFace target) {
        remember(supplies, anchor, source);
        BlockFace previous = nearby.get(source);
        if (previous == null || source.getPos().distManhattan(target.getPos()) <= source.getPos().distManhattan(previous.getPos())) {
            remember(nearby, source, target);
        }
    }

    void served(BlockFace source, BlockFace target) {
        remember(targets, source, target);
    }

    void servedFrom(BlockFace anchor, BlockFace source) {
        remember(sources, anchor, source);
    }
}
