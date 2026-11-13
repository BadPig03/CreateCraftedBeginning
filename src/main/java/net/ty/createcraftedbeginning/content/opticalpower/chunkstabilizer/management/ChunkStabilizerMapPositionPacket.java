package net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.management;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.ChunkStabilizerLocation;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.map.ChunkStabilizerMapSelection;
import net.ty.createcraftedbeginning.registry.CCBPackets;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record ChunkStabilizerMapPositionPacket(int containerId, UUID request, UUID identity, long generation, long sequence, long tick, @Nullable ChunkStabilizerLocation location, ChunkStabilizerMapSelection selection) implements ClientboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, ChunkStabilizerMapPositionPacket> STREAM_CODEC = StreamCodec.of((buffer, packet) -> {
        buffer.writeVarInt(packet.containerId());
        buffer.writeUUID(packet.request());
        buffer.writeUUID(packet.identity());
        buffer.writeVarLong(packet.generation());
        buffer.writeVarLong(packet.sequence());
        buffer.writeLong(packet.tick());
        buffer.writeNullable(packet.location(), (output, position) -> Objects.requireNonNull(position).write(output));
        packet.selection().write(buffer);
    }, buffer -> new ChunkStabilizerMapPositionPacket(buffer.readVarInt(), buffer.readUUID(), buffer.readUUID(), buffer.readVarLong(), buffer.readVarLong(), buffer.readLong(), buffer.readNullable(ChunkStabilizerLocation::read), ChunkStabilizerMapSelection.read(buffer)));

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        if (!(player.containerMenu instanceof ChunkStabilizerMenu menu) || menu.containerId != containerId) {
            return;
        }

        menu.maps().accept(this);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.CHUNK_STABILIZER_MAP_POSITION;
    }
}
