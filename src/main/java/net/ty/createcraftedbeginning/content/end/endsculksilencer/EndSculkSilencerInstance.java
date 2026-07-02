package net.ty.createcraftedbeginning.content.end.endsculksilencer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EndSculkSilencerInstance {
    public BlockPos blockPos;
    public String dimension;
    public short range;
    public UUID uuid;

    public EndSculkSilencerInstance(BlockPos blockPos, String dimension, short range) {
        this.blockPos = blockPos;
        this.dimension = dimension;
        this.range = range;
        uuid = calculateUUID(blockPos, dimension);
    }

    public static UUID calculateUUID(BlockPos blockPos, String dimension) {
        byte[] nameBytes = dimension.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES + nameBytes.length);
        buffer.putLong(BlockPos.asLong(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
        buffer.put(nameBytes);
        return UUID.nameUUIDFromBytes(buffer.array());
    }

    public static boolean isWithinChunkRange(BlockPos soundPos, BlockPos silencerPos, short range) {
        return Mth.abs((soundPos.getX() >> 4) - (silencerPos.getX() >> 4)) < range && Mth.abs((soundPos.getZ() >> 4) - (silencerPos.getZ() >> 4)) < range;
    }
}
