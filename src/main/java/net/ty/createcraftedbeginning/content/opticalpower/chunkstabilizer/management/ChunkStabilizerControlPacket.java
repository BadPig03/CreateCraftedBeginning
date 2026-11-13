package net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.management;

import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.ty.createcraftedbeginning.registry.CCBPackets;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record ChunkStabilizerControlPacket(int containerId, UUID loader, boolean enabled) implements ServerboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, ChunkStabilizerControlPacket> STREAM_CODEC = StreamCodec.of((buffer, packet) -> {
        buffer.writeVarInt(packet.containerId());
        buffer.writeUUID(packet.loader());
        buffer.writeBoolean(packet.enabled());
    }, buffer -> new ChunkStabilizerControlPacket(buffer.readVarInt(), buffer.readUUID(), buffer.readBoolean()));

    @Override
    public void handle(ServerPlayer player) {
        if (!(player.containerMenu instanceof ChunkStabilizerMenu menu) || menu.containerId != containerId || !menu.stillValid(player)) {
            return;
        }

        menu.control(loader, enabled);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.CHUNK_STABILIZER_CONTROL;
    }
}
