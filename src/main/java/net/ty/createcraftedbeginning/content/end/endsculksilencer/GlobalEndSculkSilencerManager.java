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
public final class GlobalEndSculkSilencerManager {
    private static final int STATIC_PRUNE_INTERVAL_TICKS = 100;
    private static final int MOVING_PRUNE_INTERVAL_TICKS = 20;
    private static final int MOVING_REGISTRATION_TIMEOUT_TICKS = 50;
    private static final EndSculkSilencerIndex INDEX = new EndSculkSilencerIndex();
    private static final Map<ResourceLocation, Map<BlockPos, Long>> MOVING_LAST_SEEN_BY_DIMENSION = new HashMap<>();

    private GlobalEndSculkSilencerManager() {
    }

    public static void tick(Level level) {
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
        for (EndSculkSilencerInstance instance : INDEX.getInstances(dimension)) {
            BlockPos registrationPos = instance.registrationPos();
            if (movingRegistrations != null && movingRegistrations.containsKey(registrationPos)) {
                continue;
            }

            if (level.isLoaded(registrationPos) && level.getBlockEntity(registrationPos) instanceof EndSculkSilencerBlockEntity) {
                continue;
            }

            remove(serverLevel, registrationPos);
        }
    }

    public static boolean hasCoverage(ResourceLocation dimension) {
        return INDEX.hasCoverage(dimension);
    }

    public static boolean checkWithinRange(BlockPos soundPos, ResourceLocation dimension) {
        return INDEX.isCovered(soundPos, dimension);
    }

    public static void update(ServerLevel level, BlockPos registrationPos, BlockPos effectCenter, short range) {
        ResourceLocation dimension = dimension(level);
        if (!INDEX.update(registrationPos, effectCenter, dimension, range)) {
            return;
        }

        sendToDimension(level, new EndSculkSilencerUpdatePacket(registrationPos, effectCenter, dimension, range, range > 0));
    }

    public static void updateMoving(ServerLevel level, BlockPos registrationPos, BlockPos effectCenter, short range) {
        ResourceLocation dimension = dimension(level);
        MOVING_LAST_SEEN_BY_DIMENSION.computeIfAbsent(dimension, ignored -> new HashMap<>()).put(registrationPos.immutable(), level.getGameTime());
        update(level, registrationPos, effectCenter, range);
    }

    public static boolean remove(ServerLevel level, BlockPos registrationPos) {
        ResourceLocation dimension = dimension(level);
        if (!INDEX.remove(registrationPos, dimension)) {
            return false;
        }

        sendToDimension(level, new EndSculkSilencerUpdatePacket(registrationPos, registrationPos, dimension, (short) 0, false));
        return true;
    }

    public static void removeMoving(ServerLevel level, BlockPos registrationPos) {
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

    public static void removeDimension(ServerLevel level) {
        ResourceLocation dimension = dimension(level);
        MOVING_LAST_SEEN_BY_DIMENSION.remove(dimension);
        List<EndSculkSilencerInstance> removed = INDEX.removeDimension(dimension);
        for (EndSculkSilencerInstance instance : removed) {
            sendToDimension(level, new EndSculkSilencerUpdatePacket(instance.registrationPos(), instance.effectCenter(), dimension, (short) 0, false));
        }
    }

    public static void clear() {
        MOVING_LAST_SEEN_BY_DIMENSION.clear();
        INDEX.clear();
    }

    public static void sendToClient(ServerPlayer serverPlayer) {
        CatnipServices.NETWORK.sendToClient(serverPlayer, EndSculkSilencerResetPacket.INSTANCE);
        ResourceLocation dimension = dimension(serverPlayer.level());
        for (EndSculkSilencerInstance instance : INDEX.getInstances(dimension)) {
            CatnipServices.NETWORK.sendToClient(serverPlayer, new EndSculkSilencerUpdatePacket(instance.registrationPos(), instance.effectCenter(), dimension, instance.range(), true));
        }
    }

    private static void pruneMovingRegistrations(ServerLevel level, long gameTime) {
        ResourceLocation dimension = dimension(level);
        Map<BlockPos, Long> movingRegistrations = MOVING_LAST_SEEN_BY_DIMENSION.get(dimension);
        if (movingRegistrations == null) {
            return;
        }

        List<BlockPos> staleRegistrations = movingRegistrations.entrySet().stream().filter(entry -> gameTime - entry.getValue() > MOVING_REGISTRATION_TIMEOUT_TICKS).map(Entry::getKey).toList();
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
