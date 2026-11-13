package net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork;

import com.simibubi.create.AllAttachmentTypes;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import com.simibubi.create.content.contraptions.bearing.BearingContraption;
import com.simibubi.create.content.contraptions.bearing.ClockworkContraption;
import com.simibubi.create.content.contraptions.minecart.capability.MinecartController;
import com.simibubi.create.content.contraptions.mounted.MountedContraption;
import com.simibubi.create.content.contraptions.piston.PistonContraption;
import com.simibubi.create.content.contraptions.pulley.PulleyContraption;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.ChunkStabilizerLocation;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkMovingSerialization.StoredHost;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkRuntime.Status;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData.Node;
import net.ty.createcraftedbeginning.mixin.common.accessor.ControlledContraptionEntityAccessor;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
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
public final class LoadingNetworkMoving {
    private static final int RECOVERY_TICKS = 200;
    private static final int TICKING_TICKET_DISTANCE = 2;
    private static final TicketType<UUID> RECOVERY_TICKET = TicketType.create("ccb_moving_loading_recovery", UUID::compareTo, RECOVERY_TICKS);
    private final LoadingNetworkSavedData data;
    private final Map<UUID, Host> hosts = new HashMap<>();
    private final Map<UUID, UUID> memberHosts = new HashMap<>();
    private final Map<UUID, Layout> layouts = new HashMap<>();
    private final Map<UUID, Recovery> pending = new HashMap<>();
    private final Set<UUID> attempted = new HashSet<>();
    private final Set<UUID> retryRequired = new HashSet<>();
    private final Map<UUID, Status> states = new HashMap<>();

    LoadingNetworkMoving(LoadingNetworkSavedData data) {
        this.data = data;
    }

    public static List<BlockPos> support(AbstractContraptionEntity entity) {
        Contraption contraption = entity.getContraption();
        if (contraption == null || contraption.disassembled || entity.getPassengers().stream().anyMatch(AbstractContraptionEntity.class::isInstance)) {
            return List.of();
        }

        Class<? extends Contraption> type = contraption.getClass();
        if (entity instanceof ControlledContraptionEntity && !entity.isPassenger() && (type == BearingContraption.class || type == ClockworkContraption.class || type == PistonContraption.class || type == PulleyContraption.class)) {
            BlockPos controller = ((ControlledContraptionEntityAccessor) entity).ccb$getControllerPos();
            if (controller != null) {
                return List.of(entity.blockPosition(), controller);
            }
        }
        if (!(entity instanceof OrientedContraptionEntity oriented) || !(contraption instanceof MountedContraption) || oriented.getCouplingId() != null || !(entity.getVehicle() instanceof AbstractMinecart cart) || cart.isPassenger()) {
            return List.of();
        }

        MinecartController controller = cart.getData(AllAttachmentTypes.MINECART_CONTROLLER);
        if (controller.isLeadingCoupling() || controller.isConnectedToCoupling()) {
            return List.of();
        }

        return List.of(entity.blockPosition(), cart.blockPosition());
    }

    public boolean contains(UUID identity) {
        return memberHosts.containsKey(identity);
    }

    public boolean hasHost(UUID host) {
        return hosts.containsKey(host);
    }

    public @Nullable ChunkStabilizerLocation position(MinecraftServer server, UUID identity) {
        UUID hostId = memberHosts.get(identity);
        Host host = hosts.get(hostId);
        if (host == null) {
            return null;
        }

        ServerLevel level = server.getLevel(host.position().dimension());
        if (level == null || !(level.getEntity(hostId) instanceof AbstractContraptionEntity entity) || !entity.isAlive() || support(entity).isEmpty() || SubLevelBridge.resolve(level, entity.position()).inSubLevel()) {
            return null;
        }

        for (Entry<BlockPos, Node> entry : LoadingNetworkContraptions.members(entity.getContraption()).entrySet()) {
            if (!entry.getValue().loaderId().equals(identity)) {
                continue;
            }

            Vec3 position = entity.toGlobalVector(entry.getKey().getCenter(), 1);
            if (!Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)) {
                return null;
            }

            return new ChunkStabilizerLocation(level.dimension(), position, hostId, 0);
        }
        return null;
    }

    public boolean allows(@Nullable UUID identity) {
        Node node = identity == null ? null : data.assemblyNode(identity);
        return node == null || !contains(identity) || data.enabled(node);
    }

    public boolean demanded(UUID identity) {
        Host host = hosts.get(identity);
        if (host == null) {
            return false;
        }

        if (host.independent()) {
            return true;
        }

        for (UUID member : host.members()) {
            Node node = data.assemblyNode(member);
            if (node == null || !data.enabled(node)) {
                continue;
            }

            return true;
        }
        return false;
    }

    public void observe(AbstractContraptionEntity entity, List<BlockPos> support) {
        ServerLevel level = (ServerLevel) entity.level();
        UUID identity = entity.getUUID();
        if (support.isEmpty()) {
            forget(level.getServer(), identity);
            return;
        }

        Contraption contraption = entity.getContraption();
        Layout layout = layouts.get(identity);
        if (layout == null || layout.contraption() != contraption) {
            Map<BlockPos, StructureBlockInfo> blocks = contraption.getBlocks();
            Map<BlockPos, Node> members = LoadingNetworkContraptions.members(blocks);
            boolean independent = blocks.values().stream().anyMatch(info -> info.state().is(CCBBlocks.CHUNK_STABILIZER_BLOCK.get()) && !members.containsKey(info.pos()));
            layout = new Layout(contraption, List.copyOf(members.values()), independent);
            layouts.put(identity, layout);
        }
        Set<UUID> members = new HashSet<>();
        for (Node node : layout.members()) {
            data.rememberAssembly(node);
            UUID member = node.loaderId();
            members.add(member);
            memberHosts.put(member, identity);
        }
        Host next = new Host(GlobalPos.of(level.dimension(), entity.blockPosition()), support.stream().distinct().toList(), members, layout.independent());
        Host previous = hosts.put(identity, next);
        if (previous != null) {
            for (UUID member : previous.members()) {
                if (members.contains(member)) {
                    continue;
                }

                memberHosts.remove(member, identity);
                states.remove(member);
                data.forgetAssembly(member);
            }
        }
        if (next.equals(previous)) {
            return;
        }

        data.setDirty();
    }

    public void removed(AbstractContraptionEntity entity) {
        ServerLevel level = (ServerLevel) entity.level();
        UUID identity = entity.getUUID();
        RemovalReason reason = entity.getRemovalReason();
        if (entity.getContraption().disassembled || reason != null && (reason.shouldDestroy() || reason == RemovalReason.CHANGED_DIMENSION)) {
            forget(level.getServer(), identity);
            return;
        }

        if (hosts.containsKey(identity)) {
            observe(entity, support(entity));
        }
        layouts.remove(identity);
    }

    public void claimed(MinecraftServer server, UUID host) {
        cancel(server, host);
        attempted.remove(host);
    }

    public void requireRetry(MinecraftServer server, UUID host, boolean required) {
        if (!required) {
            retryRequired.remove(host);
            return;
        }

        retryRequired.add(host);
        cancel(server, host);
    }

    public void state(UUID identity, Status status) {
        states.put(identity, status);
    }

    public boolean awaitingRetry(UUID identity) {
        Status status = states.get(identity);
        return status == Status.TIMEOUT;
    }

    Status status(UUID identity) {
        return states.getOrDefault(identity, Status.UNAVAILABLE);
    }

    void retry(UUID network, UUID member) {
        for (Node node : data.assembledMembers(network)) {
            UUID identity = node.loaderId();
            UUID host = memberHosts.get(identity);
            if (host == null || !member.equals(identity)) {
                continue;
            }

            if (pending.containsKey(host)) {
                continue;
            }

            attempted.remove(host);
            retryRequired.remove(host);
            if (!awaitingRetry(identity)) {
                continue;
            }

            states.remove(identity);
        }
    }

    void tick(MinecraftServer server) {
        boolean enabled = CCBConfig.server().opticalPower.chunkStabilizer.enableChunkLoading.get();
        int tick = server.getTickCount();
        for (Entry<UUID, Host> entry : List.copyOf(hosts.entrySet())) {
            UUID identity = entry.getKey();
            Host host = entry.getValue();
            if (!enabled || !demanded(identity)) {
                cancel(server, identity);
                attempted.remove(identity);
                continue;
            }

            ServerLevel level = server.getLevel(host.position().dimension());
            if (level == null) {
                host.members().forEach(member -> states.put(member, Status.UNAVAILABLE));
                continue;
            }

            if (level.getEntity(identity) instanceof AbstractContraptionEntity entity && entity.isAlive() && !pending.containsKey(identity)) {
                attempted.remove(identity);
                continue;
            }

            if (retryRequired.contains(identity)) {
                cancel(server, identity);
                continue;
            }

            if (attempted.add(identity)) {
                pending.put(identity, new Recovery(host, tick));
                for (BlockPos pos : host.support()) {
                    level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
                    level.getChunkSource().addRegionTicket(RECOVERY_TICKET, new ChunkPos(pos), TICKING_TICKET_DISTANCE, identity, true);
                }
            }
            Recovery recovery = pending.get(identity);
            if (recovery != null && tick - recovery.started() >= RECOVERY_TICKS) {
                cancel(server, identity);
            }
            Status status = pending.containsKey(identity) ? Status.RECOVERING : Status.TIMEOUT;
            host.members().forEach(member -> states.put(member, status));
        }
    }

    void load(ListTag saved) {
        for (StoredHost stored : LoadingNetworkMovingSerialization.read(saved)) {
            UUID identity = stored.identity();
            Host host = stored.host();
            for (UUID member : host.members()) {
                memberHosts.put(member, identity);
            }
            hosts.put(identity, host);
        }
    }

    ListTag save() {
        return LoadingNetworkMovingSerialization.write(hosts);
    }

    private void forget(MinecraftServer server, UUID identity) {
        cancel(server, identity);
        Host removed = hosts.remove(identity);
        layouts.remove(identity);
        attempted.remove(identity);
        retryRequired.remove(identity);
        if (removed == null) {
            return;
        }

        for (UUID member : removed.members()) {
            memberHosts.remove(member, identity);
            states.remove(member);
        }
        data.setDirty();
    }

    private void cancel(MinecraftServer server, UUID identity) {
        Recovery recovery = pending.remove(identity);
        if (recovery == null) {
            return;
        }

        Host host = recovery.host();
        ServerLevel level = server.getLevel(host.position().dimension());
        if (level == null) {
            return;
        }

        for (BlockPos pos : host.support()) {
            level.getChunkSource().removeRegionTicket(RECOVERY_TICKET, new ChunkPos(pos), TICKING_TICKET_DISTANCE, identity, true);
        }
    }

    record Host(GlobalPos position, List<BlockPos> support, Set<UUID> members, boolean independent) {
        Host {
            position = GlobalPos.of(position.dimension(), position.pos().immutable());
            support = support.stream().map(BlockPos::immutable).toList();
            members = Set.copyOf(members);
        }
    }

    private record Recovery(Host host, int started) {}

    private record Layout(Contraption contraption, List<Node> members, boolean independent) {}
}
