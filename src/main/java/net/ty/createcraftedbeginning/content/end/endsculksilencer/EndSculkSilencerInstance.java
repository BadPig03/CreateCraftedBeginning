package net.ty.createcraftedbeginning.content.end.endsculksilencer;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

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

    public static @NotNull UUID calculateUUID(@NotNull BlockPos blockPos, @NotNull String dimension) {
        byte[] nameBytes = dimension.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES + nameBytes.length);
        buffer.putLong(BlockPos.asLong(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
        buffer.put(nameBytes);
        return UUID.nameUUIDFromBytes(buffer.array());
    }

    public static boolean isWithinChunkRange(@NotNull BlockPos soundPos, @NotNull BlockPos silencerPos, short range) {
        return Mth.abs((soundPos.getX() >> 4) - (silencerPos.getX() >> 4)) < range && Mth.abs((soundPos.getZ() >> 4) - (silencerPos.getZ() >> 4)) < range;
    }
}
