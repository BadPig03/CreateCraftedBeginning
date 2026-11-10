package net.ty.createcraftedbeginning.content.opticalpower.amethystloader;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;
import net.neoforged.neoforge.common.world.chunk.TicketSet;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkMoving;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map.Entry;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID)
final class AmethystLoaderTickets {
    private static final TicketController CONTROLLER = new TicketController(CCBAPI.asResource("amethyst_loader"), AmethystLoaderTickets::validateTickets);

    private AmethystLoaderTickets() {
    }

    static void setForced(ServerLevel level, BlockPos owner, boolean forced) {
        ChunkPos chunk = new ChunkPos(owner);
        CONTROLLER.forceChunk(level, owner, chunk.x, chunk.z, forced, true);
    }

    static void setForced(ServerLevel level, UUID owner, long chunk, boolean forced) {
        CONTROLLER.forceChunk(level, owner, ChunkPos.getX(chunk), ChunkPos.getZ(chunk), forced, true);
    }

    @SubscribeEvent
    private static void register(RegisterTicketControllersEvent event) {
        event.register(CONTROLLER);
    }

    private static void validateTickets(ServerLevel level, TicketHelper helper) {
        boolean enabled = CCBConfig.server().opticalPower.amethystLoader.enableChunkLoading.get();
        for (Entry<BlockPos, TicketSet> entry : helper.getBlockTickets().entrySet()) {
            BlockPos owner = entry.getKey();
            if (!enabled || SubLevelBridge.resolve(level, owner).inSubLevel() || !(level.getBlockEntity(owner) instanceof AmethystLoaderBlockEntity loader) || !loader.reconcileFixedLoading()) {
                helper.removeAllTickets(owner);
                continue;
            }

            TicketSet tickets = entry.getValue();
            long currentChunk = new ChunkPos(owner).toLong();
            for (long chunk : tickets.ticking()) {
                if (chunk == currentChunk) {
                    continue;
                }

                helper.removeTicket(owner, chunk, true);
            }
            for (long chunk : tickets.nonTicking()) {
                helper.removeTicket(owner, chunk, false);
            }
        }
        for (Entry<UUID, TicketSet> entry : helper.getEntityTickets().entrySet()) {
            UUID owner = entry.getKey();
            LoadingNetworkMoving moving = LoadingNetworkSavedData.get(level).moving();
            if (!enabled || moving.hasHost(owner) && !moving.demanded(owner)) {
                helper.removeAllTickets(owner);
                continue;
            }

            TicketSet tickets = entry.getValue();
            for (long chunk : tickets.nonTicking()) {
                helper.removeTicket(owner, chunk, false);
            }
            AmethystLoaderMovingTickets.restore(level, owner, tickets.ticking());
        }
    }
}
