package net.ty.createcraftedbeginning.content.opticalpower.amethystloader;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent.Post;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkMoving;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkRuntime.Status;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.apache.commons.lang3.tuple.MutablePair;
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
final class AmethystLoaderMovingTickets {
    private static final int RECOVERY_TICKS = 200;
    private static final Map<ServerLevel, Map<UUID, MovingOwner>> OWNERS = new HashMap<>();

    private AmethystLoaderMovingTickets() {
    }

    static void restore(ServerLevel level, UUID owner, LongSet chunks) {
        MovingOwner state = OWNERS.computeIfAbsent(level, ignored -> new HashMap<>()).computeIfAbsent(owner, ignored -> new MovingOwner());
        state.chunks.addAll(chunks);
        state.recoveryDeadline = level.getGameTime() + RECOVERY_TICKS;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    private static void onJoin(EntityJoinLevelEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof AbstractContraptionEntity entity)) {
            return;
        }

        Contraption contraption = entity.getContraption();
        if (contraption instanceof CarriageContraption || !AmethystLoaderMovementBehaviour.supports(entity)) {
            return;
        }

        for (MutablePair<StructureBlockInfo, MovementContext> actor : contraption.getActors()) {
            if (!actor.getLeft().state().is(CCBBlocks.AMETHYST_LOADER_BLOCK.get())) {
                continue;
            }

            MovingOwner state = OWNERS.computeIfAbsent(level, ignored -> new HashMap<>()).computeIfAbsent(entity.getUUID(), ignored -> new MovingOwner());
            state.entity = entity;
            return;
        }
    }

    @SubscribeEvent
    private static void onTick(Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Map<UUID, MovingOwner> owners = OWNERS.get(level);
        if (owners == null) {
            return;
        }

        LoadingNetworkMoving moving = LoadingNetworkSavedData.get(level).moving();
        boolean enabled = CCBConfig.server().opticalPower.amethystLoader.enableChunkLoading.get();
        long gameTime = level.getGameTime();
        for (Entry<UUID, MovingOwner> entry : List.copyOf(owners.entrySet())) {
            UUID owner = entry.getKey();
            MovingOwner state = entry.getValue();
            AbstractContraptionEntity entity = state.entity;
            if (entity != null && entity.isRemoved()) {
                RemovalReason reason = entity.getRemovalReason();
                state.entity = null;
                state.recoveryDeadline = gameTime + RECOVERY_TICKS;
                if (reason == null || reason.shouldDestroy() || reason == RemovalReason.CHANGED_DIMENSION) {
                    state.recoveryDeadline = gameTime;
                }
                entity = null;
            }
            if (entity == null && level.getEntity(owner) instanceof AbstractContraptionEntity restored && !restored.isRemoved()) {
                entity = restored;
                state.entity = restored;
            }
            if (entity == null && enabled && (!moving.hasHost(owner) || moving.demanded(owner)) && gameTime < state.recoveryDeadline) {
                continue;
            }

            LongSet targets = new LongOpenHashSet();
            Contraption contraption = entity == null ? null : entity.getContraption();
            if (entity != null && !(contraption instanceof CarriageContraption) && AmethystLoaderMovementBehaviour.supports(entity)) {
                List<BlockPos> support = LoadingNetworkMoving.support(entity);
                moving.observe(entity, support);
                Set<BlockPos> active = new HashSet<>();
                boolean ready = support.stream().allMatch(level::isPositionEntityTicking);
                boolean suspended = false;
                for (MutablePair<StructureBlockInfo, MovementContext> actor : contraption.getActors()) {
                    StructureBlockInfo info = actor.getLeft();
                    if (!info.state().is(CCBBlocks.AMETHYST_LOADER_BLOCK.get()) || contraption.disassembled) {
                        continue;
                    }

                    UUID identity = AmethystLoaderSerialization.readNetworkNodeId(info.nbt());
                    BlockPos localPos = info.pos();
                    if (!enabled || !moving.allows(identity)) {
                        state.workChecks.remove(localPos);
                        continue;
                    }

                    BlockPos position = BlockPos.containing(entity.toGlobalVector(localPos.getCenter(), 1));
                    boolean loaded = level.isLoaded(position);
                    if (loaded) {
                        state.workChecks.remove(localPos);
                    }
                    else {
                        WorkCheck check = state.workChecks.get(localPos);
                        if (check != null && check.status() != Status.RECOVERING && identity != null && moving.contains(identity) && !moving.awaitingRetry(identity)) {
                            check = null;
                        }
                        if (check == null) {
                            check = new WorkCheck(Status.RECOVERING, gameTime);
                            state.workChecks.put(localPos, check);
                        }
                        if (check.status() == Status.RECOVERING && gameTime - check.started() >= RECOVERY_TICKS) {
                            check = new WorkCheck(Status.TIMEOUT, check.started());
                            state.workChecks.put(localPos, check);
                        }
                        if (check.status() != Status.RECOVERING) {
                            suspended = true;
                            if (identity != null && moving.contains(identity)) {
                                moving.state(identity, check.status());
                            }
                            continue;
                        }
                    }
                    targets.add(new ChunkPos(position).toLong());
                    boolean running = loaded && ready && level.isPositionEntityTicking(position);
                    if (identity != null && moving.contains(identity)) {
                        moving.state(identity, running ? Status.RUNNING : Status.RECOVERING);
                    }
                    if (!running) {
                        continue;
                    }

                    active.add(localPos);
                }
                moving.requireRetry(level.getServer(), owner, suspended && targets.isEmpty());
                if (!targets.isEmpty()) {
                    for (BlockPos pos : support) {
                        targets.add(new ChunkPos(pos).toLong());
                    }
                }
                AmethystLoaderMovementBehaviour.syncActive(entity, active);
            }
            for (long chunk : state.chunks) {
                if (targets.contains(chunk)) {
                    continue;
                }

                AmethystLoaderTickets.setForced(level, owner, chunk, false);
            }
            for (long chunk : targets) {
                if (state.chunks.contains(chunk)) {
                    continue;
                }

                AmethystLoaderTickets.setForced(level, owner, chunk, true);
            }
            state.chunks.clear();
            state.chunks.addAll(targets);
            if (entity != null && !targets.isEmpty()) {
                moving.claimed(level.getServer(), owner);
            }
            if (entity != null) {
                continue;
            }

            owners.remove(owner);
        }
    }

    @SubscribeEvent
    private static void onLeave(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof AbstractContraptionEntity entity) || entity.getContraption() instanceof CarriageContraption || !AmethystLoaderMovementBehaviour.supports(entity)) {
            return;
        }

        LoadingNetworkSavedData.get(level).moving().removed(entity);
    }

    @SubscribeEvent
    private static void onStopped(ServerStoppedEvent event) {
        OWNERS.clear();
    }

    @SubscribeEvent
    private static void onUnload(Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        OWNERS.remove(level);
    }

    private static final class MovingOwner {
        private final LongSet chunks = new LongOpenHashSet();
        private final Map<BlockPos, WorkCheck> workChecks = new HashMap<>();
        private @Nullable AbstractContraptionEntity entity;
        private long recoveryDeadline;
    }

    private record WorkCheck(Status status, long started) {}
}
