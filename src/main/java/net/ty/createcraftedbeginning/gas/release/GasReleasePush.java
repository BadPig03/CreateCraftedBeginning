package net.ty.createcraftedbeginning.gas.release;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseContext;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.config.CCBGas.EntityPush;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasReleasePush {
    private static final Map<ServerLevel, Map<Outlet, GasReleasePushSource>> PENDING = new HashMap<>();

    private GasReleasePush() {
    }

    static void submit(GasReleaseContext context, boolean continuous) {
        if (!(context.level() instanceof ServerLevel level) || !CCBConfig.server().gas.entityPush.enableEntityPush.get()) {
            return;
        }

        Map<Outlet, GasReleasePushSource> sources = PENDING.computeIfAbsent(level, ignored -> new LinkedHashMap<>());
        Outlet outlet = new Outlet(context.sourcePos(), context.direction(), continuous);
        sources.computeIfAbsent(outlet, ignored -> new GasReleasePushSource(context, continuous)).add(context);
    }

    static void tick(ServerLevel level) {
        Map<Outlet, GasReleasePushSource> sources = PENDING.remove(level);
        EntityPush config = CCBConfig.server().gas.entityPush;
        double multiplier = config.strengthMultiplier.getF();
        if (sources == null || !config.enableEntityPush.get() || multiplier <= 0) {
            return;
        }

        Map<Entity, GasReleasePushTarget> targets = new LinkedHashMap<>();
        for (GasReleasePushSource source : sources.values()) {
            source.collect(level, multiplier, targets);
        }
        for (Entry<Entity, GasReleasePushTarget> entry : targets.entrySet()) {
            entry.getValue().apply(entry.getKey());
        }
    }

    static void removeLevel(ServerLevel level) {
        PENDING.remove(level);
    }

    static void clear() {
        PENDING.clear();
    }

    private record Outlet(BlockPos pos, Optional<Direction> direction, boolean continuous) {}
}
