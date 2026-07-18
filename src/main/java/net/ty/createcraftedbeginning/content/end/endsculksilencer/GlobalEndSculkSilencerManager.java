package net.ty.createcraftedbeginning.content.end.endsculksilencer;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GlobalEndSculkSilencerManager {
    private static final int PRUNE_INTERVAL_TICKS = 100;
    private static final EndSculkSilencerIndex INDEX = new EndSculkSilencerIndex();

    private GlobalEndSculkSilencerManager() {
    }

    public static void tick(Level level) {
        if (!(level instanceof ServerLevel serverLevel) || level.getGameTime() % PRUNE_INTERVAL_TICKS != 0) {
            return;
        }

        String dimension = level.dimension().location().toString();
        for (EndSculkSilencerInstance instance : INDEX.getInstances(dimension)) {
            BlockPos blockPos = instance.blockPos();
            if (level.isLoaded(blockPos) && level.getBlockEntity(blockPos) instanceof EndSculkSilencerBlockEntity) {
                continue;
            }

            remove(serverLevel, blockPos);
        }
    }

    public static boolean checkWithinRange(BlockPos soundPos, String dimension) {
        return INDEX.isCovered(soundPos, dimension);
    }

    public static void update(ServerLevel level, BlockPos blockPos, short range) {
        String dimension = level.dimension().location().toString();
        if (!INDEX.update(blockPos, dimension, range)) {
            return;
        }

        sendToDimension(level, new EndSculkSilencerUpdatePacket(blockPos, dimension, range, range > 0));
    }

    public static boolean remove(ServerLevel level, BlockPos blockPos) {
        String dimension = level.dimension().location().toString();
        if (!INDEX.remove(blockPos, dimension)) {
            return false;
        }

        sendToDimension(level, new EndSculkSilencerUpdatePacket(blockPos, dimension, (short) 0, false));
        return true;
    }

    public static void removeDimension(ServerLevel level) {
        String dimension = level.dimension().location().toString();
        List<EndSculkSilencerInstance> removed = INDEX.removeDimension(dimension);
        for (EndSculkSilencerInstance instance : removed) {
            sendToDimension(level, new EndSculkSilencerUpdatePacket(instance.blockPos(), dimension, (short) 0, false));
        }
    }

    public static void clear() {
        INDEX.clear();
    }

    public static void sendToClient(ServerPlayer serverPlayer) {
        CatnipServices.NETWORK.sendToClient(serverPlayer, EndSculkSilencerResetPacket.INSTANCE);

        String dimension = serverPlayer.level().dimension().location().toString();
        for (EndSculkSilencerInstance instance : INDEX.getInstances(dimension)) {
            CatnipServices.NETWORK.sendToClient(serverPlayer, new EndSculkSilencerUpdatePacket(instance.blockPos(), dimension, instance.range(), true));
        }
    }

    private static void sendToDimension(ServerLevel level, EndSculkSilencerUpdatePacket packet) {
        for (ServerPlayer player : level.players()) {
            CatnipServices.NETWORK.sendToClient(player, packet);
        }
    }
}
