package net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.DirectionalBlock;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.opticalpower.amethystantenna.AmethystAntennaBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.AmethystLoaderBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData.Node;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID)
public final class LoadingNetworkRuntime {
    private static final int RECOVERY_TICKS = 200;
    private static final int AUDIT_INTERVAL = 20;
    private static final int TICKING_TICKET_DISTANCE = 2;
    private static final TicketType<UUID> RECOVERY_TICKET = TicketType.create("ccb_loading_recovery", UUID::compareTo, RECOVERY_TICKS);
    private static Consumer<PhysicalControl> physicalControl = control -> {};
    private final LoadingNetworkSavedData data;
    private final Map<GlobalPos, Recovery> pending = new HashMap<>();
    private final Map<GlobalPos, Status> states = new HashMap<>();
    private final Map<UUID, Status> trainStates = new HashMap<>();
    private final Set<UUID> trainRetries = new HashSet<>();
    private final Map<UUID, Status> physicalStates = new HashMap<>();
    private boolean initialized;
    private boolean configEnabled;

    LoadingNetworkRuntime(LoadingNetworkSavedData data) {
        this.data = data;
    }

    public static void setPhysicalControl(Consumer<PhysicalControl> control) {
        physicalControl = control;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    private static void serverTick(Post event) {
        MinecraftServer server = event.getServer();
        LoadingNetworkSavedData.get(server.overworld()).runtime().tick(server);
    }

    public boolean control(MinecraftServer server, UUID network, UUID player, @Nullable UUID loader, boolean enabled) {
        boolean accepted = loader == null ? data.setNetworkEnabled(network, player, enabled) : data.setNodeEnabled(network, player, loader, enabled);
        if (!accepted) {
            return false;
        }

        applyChanges(server, data.takeChanges());
        LoadingNetworkMoving moving = data.moving();
        if (enabled) {
            moving.retry(network, loader);
            for (Node node : data.trainMembers()) {
                UUID identity = node.loaderId();
                if (!network.equals(node.networkId()) || loader != null && !loader.equals(identity)) {
                    continue;
                }

                trainRetries.add(identity);
            }
        }
        moving.tick(server);
        physicalControl.accept(new PhysicalControl(server, network, loader, enabled));
        return true;
    }

    public Status status(Node node) {
        if (!CCBConfig.server().opticalPower.amethystLoader.enableChunkLoading.get()) {
            return Status.SERVER_DISABLED;
        }

        if (!data.enabled(node)) {
            return Status.PAUSED;
        }

        UUID identity = node.loaderId();
        if (data.trainNode(identity) != null) {
            return trainStates.getOrDefault(identity, Status.UNAVAILABLE);
        }

        LoadingNetworkMoving moving = data.moving();
        if (moving.contains(identity)) {
            return moving.status(identity);
        }

        if (data.physicalNode(identity) != null) {
            return physicalStates.getOrDefault(identity, Status.UNAVAILABLE);
        }

        GlobalPos loader = node.loader();
        if (pending.containsKey(loader)) {
            return Status.RECOVERING;
        }

        return states.getOrDefault(loader, Status.UNAVAILABLE);
    }

    public void updateTrainStates(Map<UUID, Status> states) {
        trainStates.clear();
        trainStates.putAll(states);
    }

    @Unmodifiable
    public Set<UUID> takeTrainRetries() {
        if (trainRetries.isEmpty()) {
            return Set.of();
        }

        Set<UUID> retries = Set.copyOf(trainRetries);
        trainRetries.clear();
        return retries;
    }

    public void updatePhysicalState(UUID identity, @Nullable Status status) {
        if (status == null) {
            physicalStates.remove(identity);
            return;
        }

        physicalStates.put(identity, status);
    }

    public void discard(MinecraftServer server, GlobalPos pos) {
        cancel(server, pos);
        states.remove(pos);
    }

    void tick(MinecraftServer server) {
        data.trains().refresh(server, data);
        data.moving().tick(server);
        boolean enabled = CCBConfig.server().opticalPower.amethystLoader.enableChunkLoading.get();
        int tick = server.getTickCount();
        Set<GlobalPos> changes = new HashSet<>(data.takeChanges());
        if (!initialized) {
            initialized = true;
            for (Node node : data.nodes()) {
                changes.add(node.loader());
            }
            changes.addAll(data.detachedPositions());
        }
        if (configEnabled != enabled) {
            configEnabled = enabled;
            for (Node node : data.nodes()) {
                refreshLoaded(server, node.loader());
            }
            if (enabled) {
                changes.addAll(data.detachedPositions());
            }
        }
        applyChanges(server, changes);
        for (Entry<GlobalPos, Recovery> entry : List.copyOf(pending.entrySet())) {
            GlobalPos pos = entry.getKey();
            Recovery recovery = entry.getValue();
            Node node = data.nodeAt(pos);
            UUID expected = node == null ? data.detachedAt(pos) : node.loaderId();
            if (!enabled || !recovery.identity().equals(expected) || recovery.detached() != (node == null) || node != null && !data.enabled(node)) {
                cancel(server, pos);
                continue;
            }

            ServerLevel level = server.getLevel(pos.dimension());
            if (level == null) {
                cancel(server, pos);
                states.put(pos, Status.UNAVAILABLE);
                continue;
            }

            if (refreshLoaded(server, pos)) {
                cancel(server, pos);
                continue;
            }

            if (tick - recovery.started() < RECOVERY_TICKS) {
                continue;
            }

            cancel(server, pos);
            if (node == null) {
                states.remove(pos);
                continue;
            }

            states.put(pos, Status.TIMEOUT);
        }
        if (tick % AUDIT_INTERVAL != 0) {
            return;
        }

        for (Node node : data.nodes()) {
            refreshLoaded(server, node.loader());
        }
    }

    private void applyChanges(MinecraftServer server, Set<GlobalPos> changes) {
        for (GlobalPos pos : changes) {
            Node node = data.nodeAt(pos);
            UUID identity = node == null ? data.detachedAt(pos) : node.loaderId();
            if (identity == null) {
                cancel(server, pos);
                states.remove(pos);
                continue;
            }

            if (!CCBConfig.server().opticalPower.amethystLoader.enableChunkLoading.get() || node != null && !data.enabled(node)) {
                cancel(server, pos);
                refreshLoaded(server, pos);
                continue;
            }

            if (refreshLoaded(server, pos)) {
                cancel(server, pos);
                continue;
            }

            ServerLevel level = server.getLevel(pos.dimension());
            if (level == null) {
                if (node != null) {
                    states.put(pos, Status.UNAVAILABLE);
                }
                continue;
            }

            Recovery previous = pending.get(pos);
            if (previous != null && previous.identity().equals(identity) && previous.detached() == (node == null)) {
                continue;
            }

            cancel(server, pos);
            pending.put(pos, new Recovery(identity, node == null, server.getTickCount()));
            level.getChunkSource().addRegionTicket(RECOVERY_TICKET, new ChunkPos(pos.pos()), TICKING_TICKET_DISTANCE, identity);
            if (node == null) {
                continue;
            }

            states.put(pos, Status.RECOVERING);
        }
    }

    private boolean refreshLoaded(MinecraftServer server, GlobalPos pos) {
        BlockPos loaderPos = pos.pos();
        ServerLevel level = server.getLevel(pos.dimension());
        if (level == null || !level.isLoaded(loaderPos)) {
            return false;
        }

        Node node = data.nodeAt(pos);
        UUID identity = node == null ? data.detachedAt(pos) : node.loaderId();
        if (identity == null) {
            return true;
        }

        if (!(level.getBlockEntity(loaderPos) instanceof AmethystLoaderBlockEntity loader) || !identity.equals(loader.networkNodeId()) || SubLevelBridge.resolve(level, loaderPos).inSubLevel()) {
            data.removeLoader(pos);
            states.remove(pos);
            return true;
        }

        GlobalPos antennaPos = node == null ? null : node.antenna();
        BlockPos antennaBlockPos = antennaPos == null ? null : antennaPos.pos();
        if (antennaBlockPos != null && level.isLoaded(antennaBlockPos)) {
            UUID antennaId = node.antennaId();
            boolean valid = false;
            if (level.getBlockEntity(antennaBlockPos) instanceof AmethystAntennaBlockEntity antenna) {
                LoadingNetworkBinding binding = antenna.binding();
                UUID network = binding == null ? null : binding.id();
                valid = antennaId.equals(antenna.identity()) && node.loader().pos().equals(antennaBlockPos.relative(antenna.getBlockState().getValue(DirectionalBlock.FACING).getOpposite())) && Objects.equals(node.networkId(), network);
            }
            if (!valid) {
                data.removeAntenna(antennaPos, antennaId);
            }
        }
        loader.refreshLoading();
        if (data.nodeAt(pos) == null) {
            states.remove(pos);
            if (loader.isLoadingActive()) {
                data.finishDetached(pos);
            }
            return true;
        }

        if (!loader.isLoadingActive()) {
            states.put(pos, Status.PAUSED);
            return true;
        }

        states.put(pos, Status.RUNNING);
        return true;
    }

    private void cancel(MinecraftServer server, GlobalPos pos) {
        Recovery recovery = pending.remove(pos);
        if (recovery == null) {
            return;
        }

        if (states.get(pos) == Status.RECOVERING) {
            states.put(pos, Status.UNAVAILABLE);
        }
        ServerLevel level = server.getLevel(pos.dimension());
        if (level == null) {
            return;
        }

        level.getChunkSource().removeRegionTicket(RECOVERY_TICKET, new ChunkPos(pos.pos()), TICKING_TICKET_DISTANCE, recovery.identity());
    }

    public enum Status {
        RUNNING,
        PAUSED,
        RECOVERING,
        TIMEOUT,
        UNAVAILABLE,
        SERVER_DISABLED,
        ASSEMBLED
    }

    public record PhysicalControl(MinecraftServer server, UUID network, @Nullable UUID loader, boolean enabled) {}

    private record Recovery(UUID identity, boolean detached, int started) {}
}
