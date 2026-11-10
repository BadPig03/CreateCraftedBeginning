package net.ty.createcraftedbeginning.content.opticalpower.amethystloader.management;

import net.minecraft.MethodsReturnNonnullByDefault;
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
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.AmethystLoaderLocation;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.management.AmethystLoaderSnapshot.Member;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map.AmethystLoaderMapCache;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map.AmethystLoaderMapChunk;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map.AmethystLoaderMapLayer;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map.AmethystLoaderMapSelection;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map.AmethystLoaderMapSnapshot;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map.AmethystLoaderMapSubscription;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map.AmethystLoaderMapSubscription.Target;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map.AmethystLoaderMapSubscription.Terrain;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map.AmethystLoaderMapSubscription.Update;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map.AmethystLoaderMapView;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkRuntime.Status;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID)
final class AmethystLoaderMapSession {
    private static final int POSITION_INTERVAL = 2;
    private static final int TERRAIN_INTERVAL = 4;
    private final AmethystLoaderMenu menu;
    private final Player player;
    @Nullable
    private UUID selectedMap;
    @Nullable
    private AmethystLoaderMapView mapView;
    @Nullable
    private AmethystLoaderMapSubscription mapSubscription;
    private UUID mapRequest = UUID.randomUUID();
    private int mapTicks;
    private boolean mapPending;

    AmethystLoaderMapSession(AmethystLoaderMenu menu, Player player) {
        this.menu = menu;
        this.player = player;
    }

    @SubscribeEvent
    private static void tick(Post event) {
        MinecraftServer server = event.getServer();
        List<AmethystLoaderMapSubscription> subscriptions = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!(player.containerMenu instanceof AmethystLoaderMenu menu) || !menu.stillValid(player)) {
                continue;
            }

            AmethystLoaderMapSubscription subscription = menu.maps().mapSubscription;
            if (subscription == null) {
                continue;
            }

            subscriptions.add(subscription);
        }
        AmethystLoaderMapCache.tick(server, subscriptions);
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

    @Nullable AmethystLoaderMapSnapshot snapshot() {
        if (mapView == null) {
            return null;
        }

        return mapView.snapshot();
    }

    @Nullable AmethystLoaderLocation location() {
        if (mapSubscription != null) {
            return mapSubscription.location();
        }

        if (mapView == null) {
            return null;
        }

        return mapView.location();
    }

    @Nullable AmethystLoaderMapLayer layer() {
        if (mapSubscription == null) {
            return null;
        }

        return mapSubscription.layer();
    }

    List<AmethystLoaderMapLayer> prepared() {
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
        if (loader == null) {
            return;
        }

        if (player instanceof ServerPlayer) {
            mapSubscription = new AmethystLoaderMapSubscription();
            return;
        }

        mapView = new AmethystLoaderMapView(request, loader);
    }

    void accept(AmethystLoaderMapPositionPacket incoming) {
        if (mapView == null) {
            return;
        }

        mapView.acceptPosition(incoming.request(), incoming.identity(), incoming.generation(), incoming.sequence(), incoming.tick(), incoming.location(), incoming.selection());
    }

    void accept(AmethystLoaderMapSyncPacket incoming) {
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

        Member target = AmethystLoaderMembers.find(level, network, identity);
        MinecraftServer server = level.getServer();
        AmethystLoaderLocation position = target == null ? null : AmethystLoaderMapTarget.resolve(server, target);
        ServerLevel source = position == null ? null : server.getLevel(position.dimension());
        Target current = target == null || source == null ? null : new Target(source, target.position(), target.train(), target.moving(), target.physical(), target.status() == Status.ASSEMBLED, position.host(), position.part());
        Update update = mapSubscription.refresh(server, current, position, terrain);
        long generation = update.generation();
        long sequence = update.sequence();
        AmethystLoaderLocation location = update.location();
        AmethystLoaderMapSelection selection = update.selection();
        PacketDistributor.sendToPlayer(serverPlayer, new AmethystLoaderMapPositionPacket(menu.containerId, mapRequest, identity, generation, sequence, update.tick(), location, selection));
        AmethystLoaderMapLayer layer = selection.current();
        if (location == null || layer == null) {
            return;
        }

        List<AmethystLoaderMapChunk> chunks = update.chunks();
        ResourceKey<Level> dimension = location.dimension();
        if (!chunks.isEmpty()) {
            PacketDistributor.sendToPlayer(serverPlayer, new AmethystLoaderMapSyncPacket(menu.containerId, mapRequest, identity, generation, sequence, dimension, layer, chunks));
        }
        for (Terrain prepared : update.prefetched()) {
            PacketDistributor.sendToPlayer(serverPlayer, new AmethystLoaderMapSyncPacket(menu.containerId, mapRequest, identity, generation, sequence, dimension, prepared.layer(), prepared.chunks()));
        }
    }
}
