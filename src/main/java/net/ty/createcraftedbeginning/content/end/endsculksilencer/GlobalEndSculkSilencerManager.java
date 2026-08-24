package net.ty.createcraftedbeginning.content.end.endsculksilencer;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GlobalEndSculkSilencerManager {
    private static final int STATIC_PRUNE_INTERVAL_TICKS = 100;
    private static final int MOVING_PRUNE_INTERVAL_TICKS = 20;
    private static final int MOVING_REGISTRATION_TIMEOUT_TICKS = 50;
    private static final EndSculkSilencerIndex INDEX = new EndSculkSilencerIndex();
    private static final Map<ResourceLocation, Map<BlockPos, Long>> MOVING_LAST_SEEN_BY_DIMENSION = new HashMap<>();

    private GlobalEndSculkSilencerManager() {
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
        Map<BlockPos, Long> movingRegistrations = MOVING_LAST_SEEN_BY_DIMENSION.get(dimension);
        for (EndSculkSilencerInstance silencer : INDEX.getInstances(dimension)) {
            BlockPos registrationPos = silencer.registrationPos();
            if (movingRegistrations != null && movingRegistrations.containsKey(registrationPos)) {
                continue;
            }

            if (level.isLoaded(registrationPos) && level.getBlockEntity(registrationPos) instanceof EndSculkSilencerBlockEntity) {
                continue;
            }

            remove(serverLevel, registrationPos);
        }
    }

    static boolean hasCoverage(ResourceLocation dimension) {
        return INDEX.hasCoverage(dimension);
    }

    static boolean checkWithinRange(BlockPos soundPos, ResourceLocation dimension) {
        return INDEX.isCovered(soundPos, dimension);
    }

    static void update(ServerLevel level, BlockPos registrationPos, BlockPos effectCenter, short range) {
        ResourceLocation dimension = dimension(level);
        if (!INDEX.update(registrationPos, effectCenter, dimension, range)) {
            return;
        }

        sendToDimension(level, new EndSculkSilencerUpdatePacket(registrationPos, effectCenter, dimension, range, range > 0));
    }

    static void updateMoving(ServerLevel level, BlockPos registrationPos, BlockPos effectCenter, short range) {
        ResourceLocation dimension = dimension(level);
        MOVING_LAST_SEEN_BY_DIMENSION.computeIfAbsent(dimension, ignored -> new HashMap<>()).put(registrationPos.immutable(), level.getGameTime());
        update(level, registrationPos, effectCenter, range);
    }

    static void remove(ServerLevel level, BlockPos registrationPos) {
        ResourceLocation dimension = dimension(level);
        if (!INDEX.remove(registrationPos, dimension)) {
            return;
        }

        sendToDimension(level, new EndSculkSilencerUpdatePacket(registrationPos, registrationPos, dimension, (short) 0, false));
    }

    static void removeMoving(ServerLevel level, BlockPos registrationPos) {
        ResourceLocation dimension = dimension(level);
        Map<BlockPos, Long> movingRegistrations = MOVING_LAST_SEEN_BY_DIMENSION.get(dimension);
        if (movingRegistrations != null) {
            movingRegistrations.remove(registrationPos);
            if (movingRegistrations.isEmpty()) {
                MOVING_LAST_SEEN_BY_DIMENSION.remove(dimension);
            }
        }
        remove(level, registrationPos);
    }

    static void removeDimension(ServerLevel level) {
        ResourceLocation dimension = dimension(level);
        MOVING_LAST_SEEN_BY_DIMENSION.remove(dimension);
        for (EndSculkSilencerInstance silencer : INDEX.removeDimension(dimension)) {
            sendToDimension(level, new EndSculkSilencerUpdatePacket(silencer.registrationPos(), silencer.effectCenter(), dimension, (short) 0, false));
        }
    }

    static void clear() {
        MOVING_LAST_SEEN_BY_DIMENSION.clear();
        INDEX.clear();
    }

    static void sendToClient(ServerPlayer serverPlayer) {
        CatnipServices.NETWORK.sendToClient(serverPlayer, EndSculkSilencerResetPacket.INSTANCE);
        ResourceLocation dimension = dimension(serverPlayer.level());
        for (EndSculkSilencerInstance silencer : INDEX.getInstances(dimension)) {
            CatnipServices.NETWORK.sendToClient(serverPlayer, new EndSculkSilencerUpdatePacket(silencer.registrationPos(), silencer.effectCenter(), dimension, silencer.range(), true));
        }
    }

    private static void pruneMovingRegistrations(ServerLevel level, long gameTime) {
        ResourceLocation dimension = dimension(level);
        Map<BlockPos, Long> movingRegistrations = MOVING_LAST_SEEN_BY_DIMENSION.get(dimension);
        if (movingRegistrations == null) {
            return;
        }

        List<BlockPos> staleRegistrations = movingRegistrations.entrySet().stream().filter(registrationEntry -> gameTime - registrationEntry.getValue() > MOVING_REGISTRATION_TIMEOUT_TICKS).map(Entry::getKey).toList();
        for (BlockPos registrationPos : staleRegistrations) {
            removeMoving(level, registrationPos);
        }
    }

    private static ResourceLocation dimension(Level level) {
        return level.dimension().location();
    }

    private static void sendToDimension(ServerLevel level, EndSculkSilencerUpdatePacket packet) {
        for (ServerPlayer player : level.players()) {
            CatnipServices.NETWORK.sendToClient(player, packet);
        }
    }
}
