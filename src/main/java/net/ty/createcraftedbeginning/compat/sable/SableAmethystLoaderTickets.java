package net.ty.createcraftedbeginning.compat.sable;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;
import net.neoforged.neoforge.event.tick.LevelTickEvent.Post;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.AmethystLoaderBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID)
final class SableAmethystLoaderTickets {
    private static final TicketController CONTROLLER = new TicketController(CCBAPI.asResource("amethyst_loader_sable"), SableAmethystLoaderTickets::validateTickets);
    private static final Map<ServerLevel, Map<UUID, LongSet>> OWNERS = new HashMap<>();

    private SableAmethystLoaderTickets() {
    }

    static void refresh(ServerLevel level) {
        Object container = SableAmethystLoaderAccess.container(level);
        if (container == null) {
            return;
        }

        boolean enabled = CCBConfig.server().opticalPower.amethystLoader.enableChunkLoading.get();
        LoadingNetworkSavedData data = LoadingNetworkSavedData.get(level);
        Map<UUID, LongSet> targets = new HashMap<>();
        for (Entry<UUID, Object> entry : new ArrayList<>(SableAmethystLoaderAccess.tickets(container).entrySet())) {
            UUID owner = entry.getKey();
            SubLevelAccess subLevel = SableAmethystLoaderAccess.subLevel(container, owner);
            if (subLevel == null) {
                continue;
            }

            for (BlockPos pos : List.copyOf(SableAmethystLoaderAccess.loaders(entry.getValue()))) {
                if (!enabled || SableCompanion.INSTANCE.getContaining(level, pos) != subLevel || !(level.getBlockEntity(pos) instanceof AmethystLoaderBlockEntity loader)) {
                    SableAmethystLoaderAccess.setForced(container, subLevel, pos, false);
                    continue;
                }

                UUID networkNodeId = loader.networkNodeId();
                if (networkNodeId == null || !data.allowsPhysical(networkNodeId) || data.physicalNode(networkNodeId) != null && !SableLoadingNetworkControl.allowsClaim(level, networkNodeId)) {
                    SableAmethystLoaderAccess.setForced(container, subLevel, pos, false);
                    continue;
                }

                loader.refreshLoading();
                long chunk = new ChunkPos(SubLevelBridge.resolve(level, pos).blockPos()).toLong();
                targets.computeIfAbsent(owner, ignored -> new LongOpenHashSet()).add(chunk);
            }
        }
        Map<UUID, LongSet> previous = OWNERS.getOrDefault(level, Map.of());
        for (Entry<UUID, LongSet> entry : previous.entrySet()) {
            LongSet next = targets.get(entry.getKey());
            for (long chunk : entry.getValue()) {
                if (next != null && next.contains(chunk)) {
                    continue;
                }

                CONTROLLER.forceChunk(level, entry.getKey(), ChunkPos.getX(chunk), ChunkPos.getZ(chunk), false, true);
            }
        }
        for (Entry<UUID, LongSet> entry : targets.entrySet()) {
            LongSet old = previous.get(entry.getKey());
            for (long chunk : entry.getValue()) {
                if (old != null && old.contains(chunk)) {
                    continue;
                }

                CONTROLLER.forceChunk(level, entry.getKey(), ChunkPos.getX(chunk), ChunkPos.getZ(chunk), true, true);
            }
        }
        OWNERS.put(level, targets);
    }

    static void register() {
        SableAmethystLoaderAccess.initialize();
        AmethystLoaderBlockEntity.setSubLevelTicker(SableAmethystLoaderTickets::claim);
        NeoForge.EVENT_BUS.addListener(SableAmethystLoaderTickets::onTick);
        NeoForge.EVENT_BUS.addListener(SableAmethystLoaderTickets::onUnload);
    }

    private static boolean claim(AmethystLoaderBlockEntity loader) {
        if (!(loader.getLevel() instanceof ServerLevel level)) {
            return false;
        }

        BlockPos pos = loader.getBlockPos();
        SubLevelAccess subLevel = SableCompanion.INSTANCE.getContaining(level, pos);
        Object container = SableAmethystLoaderAccess.container(level);
        if (subLevel == null || container == null) {
            return false;
        }

        LoadingNetworkSavedData data = LoadingNetworkSavedData.get(level);
        UUID networkNodeId = loader.networkNodeId();
        if (!CCBConfig.server().opticalPower.amethystLoader.enableChunkLoading.get() || networkNodeId == null || !data.allowsPhysical(networkNodeId) || data.isPlacing(GlobalPos.of(level.dimension(), pos)) || data.physicalNode(networkNodeId) != null && !SableLoadingNetworkControl.allowsClaim(level, networkNodeId)) {
            SableAmethystLoaderAccess.setForced(container, subLevel, pos, false);
            return false;
        }

        Object info = SableAmethystLoaderAccess.tickets(container).get(subLevel.getUniqueId());
        if (info == null || !SableAmethystLoaderAccess.loaders(info).contains(pos)) {
            SableAmethystLoaderAccess.setForced(container, subLevel, pos, true);
        }
        if (data.physicalNode(loader.networkNodeId()) != null) {
            return level.isPositionEntityTicking(SubLevelBridge.resolve(level, pos).blockPos());
        }

        return true;
    }

    @SubscribeEvent
    private static void registerController(RegisterTicketControllersEvent event) {
        event.register(CONTROLLER);
    }

    private static void onTick(Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        refresh(level);
    }

    private static void onUnload(Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            OWNERS.remove(level);
        }
    }

    private static void validateTickets(ServerLevel level, TicketHelper helper) {
        for (BlockPos owner : helper.getBlockTickets().keySet()) {
            helper.removeAllTickets(owner);
        }
        for (UUID owner : helper.getEntityTickets().keySet()) {
            helper.removeAllTickets(owner);
        }
    }
}
