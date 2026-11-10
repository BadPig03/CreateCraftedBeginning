package net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
enum ClientAmethystSoundSuppressorCache {
    INSTANCE;

    private final AmethystSoundSuppressorIndex index = new AmethystSoundSuppressorIndex();

    void add(AmethystSoundSuppressorIdentity identity, BlockPos effectCenter, ResourceLocation dimension, short range) {
        index.update(identity, effectCenter, dimension, range, null);
    }

    void remove(AmethystSoundSuppressorIdentity identity, ResourceLocation dimension) {
        index.remove(identity, dimension, null);
    }

    void clear() {
        index.clear();
    }

    short getRange(AmethystSoundSuppressorIdentity identity, ResourceLocation dimension) {
        return index.getRange(identity, dimension);
    }

    boolean hasCoverage(ResourceLocation dimension) {
        return index.hasCoverage(dimension);
    }

    boolean checkWithinRange(BlockPos soundPos, ResourceLocation dimension) {
        return index.isCovered(soundPos, dimension);
    }
}
