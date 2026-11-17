package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;
import net.neoforged.neoforge.event.tick.LevelTickEvent.Post;
import net.ty.createcraftedbeginning.api.CCBAPI;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID)
final class TeslaTurbineKineticUpdates {
    private static final Map<ServerLevel, LinkedHashSet<TeslaTurbineBlockEntity>> PENDING = new IdentityHashMap<>();

    private TeslaTurbineKineticUpdates() {
    }

    static void schedule(TeslaTurbineBlockEntity turbine) {
        if (!(turbine.getLevel() instanceof ServerLevel level) || turbine.isRemoved()) {
            return;
        }

        PENDING.computeIfAbsent(level, ignored -> new LinkedHashSet<>()).add(turbine);
    }

    @SubscribeEvent
    private static void onLevelTick(Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        LinkedHashSet<TeslaTurbineBlockEntity> pending = PENDING.get(level);
        if (pending == null) {
            return;
        }

        while (!pending.isEmpty()) {
            Iterator<TeslaTurbineBlockEntity> iterator = pending.iterator();
            TeslaTurbineBlockEntity turbine = iterator.next();
            iterator.remove();
            BlockPos pos = turbine.getBlockPos();
            if (turbine.isRemoved() || !level.isLoaded(pos) || level.getBlockEntity(pos) != turbine) {
                continue;
            }

            turbine.applyGeneratedRotation();
        }
        PENDING.remove(level);
    }

    @SubscribeEvent
    private static void onLevelUnload(Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        PENDING.remove(level);
    }
}
