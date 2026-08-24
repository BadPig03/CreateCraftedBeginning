package net.ty.createcraftedbeginning.content.end.endsculksilencer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
enum ClientEndSculkSilencerCache {
    INSTANCE;

    private final EndSculkSilencerIndex index = new EndSculkSilencerIndex();

    boolean hasCoverage(ResourceLocation dimension) {
        return index.hasCoverage(dimension);
    }

    boolean checkWithinRange(BlockPos soundPos, ResourceLocation dimension) {
        return index.isCovered(soundPos, dimension);
    }

    void add(BlockPos registrationPos, BlockPos effectCenter, ResourceLocation dimension, short range) {
        index.update(registrationPos, effectCenter, dimension, range);
    }

    void remove(BlockPos registrationPos, ResourceLocation dimension) {
        index.remove(registrationPos, dimension);
    }

    void clear() {
        index.clear();
    }
}
