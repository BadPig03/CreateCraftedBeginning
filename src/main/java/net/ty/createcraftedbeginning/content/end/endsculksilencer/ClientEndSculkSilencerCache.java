package net.ty.createcraftedbeginning.content.end.endsculksilencer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum ClientEndSculkSilencerCache {
    INSTANCE;

    private final EndSculkSilencerIndex index = new EndSculkSilencerIndex();

    public boolean hasCoverage(ResourceLocation dimension) {
        return index.hasCoverage(dimension);
    }

    public boolean checkWithinRange(BlockPos soundPos, ResourceLocation dimension) {
        return index.isCovered(soundPos, dimension);
    }

    public @Nullable EndSculkSilencerInstance get(BlockPos registrationPos, ResourceLocation dimension) {
        return index.get(registrationPos, dimension);
    }

    public void add(BlockPos registrationPos, BlockPos effectCenter, ResourceLocation dimension, short range) {
        index.update(registrationPos, effectCenter, dimension, range);
    }

    public void remove(BlockPos registrationPos, ResourceLocation dimension) {
        index.remove(registrationPos, dimension);
    }

    public void clear() {
        index.clear();
    }
}
