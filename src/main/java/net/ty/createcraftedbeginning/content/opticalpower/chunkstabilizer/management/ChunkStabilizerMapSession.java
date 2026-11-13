package net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.management;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.ChunkStabilizerLocation;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.management.ChunkStabilizerSnapshot.Member;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.map.ChunkStabilizerMapCache;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.map.ChunkStabilizerMapChunk;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.map.ChunkStabilizerMapLayer;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.map.ChunkStabilizerMapSelection;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.map.ChunkStabilizerMapSnapshot;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.map.ChunkStabilizerMapSubscription;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.map.ChunkStabilizerMapSubscription.Target;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.map.ChunkStabilizerMapSubscription.Terrain;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.map.ChunkStabilizerMapSubscription.Update;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.map.ChunkStabilizerMapView;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkRuntime.Status;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID)
final class ChunkStabilizerMapSession {
    private static final int POSITION_INTERVAL = 2;
    private static final int TERRAIN_INTERVAL = 4;
    private final ChunkStabilizerMenu menu;
    private final Player player;
    @Nullable
    private UUID selectedMap;
    @Nullable
    private ChunkStabilizerMapView mapView;
    @Nullable
    private ChunkStabilizerMapSubscription mapSubscription;
    private UUID mapRequest = UUID.randomUUID();
    private int mapTicks;
    private boolean mapPending;
    private boolean mapUnavailable;

    ChunkStabilizerMapSession(ChunkStabilizerMenu menu, Player player) {
        this.menu = menu;
        this.player = player;
    }

    @SubscribeEvent
    private static void tick(Post event) {
        MinecraftServer server = event.getServer();
        List<ChunkStabilizerMapSubscription> subscriptions = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!(player.containerMenu instanceof ChunkStabilizerMenu menu) || !menu.stillValid(player)) {
                continue;
            }

            ChunkStabilizerMapSubscription subscription = menu.maps().mapSubscription;
            if (subscription == null) {
                continue;
            }

            subscriptions.add(subscription);
        }
        ChunkStabilizerMapCache.tick(server, subscriptions);
    }

    void tick() {
        if (mapPending) {
            mapPending = false;
            mapTicks = 0;
            synchronizeMap(true);
            return;
        }

        if (++mapTicks % POSITION_INTERVAL != 0) {
            return;
        }

        synchronizeMap(mapTicks % TERRAIN_INTERVAL == 0);
    }

    @Nullable ChunkStabilizerMapSnapshot snapshot() {
        if (mapView == null) {
            return null;
        }

        return mapView.snapshot();
    }

    @Nullable ChunkStabilizerLocation location() {
        if (mapSubscription != null) {
            return mapSubscription.location();
        }

        if (mapView == null) {
            return null;
        }

        return mapView.location();
    }

    @Nullable ChunkStabilizerMapLayer layer() {
        if (mapSubscription == null) {
            return null;
        }

        return mapSubscription.layer();
    }

    List<ChunkStabilizerMapLayer> prepared() {
        if (mapSubscription == null) {
            return List.of();
        }

        return mapSubscription.prepared();
    }

    @Nullable Vec3 point(long nanos) {
        if (mapView == null) {
            return null;
        }

        return mapView.point(nanos);
    }

    @Nullable Vec3 origin(long nanos) {
        if (mapView == null) {
            return null;
        }

        return mapView.origin(nanos);
    }

    UUID request() {
        return mapRequest;
    }

    void select(@Nullable UUID loader) {
        select(loader, UUID.randomUUID());
    }

    void select(@Nullable UUID loader, UUID request) {
        selectedMap = loader;
        mapRequest = request;
        mapView = null;
        mapSubscription = null;
        mapPending = loader != null;
        mapUnavailable = false;
        if (loader == null) {
            return;
        }

        if (player instanceof ServerPlayer) {
            mapSubscription = new ChunkStabilizerMapSubscription();
            return;
        }

        mapView = new ChunkStabilizerMapView(request, loader);
    }

    void accept(ChunkStabilizerMapPositionPacket incoming) {
        if (mapView == null) {
            return;
        }

        mapView.acceptPosition(incoming.request(), incoming.identity(), incoming.generation(), incoming.sequence(), incoming.tick(), incoming.location(), incoming.selection());
    }

    void accept(ChunkStabilizerMapSyncPacket incoming) {
        if (mapView == null) {
            return;
        }

        mapView.acceptTerrain(incoming.request(), incoming.identity(), incoming.generation(), incoming.sequence(), incoming.dimension(), incoming.layer(), incoming.chunks());
    }

    private void synchronizeMap(boolean terrain) {
        UUID identity = selectedMap;
        if (identity == null || mapSubscription == null || !(player instanceof ServerPlayer serverPlayer) || !menu.stillValid(player)) {
            return;
        }

        ServerLevel level = serverPlayer.serverLevel();
        UUID network = menu.snapshot().network();
        if (network == null) {
            return;
        }

        List<CustomPacketPayload> packets = prepareMapPackets(level, network, identity, mapSubscription, terrain);
        for (CustomPacketPayload packet : packets) {
            PacketDistributor.sendToPlayer(serverPlayer, packet);
        }
    }

    private List<CustomPacketPayload> prepareMapPackets(ServerLevel level, UUID network, UUID identity, ChunkStabilizerMapSubscription subscription, boolean terrain) {
        MinecraftServer server = level.getServer();
        try {
            Member target = ChunkStabilizerMembers.find(level, network, identity);
            ChunkStabilizerLocation position = target == null ? null : ChunkStabilizerMapTarget.resolve(server, target);
            ServerLevel source = position == null ? null : server.getLevel(position.dimension());
            if (source == null || source.registryAccess().registry(Registries.BIOME).isPresent()) {
                Target current = target == null || source == null ? null : new Target(source, target.position(), target.train(), target.moving(), target.physical(), target.status() == Status.ASSEMBLED, position.host(), position.part());
                List<CustomPacketPayload> packets = mapPackets(identity, subscription.refresh(server, current, position, terrain));
                mapUnavailable = false;
                return packets;
            }

            if (!mapUnavailable) {
                CCBAPI.LOGGER.warn("Cannot prepare the chunk stabilizer map for loader '{}' in dimension '{}': the biome registry is unavailable.", identity, source.dimension().location());
            }
        }
        catch (RuntimeException failure) {
            if (!mapUnavailable) {
                CCBAPI.LOGGER.warn("Failed to prepare the chunk stabilizer map for loader '{}' in network '{}'; sending an unavailable map.", identity, network, failure);
            }
        }

        mapUnavailable = true;
        return mapPackets(identity, subscription.refresh(server, null, null, false));
    }

    private List<CustomPacketPayload> mapPackets(UUID identity, Update update) {
        long generation = update.generation();
        long sequence = update.sequence();
        ChunkStabilizerLocation location = update.location();
        ChunkStabilizerMapSelection selection = update.selection();
        ChunkStabilizerMapPositionPacket position = new ChunkStabilizerMapPositionPacket(menu.containerId, mapRequest, identity, generation, sequence, update.tick(), location, selection);
        ChunkStabilizerMapLayer layer = selection.current();
        if (location == null || layer == null) {
            return List.of(position);
        }

        List<CustomPacketPayload> packets = new ArrayList<>();
        packets.add(position);
        List<ChunkStabilizerMapChunk> chunks = update.chunks();
        ResourceKey<Level> dimension = location.dimension();
        if (!chunks.isEmpty()) {
            packets.add(new ChunkStabilizerMapSyncPacket(menu.containerId, mapRequest, identity, generation, sequence, dimension, layer, chunks));
        }
        for (Terrain prepared : update.prefetched()) {
            packets.add(new ChunkStabilizerMapSyncPacket(menu.containerId, mapRequest, identity, generation, sequence, dimension, prepared.layer(), prepared.chunks()));
        }
        return packets;
    }
}
