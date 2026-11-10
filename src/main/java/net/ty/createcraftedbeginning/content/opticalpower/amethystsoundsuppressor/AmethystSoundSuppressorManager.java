package net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystSoundSuppressorManager {
    private static final int STATIC_PRUNE_INTERVAL_TICKS = 100;
    private static final int MOVING_PRUNE_INTERVAL_TICKS = 20;
    private static final int MOVING_REGISTRATION_TIMEOUT_TICKS = 50;
    private static final AmethystSoundSuppressorIndex INDEX = new AmethystSoundSuppressorIndex();
    private static final Map<ResourceLocation, Map<AmethystSoundSuppressorIdentity, MovingRegistration>> MOVING_LAST_SEEN_BY_DIMENSION = new HashMap<>();

    private AmethystSoundSuppressorManager() {
    }

    static void tick(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        long gameTime = level.getGameTime();
        if (gameTime % MOVING_PRUNE_INTERVAL_TICKS == 0) {
            pruneMovingRegistrations(serverLevel, gameTime);
        }

        if (gameTime % STATIC_PRUNE_INTERVAL_TICKS != 0) {
            return;
        }

        ResourceLocation dimension = dimension(level);
        for (AmethystSoundSuppressorInstance suppressor : INDEX.getInstances(dimension)) {
            AmethystSoundSuppressorIdentity identity = suppressor.identity();
            if (identity.contraptionId() != null) {
                continue;
            }

            BlockPos registrationPos = identity.blockPos();
            if (level.isLoaded(registrationPos) && level.getBlockEntity(registrationPos) instanceof AmethystSoundSuppressorBlockEntity) {
                continue;
            }

            remove(serverLevel, identity, suppressor.nativeOwner());
        }
    }

    static boolean hasCoverage(ResourceLocation dimension) {
        return INDEX.hasCoverage(dimension);
    }

    static @Nullable AmethystSoundSuppressorInstance findCoveringSuppressor(BlockPos blockPos, ResourceLocation dimension) {
        return INDEX.findCoveringSuppressor(blockPos, dimension, identity -> true);
    }

    static void update(ServerLevel level, AmethystSoundSuppressorIdentity identity, BlockPos effectCenter, @Nullable AmethystSoundSuppressorBlockEntity nativeOwner) {
        ResourceLocation dimension = dimension(level);
        if (!INDEX.update(identity, effectCenter, dimension, AmethystSoundSuppressorBlockEntity.BASE_WORKING_RANGE, nativeOwner)) {
            return;
        }

        sendToDimension(level, new AmethystSoundSuppressorUpdatePacket(identity, effectCenter, dimension, AmethystSoundSuppressorBlockEntity.BASE_WORKING_RANGE, true));
    }

    static void updateMoving(ServerLevel level, AmethystSoundSuppressorIdentity identity, BlockPos effectCenter) {
        ResourceLocation dimension = dimension(level);
        Map<AmethystSoundSuppressorIdentity, MovingRegistration> movingRegistrations = MOVING_LAST_SEEN_BY_DIMENSION.computeIfAbsent(dimension, ignored -> new HashMap<>());
        MovingRegistration registration = movingRegistrations.computeIfAbsent(identity, ignored -> new MovingRegistration());
        registration.lastSeen = level.getGameTime();
        update(level, identity, effectCenter, null);
    }

    static void remove(ServerLevel level, AmethystSoundSuppressorIdentity identity, @Nullable AmethystSoundSuppressorBlockEntity nativeOwner) {
        ResourceLocation dimension = dimension(level);
        if (!INDEX.remove(identity, dimension, nativeOwner)) {
            return;
        }

        sendToDimension(level, new AmethystSoundSuppressorUpdatePacket(identity, identity.blockPos(), dimension, (short) 0, false));
    }

    static void removeMoving(ServerLevel level, AmethystSoundSuppressorIdentity identity) {
        ResourceLocation dimension = dimension(level);
        Map<AmethystSoundSuppressorIdentity, MovingRegistration> movingRegistrations = MOVING_LAST_SEEN_BY_DIMENSION.get(dimension);
        if (movingRegistrations != null) {
            movingRegistrations.remove(identity);
            if (movingRegistrations.isEmpty()) {
                MOVING_LAST_SEEN_BY_DIMENSION.remove(dimension);
            }
        }

        remove(level, identity, null);
    }

    static void removeContraption(ServerLevel level, UUID contraptionId) {
        Map<AmethystSoundSuppressorIdentity, MovingRegistration> movingRegistrations = MOVING_LAST_SEEN_BY_DIMENSION.get(dimension(level));
        if (movingRegistrations == null) {
            return;
        }

        List<AmethystSoundSuppressorIdentity> identities = movingRegistrations.keySet().stream().filter(identity -> contraptionId.equals(identity.contraptionId())).toList();
        for (AmethystSoundSuppressorIdentity identity : identities) {
            removeMoving(level, identity);
        }
    }

    static void removeDimension(ServerLevel level) {
        ResourceLocation dimension = dimension(level);
        MOVING_LAST_SEEN_BY_DIMENSION.remove(dimension);
        for (AmethystSoundSuppressorInstance suppressor : INDEX.removeDimension(dimension)) {
            sendToDimension(level, new AmethystSoundSuppressorUpdatePacket(suppressor.identity(), suppressor.effectCenter(), dimension, (short) 0, false));
        }
    }

    static void clear() {
        MOVING_LAST_SEEN_BY_DIMENSION.clear();
        INDEX.clear();
    }

    static void sendToClient(ServerPlayer serverPlayer) {
        CatnipServices.NETWORK.sendToClient(serverPlayer, AmethystSoundSuppressorResetPacket.INSTANCE);
        ResourceLocation dimension = dimension(serverPlayer.level());
        for (AmethystSoundSuppressorInstance suppressor : INDEX.getInstances(dimension)) {
            CatnipServices.NETWORK.sendToClient(serverPlayer, new AmethystSoundSuppressorUpdatePacket(suppressor.identity(), suppressor.effectCenter(), dimension, suppressor.range(), true));
        }
    }

    private static void pruneMovingRegistrations(ServerLevel level, long gameTime) {
        Map<AmethystSoundSuppressorIdentity, MovingRegistration> movingRegistrations = MOVING_LAST_SEEN_BY_DIMENSION.get(dimension(level));
        if (movingRegistrations == null) {
            return;
        }

        List<AmethystSoundSuppressorIdentity> staleRegistrations = movingRegistrations.entrySet().stream().filter(registrationEntry -> gameTime - registrationEntry.getValue().lastSeen > MOVING_REGISTRATION_TIMEOUT_TICKS).map(Entry::getKey).toList();
        for (AmethystSoundSuppressorIdentity identity : staleRegistrations) {
            removeMoving(level, identity);
        }
    }

    private static ResourceLocation dimension(Level level) {
        return level.dimension().location();
    }

    private static void sendToDimension(ServerLevel level, AmethystSoundSuppressorUpdatePacket packet) {
        for (ServerPlayer player : level.players()) {
            CatnipServices.NETWORK.sendToClient(player, packet);
        }
    }

    private static final class MovingRegistration {
        private long lastSeen;
    }
}
