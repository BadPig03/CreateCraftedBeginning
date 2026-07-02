package net.ty.createcraftedbeginning.content.end.endsculksilencer;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GlobalEndSculkSilencerManager {
    private static final float TICK_RATE = 10;
    private static final Map<UUID, EndSculkSilencerInstance> SILENCERS = new HashMap<>();

    private GlobalEndSculkSilencerManager() {
    }

    public static void tick(Level level) {
        if (level.getGameTime() % TICK_RATE != 0) {
            return;
        }

        List<UUID> toRemove = new ArrayList<>();
        SILENCERS.values().forEach(instance -> {
            String dimension = instance.dimension;
            if (!Objects.equals(dimension, level.dimension().location().toString())) {
                return;
            }

            BlockPos blockPos = instance.blockPos;
            if (level.getBlockEntity(blockPos) instanceof EndSculkSilencerBlockEntity) {
                return;
            }

            toRemove.add(instance.uuid);
        });
        if (toRemove.isEmpty()) {
            return;
        }

        toRemove.forEach(SILENCERS::remove);
    }

    public static boolean checkWithinRange(BlockPos soundPos, String dimension) {
        return SILENCERS.values().stream().anyMatch(instance -> Objects.equals(instance.dimension, dimension) && EndSculkSilencerInstance.isWithinChunkRange(soundPos, instance.blockPos, instance.range));
    }

    public static boolean canUpdate(BlockPos blockPos, String dimension, short range) {
        UUID uuid = EndSculkSilencerInstance.calculateUUID(blockPos, dimension);
        return !SILENCERS.containsKey(uuid) || SILENCERS.get(uuid).range != range;
    }

    public static void add(BlockPos blockPos, String dimension, short range) {
        UUID uuid = EndSculkSilencerInstance.calculateUUID(blockPos, dimension);
        SILENCERS.put(uuid, new EndSculkSilencerInstance(blockPos, dimension, range));
    }

    public static void remove(BlockPos blockPos, String dimension) {
        UUID uuid = EndSculkSilencerInstance.calculateUUID(blockPos, dimension);
        if (!SILENCERS.containsKey(uuid)) {
            return;
        }

        SILENCERS.remove(uuid);
    }

    public static void clear() {
        SILENCERS.clear();
    }

    public static void sendToClients(ServerPlayer serverPlayer) {
        SILENCERS.values().forEach(instance -> CatnipServices.NETWORK.sendToClient(serverPlayer, new EndSculkSilencerUpdatePacket(instance.blockPos, instance.dimension, instance.range, true)));
    }
}
