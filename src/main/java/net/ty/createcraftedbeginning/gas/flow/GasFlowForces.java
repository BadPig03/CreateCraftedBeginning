package net.ty.createcraftedbeginning.gas.flow;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseArea;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseContext;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseHandlers;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.config.CCBGas.EntityPush;
import net.ty.createcraftedbeginning.gas.visual.GasIntakeOutlinePacket;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasFlowForces {
    private static final Map<ServerLevel, Pending> PENDING = new HashMap<>();
    private static final Map<Level, Set<ItemEntity>> POSITION_SYNC = new HashMap<>();

    private GasFlowForces() {
    }

    public static void release(GasReleaseContext context, boolean continuous) {
        if (!(context.level() instanceof ServerLevel level) || !CCBConfig.server().gas.entityPush.enableEntityPush.get()) {
            return;
        }

        Pending pending = PENDING.computeIfAbsent(level, ignored -> new Pending());
        Outlet outlet = new Outlet(context.sourcePos(), context.direction(), continuous);
        pending.releases.computeIfAbsent(outlet, ignored -> new GasReleaseFlow(context, continuous)).add(context);
    }

    public static void intake(Level level, BlockPos pos, Direction direction, long amount) {
        if (!(level instanceof ServerLevel serverLevel) || amount <= 0 || !CCBConfig.server().gas.intakeSuction.enableIntakeSuction.get()) {
            return;
        }

        Pending pending = PENDING.computeIfAbsent(serverLevel, ignored -> new Pending());
        Inlet inlet = new Inlet(pos.immutable(), direction);
        pending.intakes.computeIfAbsent(inlet, ignored -> new GasIntakeFlow(level, pos, direction)).add(amount);
    }

    public static boolean shouldSyncItemPosition(Entity entity) {
        Set<ItemEntity> items = POSITION_SYNC.get(entity.level());
        return items != null && items.contains(entity);
    }

    public static void outline(GasReleaseContext context) {
        if (!(context.level() instanceof ServerLevel level)) {
            return;
        }

        Pending pending = PENDING.computeIfAbsent(level, ignored -> new Pending());
        Outlet outlet = new Outlet(context.sourcePos(), context.direction(), false);
        GasReleaseArea area = context.releaseArea();
        Outline previous = pending.outlines.get(outlet);
        if (previous != null && previous.area().sideLength() > area.sideLength()) {
            return;
        }

        pending.outlines.put(outlet, new Outline(area, context.gas().getHint()));
    }

    static void synchronizePosition(ItemEntity item) {
        POSITION_SYNC.computeIfAbsent(item.level(), ignored -> new HashSet<>()).add(item);
    }

    static void tick(ServerLevel level) {
        POSITION_SYNC.remove(level);
        Pending pending = PENDING.remove(level);
        if (pending == null) {
            return;
        }

        Map<Entity, GasFlowTarget> targets = new LinkedHashMap<>();
        EntityPush push = CCBConfig.server().gas.entityPush;
        double multiplier = push.strengthMultiplier.getF();
        if (push.enableEntityPush.get() && multiplier > 0) {
            for (GasReleaseFlow source : pending.releases.values()) {
                source.collect(level, multiplier, targets);
            }
        }
        if (CCBConfig.server().gas.intakeSuction.enableIntakeSuction.get()) {
            for (Entry<Inlet, GasIntakeFlow> entry : pending.intakes.entrySet()) {
                entry.getValue().collect(level, targets);
                Inlet inlet = entry.getKey();
                GasIntakeOutlinePacket.send(level, inlet.pos(), inlet.direction());
            }
        }
        for (Entry<Entity, GasFlowTarget> entry : targets.entrySet()) {
            entry.getValue().apply(entry.getKey());
        }
        for (Entry<Outlet, Outline> entry : pending.outlines.entrySet()) {
            Outlet outlet = entry.getKey();
            Outline outline = entry.getValue();
            GasReleaseHandlers.showOutline(level, outlet.pos(), outlet.direction(), outline.area(), outline.color());
        }
    }

    static void removeLevel(ServerLevel level) {
        PENDING.remove(level);
        POSITION_SYNC.remove(level);
    }

    static void clear() {
        PENDING.clear();
        POSITION_SYNC.clear();
    }

    private record Outlet(BlockPos pos, Optional<Direction> direction, boolean continuous) {}

    private record Inlet(BlockPos pos, Direction direction) {}

    private record Outline(GasReleaseArea area, int color) {}

    private static final class Pending {
        private final Map<Outlet, GasReleaseFlow> releases = new LinkedHashMap<>();
        private final Map<Inlet, GasIntakeFlow> intakes = new LinkedHashMap<>();
        private final Map<Outlet, Outline> outlines = new LinkedHashMap<>();
    }
}
