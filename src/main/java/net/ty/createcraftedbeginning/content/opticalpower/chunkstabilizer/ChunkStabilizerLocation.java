package net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record ChunkStabilizerLocation(ResourceKey<Level> dimension, Vec3 position, UUID host, int part) {
    public ChunkStabilizerLocation {
        if (!Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)) {
            throw new IllegalArgumentException("Chunk stabilizer world position must have finite x, y and z coordinates; got " + position + '.');
        }
    }

    public static ChunkStabilizerLocation read(FriendlyByteBuf buffer) {
        return new ChunkStabilizerLocation(buffer.readResourceKey(Registries.DIMENSION), new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()), buffer.readUUID(), buffer.readVarInt());
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeResourceKey(dimension);
        buffer.writeDouble(position.x);
        buffer.writeDouble(position.y);
        buffer.writeDouble(position.z);
        buffer.writeUUID(host);
        buffer.writeVarInt(part);
    }

    public GlobalPos blockPosition() {
        return GlobalPos.of(dimension, BlockPos.containing(position));
    }
}
