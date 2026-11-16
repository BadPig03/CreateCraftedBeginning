package net.ty.createcraftedbeginning.gas.flow;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent.Post;
import net.ty.createcraftedbeginning.api.CCBAPI;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID)
public final class GasFlowEvents {
    private GasFlowEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLevelTick(Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        GasFlowForces.tick(level);
    }

    @SubscribeEvent
    public static void onLevelUnload(Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        GasFlowForces.removeLevel(level);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        GasFlowForces.clear();
    }
}
