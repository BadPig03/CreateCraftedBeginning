package net.ty.createcraftedbeginning.content.end.endsculksilencer;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class GlobalEndSculkSilencerManager {
    private static final float TICK_RATE = EndSculkSilencerBlockEntity.LAZY_TICK_RATE * 2;

    private final Map<UUID, EndSculkSilencerInstance> silencers = new HashMap<>();

    public boolean checkWithinRange(BlockPos soundPos, String dimension) {
        return silencers.values().stream().anyMatch(instance -> Objects.equals(instance.dimension, dimension) && EndSculkSilencerInstance.isWithinChunkRange(soundPos, instance.blockPos, instance.range));
    }

    public boolean canUpdate(@NotNull BlockPos blockPos, String dimension, short range) {
        UUID uuid = EndSculkSilencerInstance.calculateUUID(blockPos, dimension);
        return !silencers.containsKey(uuid) || silencers.get(uuid).range != range;
    }

    public void add(BlockPos blockPos, String dimension, short range) {
        UUID uuid = EndSculkSilencerInstance.calculateUUID(blockPos, dimension);
        silencers.put(uuid, new EndSculkSilencerInstance(blockPos, dimension, range));
    }

    public void remove(BlockPos blockPos, String dimension) {
        UUID uuid = EndSculkSilencerInstance.calculateUUID(blockPos, dimension);
        if (!silencers.containsKey(uuid)) {
            return;
        }

        silencers.remove(uuid);
    }

    public void tick(@NotNull Level level) {
        if (level.getGameTime() % TICK_RATE != 0) {
            return;
        }

        List<UUID> toRemove = new ArrayList<>();
        silencers.values().forEach(instance -> {
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

        toRemove.forEach(silencers::remove);
    }

    public void clear() {
        silencers.clear();
    }

    public void sendToClients(ServerPlayer serverPlayer) {
        silencers.values().forEach(instance -> CatnipServices.NETWORK.sendToClient(serverPlayer, new EndSculkSilencerUpdatePacket(instance.blockPos, instance.dimension, instance.range, true)));
    }
}
