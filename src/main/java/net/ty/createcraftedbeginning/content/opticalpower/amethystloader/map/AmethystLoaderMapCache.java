package net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.AmethystLoaderLocation;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map.AmethystLoaderMapChunk.Version;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map.AmethystLoaderMapSampler.Sample;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.map.AmethystLoaderMapSnapshot.Column;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Internal
@EventBusSubscriber(modid = CCBAPI.MOD_ID)
public final class AmethystLoaderMapCache {
    private static final int MAX_TILES = 2048;
    private static final int IDLE_TICKS = 1200;
    private static final int MAX_COLUMNS_PER_TICK = 2048;
    private static final long MAX_SAMPLE_NANOS = 2000000;
    private static final int VIEW_QUANTUM = 16;
    private static final int VOLATILE_INTERVAL = 20;
    private static final Map<MinecraftServer, AmethystLoaderMapCache> SERVERS = new IdentityHashMap<>();
    private final Map<Key, Tile> tiles = new LinkedHashMap<>(128, 0.75F, true);
    private int nextView;

    private AmethystLoaderMapCache() {
    }

    public static void tick(MinecraftServer server, List<AmethystLoaderMapSubscription> subscriptions) {
        AmethystLoaderMapCache cache = SERVERS.get(server);
        if (cache == null) {
            return;
        }

        List<List<Key>> views = new ArrayList<>();
        for (AmethystLoaderMapSubscription maps : subscriptions) {
            AmethystLoaderLocation position = maps.location();
            ServerLevel level = position == null ? null : server.getLevel(position.dimension());
            if (level == null) {
                continue;
            }

            BlockPos center = position.blockPosition().pos();
            AmethystLoaderMapLayer layer = maps.layer();
            if (layer == null) {
                continue;
            }

            int centerX = center.getX();
            int centerZ = center.getZ();
            Set<Long> visible = new HashSet<>(AmethystLoaderMapSnapshot.window(center));
            List<Key> window = new ArrayList<>();
            for (long chunk : AmethystLoaderMapSnapshot.bufferedWindow(center)) {
                window.add(new Key(level, chunk, layer));
            }
            window.sort(Comparator.comparing((Key key) -> !visible.contains(key.chunk())).thenComparingInt(key -> {
                long chunk = key.chunk();
                return Math.abs(ChunkPos.getX(chunk) - Math.floorDiv(centerX, 16)) + Math.abs(ChunkPos.getZ(chunk) - Math.floorDiv(centerZ, 16));
            }));
            List<Key> current = List.copyOf(window);
            for (AmethystLoaderMapLayer prepared : maps.prepared()) {
                if (prepared.equals(layer)) {
                    continue;
                }

                for (Key key : current) {
                    window.add(new Key(level, key.chunk(), prepared));
                }
            }
            views.add(window);
        }
        cache.sample(views);
        int tick = server.getTickCount();
        cache.tiles.entrySet().removeIf(entry -> tick - entry.getValue().lastUse >= IDLE_TICKS);
    }

    static @Nullable AmethystLoaderMapChunk read(ServerLevel level, long chunk, AmethystLoaderMapLayer layer, @Nullable Version known) {
        return read(level, chunk, layer, known, false);
    }

    static @Nullable AmethystLoaderMapChunk read(ServerLevel level, long chunk, AmethystLoaderMapLayer layer, @Nullable Version known, boolean complete) {
        AmethystLoaderMapCache cache = SERVERS.computeIfAbsent(level.getServer(), ignored -> new AmethystLoaderMapCache());
        Tile tile = cache.find(new Key(level, chunk, layer));
        if (tile == null) {
            if (known == null) {
                return null;
            }

            return new AmethystLoaderMapChunk(chunk, null, List.of());
        }

        Version version = new Version(tile.incarnation, tile.revision);
        if (version.equals(known)) {
            return null;
        }

        if (complete && !tile.dirty.isEmpty()) {
            return null;
        }

        return new AmethystLoaderMapChunk(chunk, version, Arrays.asList(tile.columns));
    }

    static void discardUnloadedChunks(ServerLevel level, Set<Long> knownChunks) {
        AmethystLoaderMapCache cache = SERVERS.get(level.getServer());
        if (cache == null) {
            return;
        }

        ServerChunkCache source = level.getChunkSource();
        Set<Long> unavailable = new HashSet<>();
        for (long chunk : knownChunks) {
            if (source.getChunkNow(ChunkPos.getX(chunk), ChunkPos.getZ(chunk)) != null) {
                continue;
            }

            unavailable.add(chunk);
        }
        if (unavailable.isEmpty()) {
            return;
        }

        cache.tiles.keySet().removeIf(key -> key.level() == level && unavailable.contains(key.chunk()));
    }

    @SubscribeEvent
    private static void unload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        AmethystLoaderMapCache cache = SERVERS.get(level.getServer());
        if (cache == null) {
            return;
        }

        long chunk = event.getChunk().getPos().toLong();
        cache.tiles.keySet().removeIf(key -> key.level() == level && key.chunk() == chunk);
    }

    @SubscribeEvent
    private static void unload(Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        AmethystLoaderMapCache cache = SERVERS.get(level.getServer());
        if (cache == null) {
            return;
        }

        cache.tiles.keySet().removeIf(key -> key.level() == level);
    }

    @SubscribeEvent
    private static void stopped(ServerStoppedEvent event) {
        SERVERS.remove(event.getServer());
    }

    @SubscribeEvent
    private static void reload(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            return;
        }

        SERVERS.remove(event.getPlayerList().getServer());
    }

    @Nullable
    private Tile find(Key key) {
        ServerLevel level = key.level();
        long position = key.chunk();
        LevelChunk chunk = level.getChunkSource().getChunkNow(ChunkPos.getX(position), ChunkPos.getZ(position));
        if (chunk == null) {
            tiles.remove(key);
            return null;
        }

        Tile tile = tiles.get(key);
        if (tile == null || tile.chunk.get() != chunk) {
            tile = new Tile(chunk);
            tiles.put(key, tile);
            while (tiles.size() > MAX_TILES) {
                Iterator<Key> oldest = tiles.keySet().iterator();
                oldest.next();
                oldest.remove();
            }
        }
        tile.refresh(chunk, level.getServer().getTickCount());
        return tile;
    }

    private void sample(List<List<Key>> views) {
        if (views.isEmpty()) {
            return;
        }

        long deadline = System.nanoTime() + MAX_SAMPLE_NANOS;
        int remaining = MAX_COLUMNS_PER_TICK;
        int[] cursors = new int[views.size()];
        int exhausted = 0;
        MutableBlockPos cursor = new MutableBlockPos();
        while (remaining > 0 && exhausted < views.size() && System.nanoTime() < deadline) {
            int view = Math.floorMod(nextView++, views.size());
            List<Key> window = views.get(view);
            if (cursors[view] >= window.size()) {
                exhausted++;
                continue;
            }

            exhausted = 0;
            int quantum = Math.min(VIEW_QUANTUM, remaining);
            while (quantum > 0 && cursors[view] < window.size() && System.nanoTime() < deadline) {
                Key key = window.get(cursors[view]);
                Tile tile = find(key);
                LevelChunk chunk = tile == null ? null : tile.chunk.get();
                if (tile == null || chunk == null || tile.dirty.isEmpty()) {
                    cursors[view]++;
                    continue;
                }

                int index = tile.dirty.nextSetBit(0);
                ServerLevel level = key.level();
                ChunkPos position = chunk.getPos();
                Registry<Biome> biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
                Sample result = AmethystLoaderMapSampler.sampleColumn(level, chunk, biomes, cursor, position.getMinBlockX() + (index & 15), position.getMinBlockZ() + (index >> 4), key.layer());
                Column column = result.column();
                if (!column.equals(tile.columns[index])) {
                    tile.columns[index] = column;
                    tile.revision++;
                }
                tile.volatileColumns.set(index, !result.cacheable());
                tile.dirty.clear(index);
                quantum--;
                remaining--;
            }
        }
    }

    private record Key(ServerLevel level, long chunk, AmethystLoaderMapLayer layer) {
    }

    private static final class Tile {
        private final WeakReference<LevelChunk> chunk;
        private final UUID incarnation = UUID.randomUUID();
        private final Column[] columns = new Column[256];
        private final BitSet dirty = new BitSet(256);
        private final BitSet volatileColumns = new BitSet(256);
        private final AmethystLoaderMapChanges[] sections;
        private long revision;
        private int lastUse;
        private int volatileTick;

        private Tile(LevelChunk chunk) {
            this.chunk = new WeakReference<>(chunk);
            sections = new AmethystLoaderMapChanges[chunk.getSectionsCount()];
            Arrays.fill(columns, Column.UNKNOWN);
            dirty.set(0, 256);
        }

        private void refresh(LevelChunk chunk, int tick) {
            lastUse = tick;
            LevelChunkSection[] current = chunk.getSections();
            for (int index = 0; index < current.length; index++) {
                AmethystLoaderMapChanges changes = ((AmethystLoaderMapSectionAccess) current[index]).ccb$getMapChanges();
                if (sections[index] == changes) {
                    continue;
                }

                sections[index] = changes;
                dirty.set(0, 256);
                changes.watch(dirty);
            }
            if (tick / VOLATILE_INTERVAL == volatileTick) {
                return;
            }

            volatileTick = tick / VOLATILE_INTERVAL;
            dirty.or(volatileColumns);
        }
    }
}
