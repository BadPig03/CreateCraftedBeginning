package net.ty.createcraftedbeginning.compat.sable;

import dev.ryanhcode.sable.companion.SubLevelAccess;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;
import net.neoforged.neoforge.event.tick.LevelTickEvent.Post;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.AmethystLoaderBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkRuntime.PhysicalControl;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkRuntime.Status;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkRuntime;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData.Node;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData.PhysicalNode;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class SableLoadingNetworkControl {
    private static final int RECOVERY_TICKS = 200;
    private static final Map<ServerLevel, SableLoadingNetworkControl> LEVELS = new HashMap<>();
    private final Map<UUID, Status> states = new HashMap<>();
    private final Map<UUID, Recovery> pending = new HashMap<>();

    private SableLoadingNetworkControl() {
    }

    public static void rememberHolding(ServerLevel level, Object chunk, Object holding) {
        remember(level, SableAmethystLoaderAccess.holdingId(holding), SableAmethystLoaderAccess.holdingPos(chunk));
    }

    public static void rememberSaved(ServerLevel level, Object data, Object pointer) {
        remember(level, SableAmethystLoaderAccess.savedId(data), SableAmethystLoaderAccess.pointerPos(pointer));
    }

    static void register() {
        LoadingNetworkRuntime.setPhysicalControl(SableLoadingNetworkControl::control);
        NeoForge.EVENT_BUS.addListener(SableLoadingNetworkControl::onTick);
        NeoForge.EVENT_BUS.addListener(SableLoadingNetworkControl::onUnload);
    }

    static boolean allowsClaim(ServerLevel level, UUID identity) {
        SableLoadingNetworkControl control = LEVELS.get(level);
        if (control == null) {
            return true;
        }

        Status status = control.states.get(identity);
        return status != Status.TIMEOUT && status != Status.UNAVAILABLE;
    }

    private static void remember(ServerLevel level, UUID host, ChunkPos chunk) {
        LoadingNetworkSavedData data = LoadingNetworkSavedData.get(level);
        for (PhysicalNode member : data.physicalNodes()) {
            if (!member.host().equals(host) || member.node().loader().dimension() != level.dimension()) {
                continue;
            }

            data.rememberPhysical(new PhysicalNode(host, member.node(), chunk));
        }
    }

    private static void control(PhysicalControl request) {
        LoadingNetworkSavedData data = LoadingNetworkSavedData.get(request.server().overworld());
        for (ServerLevel level : request.server().getAllLevels()) {
            SableLoadingNetworkControl control = LEVELS.computeIfAbsent(level, ignored -> new SableLoadingNetworkControl());
            if (request.enabled()) {
                for (PhysicalNode member : data.physicalNodes()) {
                    Node node = member.node();
                    if (node.loader().dimension() != level.dimension() || !Objects.equals(node.networkId(), request.network()) || request.loader() != null && !request.loader().equals(node.loaderId()) || control.pending.containsKey(node.loaderId())) {
                        continue;
                    }

                    control.states.remove(node.loaderId());
                }
            }
            control.tick(level);
        }
    }

    private static void onTick(Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        LEVELS.computeIfAbsent(level, ignored -> new SableLoadingNetworkControl()).tick(level);
    }

    private static void onUnload(Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        SableLoadingNetworkControl control = LEVELS.remove(level);
        Object container = SableAmethystLoaderAccess.container(level);
        if (control == null || container == null) {
            return;
        }

        for (UUID identity : List.copyOf(control.pending.keySet())) {
            control.cancel(container, identity);
        }
    }

    private void tick(ServerLevel level) {
        Object container = SableAmethystLoaderAccess.container(level);
        if (container == null) {
            return;
        }

        LoadingNetworkSavedData data = LoadingNetworkSavedData.get(level);
        Set<UUID> present = new HashSet<>();
        for (PhysicalNode member : data.physicalNodes()) {
            Node node = member.node();
            if (node.loader().dimension() != level.dimension()) {
                continue;
            }

            UUID identity = node.loaderId();
            present.add(identity);
            SubLevelAccess host = SableAmethystLoaderAccess.subLevel(container, member.host());
            boolean enabled = CCBConfig.server().opticalPower.amethystLoader.enableChunkLoading.get() && data.allowsPhysical(identity);
            if (!enabled) {
                cancel(container, identity);
                states.put(identity, Status.PAUSED);
                if (host != null) {
                    SableAmethystLoaderAccess.setForced(container, host, node.loader().pos(), false);
                    refresh(level, node);
                }
                continue;
            }

            Status state = states.get(identity);
            if (state == Status.TIMEOUT || state == Status.UNAVAILABLE) {
                continue;
            }

            Recovery recovery = pending.get(identity);
            if (recovery != null && !recovery.host().equals(member.host())) {
                cancel(container, identity);
                recovery = null;
            }
            if (host == null && recovery == null) {
                if (member.holdingChunk() == null) {
                    states.put(identity, Status.UNAVAILABLE);
                    continue;
                }

                SableAmethystLoaderAccess.recover(container, member.host(), member.holdingChunk());
                host = SableAmethystLoaderAccess.subLevel(container, member.host());
            }
            if (host != null && level.isLoaded(node.loader().pos()) && level.isLoaded(node.antenna().pos())) {
                if (!member.host().equals(SubLevelBridge.subLevelId(level, node.loader().pos())) || !SableLoadingNetwork.matches(level, node, false)) {
                    data.forgetPhysical(identity);
                    cancel(container, identity);
                    refresh(level, node);
                    continue;
                }

                refresh(level, node);
                if (level.isPositionEntityTicking(SubLevelBridge.resolve(level, node.loader().pos()).blockPos())) {
                    cancel(container, identity);
                    states.put(identity, Status.RUNNING);
                    continue;
                }
            }
            if (recovery == null) {
                recovery = new Recovery(member.host(), node.loader().pos(), level.getServer().getTickCount());
                pending.put(identity, recovery);
            }
            states.put(identity, Status.RECOVERING);
            if (host != null) {
                SableAmethystLoaderAccess.setRecovery(container, host, recovery.pos(), true);
            }
            if (level.getServer().getTickCount() - recovery.started() < RECOVERY_TICKS) {
                continue;
            }

            cancel(container, identity);
            states.put(identity, Status.TIMEOUT);
            if (host == null) {
                continue;
            }

            SableAmethystLoaderAccess.setForced(container, host, node.loader().pos(), false);
            refresh(level, node);
        }
        for (UUID identity : List.copyOf(states.keySet())) {
            if (present.contains(identity)) {
                data.runtime().updatePhysicalState(identity, states.get(identity));
                continue;
            }

            cancel(container, identity);
            states.remove(identity);
            data.runtime().updatePhysicalState(identity, null);
        }
        for (Entry<UUID, Object> entry : List.copyOf(SableAmethystLoaderAccess.tickets(container).entrySet())) {
            SubLevelAccess host = SableAmethystLoaderAccess.subLevel(container, entry.getKey());
            if (host == null) {
                continue;
            }

            for (BlockPos pos : SableAmethystLoaderAccess.recoveries(entry.getValue())) {
                if (pending.values().stream().anyMatch(recovery -> recovery.host().equals(entry.getKey()) && recovery.pos().equals(pos))) {
                    continue;
                }

                SableAmethystLoaderAccess.setRecovery(container, host, pos, false);
            }
        }
        SableAmethystLoaderTickets.refresh(level);
    }

    private static void refresh(ServerLevel level, Node node) {
        if (!level.isLoaded(node.loader().pos()) || !(level.getBlockEntity(node.loader().pos()) instanceof AmethystLoaderBlockEntity loader) || !node.loaderId().equals(loader.networkNodeId())) {
            return;
        }

        loader.refreshLoading();
    }

    private void cancel(Object container, UUID identity) {
        Recovery recovery = pending.remove(identity);
        if (recovery == null) {
            return;
        }

        SubLevelAccess host = SableAmethystLoaderAccess.subLevel(container, recovery.host());
        if (host == null) {
            return;
        }

        SableAmethystLoaderAccess.setRecovery(container, host, recovery.pos(), false);
    }

    private record Recovery(UUID host, BlockPos pos, int started) {}
}
