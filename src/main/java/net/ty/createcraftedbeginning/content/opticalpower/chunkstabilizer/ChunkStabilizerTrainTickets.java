package net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer;

import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;
import net.neoforged.neoforge.common.world.chunk.TicketSet;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.ChunkStabilizerTrainPositions.Position;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkRuntime;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkRuntime.Status;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID)
final class ChunkStabilizerTrainTickets {
    private static final int RECOVERY_TICKS = 200;
    private static final TicketController CONTROLLER = new TicketController(CCBAPI.asResource("chunk_stabilizer_train"), ChunkStabilizerTrainTickets::validateTickets);
    private static final Map<ServerLevel, Map<UUID, LongSet>> OWNERS = new HashMap<>();
    private static final Map<Loader, Gate> GATES = new HashMap<>();

    private ChunkStabilizerTrainTickets() {
    }

    @SubscribeEvent
    private static void register(RegisterTicketControllersEvent event) {
        event.register(CONTROLLER);
    }

    @SubscribeEvent
    private static void onTick(Post event) {
        MinecraftServer server = event.getServer();
        Map<ServerLevel, Map<UUID, LongSet>> targets = new HashMap<>();
        Set<Carriage> present = new HashSet<>();
        Set<Loader> presentGates = new HashSet<>();
        LoadingNetworkSavedData data = LoadingNetworkSavedData.get(server.overworld());
        LoadingNetworkRuntime runtime = data.runtime();
        Set<UUID> retries = runtime.takeTrainRetries();
        Map<UUID, Status> states = new HashMap<>();
        boolean enabled = CCBConfig.server().opticalPower.chunkStabilizer.enableChunkLoading.get();
        for (Train train : Create.RAILWAYS.trains.values()) {
            if (train.invalid) {
                continue;
            }

            for (Carriage carriage : train.carriages) {
                present.add(carriage);
                ChunkStabilizerTrainCarriage layout = ChunkStabilizerTrainCarriage.capture(server, train.id, carriage);
                if (layout == null) {
                    suspendCarriage(server, train.id, carriage, data, enabled, retries, presentGates);
                    continue;
                }

                if (layout.loaders().isEmpty()) {
                    ChunkStabilizerTrainCarriage.completeWork(carriage);
                    continue;
                }

                CarriageWork work;
                try {
                    work = prepareTargets(server, train.id, carriage, layout, data, enabled, retries);
                    carriage.forEachPresentEntity(entity -> ChunkStabilizerMovementBehaviour.syncActive(entity, work.active()));
                }
                catch (RuntimeException failure) {
                    ChunkStabilizerTrainCarriage.suspendWork(server, train.id, carriage, "prepare the loader tickets", failure);
                    suspendCarriage(server, train.id, carriage, data, enabled, retries, presentGates);
                    continue;
                }

                for (Entry<ServerLevel, LongSet> entry : work.chunks().entrySet()) {
                    targets.computeIfAbsent(entry.getKey(), ignored -> new HashMap<>()).computeIfAbsent(train.id, ignored -> new LongOpenHashSet()).addAll(entry.getValue());
                }
                states.putAll(work.states());
                GATES.putAll(work.gates());
                presentGates.addAll(work.gates().keySet());
                ChunkStabilizerTrainCarriage.completeWork(carriage);
            }
        }
        runtime.updateTrainStates(states);
        ChunkStabilizerTrainCarriage.retainCarriages(present);
        GATES.keySet().retainAll(presentGates);
        for (Entry<ServerLevel, Map<UUID, LongSet>> dimension : OWNERS.entrySet()) {
            ServerLevel level = dimension.getKey();
            Map<UUID, LongSet> nextOwners = targets.getOrDefault(level, Map.of());
            for (Entry<UUID, LongSet> owner : dimension.getValue().entrySet()) {
                UUID identity = owner.getKey();
                LongSet next = nextOwners.get(identity);
                for (long chunk : owner.getValue()) {
                    if (next != null && next.contains(chunk)) {
                        continue;
                    }

                    CONTROLLER.forceChunk(level, identity, ChunkPos.getX(chunk), ChunkPos.getZ(chunk), false, true);
                }
            }
        }
        for (Entry<ServerLevel, Map<UUID, LongSet>> dimension : targets.entrySet()) {
            ServerLevel level = dimension.getKey();
            Map<UUID, LongSet> previousOwners = OWNERS.getOrDefault(level, Map.of());
            for (Entry<UUID, LongSet> owner : dimension.getValue().entrySet()) {
                UUID identity = owner.getKey();
                LongSet previous = previousOwners.get(identity);
                for (long chunk : owner.getValue()) {
                    if (previous != null && previous.contains(chunk)) {
                        continue;
                    }

                    CONTROLLER.forceChunk(level, identity, ChunkPos.getX(chunk), ChunkPos.getZ(chunk), true, true);
                }
            }
        }
        OWNERS.clear();
        OWNERS.putAll(targets);
    }

    @SubscribeEvent
    private static void onUnload(Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        OWNERS.remove(level);
    }

    @SubscribeEvent
    private static void onStopped(ServerStoppedEvent event) {
        OWNERS.clear();
        ChunkStabilizerTrainCarriage.clear();
        GATES.clear();
    }

    private static CarriageWork prepareTargets(MinecraftServer server, UUID owner, Carriage carriage, ChunkStabilizerTrainCarriage layout, LoadingNetworkSavedData data, boolean enabled, Set<UUID> retries) {
        List<ResourceKey<Level>> dimensions = ChunkStabilizerTrainPositions.dimensions(carriage);
        Map<ServerLevel, LongSet> targets = new HashMap<>();
        Map<UUID, Status> states = new HashMap<>();
        Map<Loader, Gate> gates = new HashMap<>();
        Set<BlockPos> active = new HashSet<>();
        for (BlockPos localPos : layout.loaders()) {
            Loader key = new Loader(owner, carriage.id, localPos);
            UUID identity = layout.identities().get(localPos);
            if (!enabled || !data.allowsTrain(identity)) {
                continue;
            }

            Gate gate = GATES.get(key);
            if (gate != null && gate.status() == Status.TIMEOUT && identity != null && retries.contains(identity)) {
                gate = null;
            }
            Position position = ChunkStabilizerTrainPositions.locate(server, carriage, layout.direction(), localPos, dimensions);
            if (position == null) {
                if (gate != null) {
                    gates.put(key, gate);
                }
                continue;
            }

            ServerLevel level = position.level();
            BlockPos loaderPos = position.loader();
            BlockPos anchorPos = position.anchor();
            CarriageContraptionEntity present = position.dimensional().entity.get();
            boolean loaded = level.isLoaded(loaderPos);
            boolean available = present != null && present.isAlive() && !present.leftTickingChunks && loaded;
            if (!loaded) {
                int tick = server.getTickCount();
                if (gate == null) {
                    gate = new Gate(identity, Status.RECOVERING, tick);
                }
                if (gate.status() == Status.RECOVERING && tick - gate.started() >= RECOVERY_TICKS) {
                    gate = new Gate(identity, Status.TIMEOUT, gate.started());
                }
                gates.put(key, gate);
                if (gate.status() != Status.RECOVERING) {
                    if (identity != null) {
                        states.put(identity, gate.status());
                    }
                    continue;
                }
            }
            LongSet chunks = targets.computeIfAbsent(level, ignored -> new LongOpenHashSet());
            chunks.add(new ChunkPos(loaderPos).toLong());
            chunks.add(new ChunkPos(anchorPos).toLong());
            boolean running = available && level.isPositionEntityTicking(loaderPos) && level.isPositionEntityTicking(anchorPos);
            if (identity != null) {
                states.put(identity, running ? Status.RUNNING : Status.RECOVERING);
            }
            if (!running) {
                continue;
            }

            active.add(localPos);
        }
        return new CarriageWork(targets, states, gates, active);
    }

    private static void suspendCarriage(MinecraftServer server, UUID owner, Carriage carriage, LoadingNetworkSavedData data, boolean enabled, Set<UUID> retries, Set<Loader> presentGates) {
        if (enabled) {
            for (Entry<Loader, Gate> entry : GATES.entrySet()) {
                Loader loader = entry.getKey();
                Gate gate = entry.getValue();
                UUID identity = gate.identity();
                if (!loader.train().equals(owner) || loader.carriage() != carriage.id || gate.status() != Status.TIMEOUT || identity != null && retries.contains(identity) || !data.allowsTrain(identity)) {
                    continue;
                }

                presentGates.add(loader);
            }
        }
        carriage.forEachPresentEntity(entity -> {
            try {
                ChunkStabilizerMovementBehaviour.clearTrainActive(entity);
            }
            catch (RuntimeException failure) {
                ChunkStabilizerTrainCarriage.suspendWork(server, owner, carriage, "clear the loader activity", failure);
            }
        });
    }

    private static void validateTickets(ServerLevel level, TicketHelper helper) {
        for (BlockPos owner : helper.getBlockTickets().keySet()) {
            helper.removeAllTickets(owner);
        }
        for (Entry<UUID, TicketSet> entry : helper.getEntityTickets().entrySet()) {
            UUID owner = entry.getKey();
            if (!CCBConfig.server().opticalPower.chunkStabilizer.enableChunkLoading.get()) {
                helper.removeAllTickets(owner);
                continue;
            }

            TicketSet tickets = entry.getValue();
            for (long chunk : tickets.nonTicking()) {
                helper.removeTicket(owner, chunk, false);
            }
            OWNERS.computeIfAbsent(level, ignored -> new HashMap<>()).put(owner, new LongOpenHashSet(tickets.ticking()));
        }
    }

    private record Loader(UUID train, int carriage, BlockPos pos) {}

    private record Gate(@Nullable UUID identity, Status status, int started) {}

    private record CarriageWork(Map<ServerLevel, LongSet> chunks, Map<UUID, Status> states, Map<Loader, Gate> gates, Set<BlockPos> active) {}
}
