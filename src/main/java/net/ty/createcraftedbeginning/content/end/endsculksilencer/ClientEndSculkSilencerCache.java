package net.ty.createcraftedbeginning.content.end.endsculksilencer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum ClientEndSculkSilencerCache {
    INSTANCE;

    private final EndSculkSilencerIndex index = new EndSculkSilencerIndex();

    public boolean checkWithinRange(BlockPos soundPos, String dimension) {
        return index.isCovered(soundPos, dimension);
    }

    public void add(BlockPos blockPos, String dimension, short range) {
        index.update(blockPos, dimension, range);
    }

    public void remove(BlockPos blockPos, String dimension) {
        index.remove(blockPos, dimension);
    }

    public void clear() {
        index.clear();
    }
}
