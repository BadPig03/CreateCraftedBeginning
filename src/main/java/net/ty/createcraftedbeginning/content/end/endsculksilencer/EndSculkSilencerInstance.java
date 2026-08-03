package net.ty.createcraftedbeginning.content.end.endsculksilencer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.LongConsumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record EndSculkSilencerInstance(BlockPos registrationPos, BlockPos effectCenter, ResourceLocation dimension, short range) {
    public EndSculkSilencerInstance {
        registrationPos = registrationPos.immutable();
        effectCenter = effectCenter.immutable();
        if (range <= 0) {
            throw new IllegalArgumentException("Silencer range must be positive");
        }
    }

    public static long chunkKey(BlockPos blockPos) {
        return chunkKey(blockPos.getX() >> 4, blockPos.getZ() >> 4);
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return chunkX & 0xFFFFFFFFL | (long) chunkZ << 32;
    }

    public boolean hasSameCoverage(BlockPos otherEffectCenter, short otherRange) {
        return range == otherRange && chunkKey(effectCenter) == chunkKey(otherEffectCenter);
    }

    public void forEachCoveredChunk(LongConsumer consumer) {
        int centerChunkX = effectCenter.getX() >> 4;
        int centerChunkZ = effectCenter.getZ() >> 4;
        int chunkRadius = range - 1;
        for (int chunkX = centerChunkX - chunkRadius; chunkX <= centerChunkX + chunkRadius; chunkX++) {
            for (int chunkZ = centerChunkZ - chunkRadius; chunkZ <= centerChunkZ + chunkRadius; chunkZ++) {
                consumer.accept(chunkKey(chunkX, chunkZ));
            }
        }
    }
}
