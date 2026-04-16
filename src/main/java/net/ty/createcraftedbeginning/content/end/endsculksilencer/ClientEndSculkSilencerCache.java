package net.ty.createcraftedbeginning.content.end.endsculksilencer;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public enum ClientEndSculkSilencerCache {
    INSTANCE;

    private final Map<UUID, EndSculkSilencerInstance> silencers = new HashMap<>();

    public boolean checkWithinRange(BlockPos soundPos, String dimension) {
        return silencers.values().stream().anyMatch(instance -> Objects.equals(instance.dimension, dimension) && EndSculkSilencerInstance.isWithinChunkRange(soundPos, instance.blockPos, instance.range));
    }

    public void add(BlockPos blockPos, String dimension, short range) {
        UUID uuid = EndSculkSilencerInstance.calculateUUID(blockPos, dimension);
        silencers.put(uuid, new EndSculkSilencerInstance(blockPos, dimension, range));
    }

    public void remove(BlockPos blockPos, String dimension) {
        UUID uuid = EndSculkSilencerInstance.calculateUUID(blockPos, dimension);
        silencers.remove(uuid);
    }

    public void clear() {
        silencers.clear();
    }
}