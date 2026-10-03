package net.ty.createcraftedbeginning.content.opticalpower.network;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ChunkEvent.Load;
import net.neoforged.neoforge.event.level.ChunkTicketLevelUpdatedEvent;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent.Post;
import net.ty.createcraftedbeginning.api.CCBAPI;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID)
public final class OpticalPowerNetworkEvents {
    private OpticalPowerNetworkEvents() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        OpticalPowerNetworkManager.clear();
    }

    @SubscribeEvent
    public static void onLevelTick(Post event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        OpticalPowerNetworkManager.tick(serverLevel);
    }

    @SubscribeEvent
    public static void onChunkLoad(Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        OpticalPowerNetworkManager.onChunkAccessibilityChanged(serverLevel, event.getChunk().getPos());
    }

    @SubscribeEvent
    public static void onChunkTicketLevelUpdated(ChunkTicketLevelUpdatedEvent event) {
        int accessibleLevel = ChunkLevel.byStatus(ChunkStatus.FULL);
        boolean wasAccessible = event.getOldTicketLevel() <= accessibleLevel;
        boolean isAccessible = event.getNewTicketLevel() <= accessibleLevel;
        if (wasAccessible == isAccessible) {
            return;
        }

        OpticalPowerNetworkManager.queueChunkAccessibilityChange(event.getLevel(), new ChunkPos(event.getChunkPos()));
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        OpticalPowerNetworkManager.onChunkAccessibilityChanged(serverLevel, event.getChunk().getPos());
    }

    @SubscribeEvent
    public static void onLevelUnload(Unload event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        OpticalPowerNetworkManager.removeLevel(serverLevel);
    }
}
