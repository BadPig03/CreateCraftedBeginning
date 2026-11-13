package net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.management;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.registry.CCBPackets;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record ChunkStabilizerSyncPacket(int containerId, ChunkStabilizerSnapshot snapshot) implements ClientboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, ChunkStabilizerSyncPacket> STREAM_CODEC = StreamCodec.of((buffer, packet) -> {
        buffer.writeVarInt(packet.containerId());
        packet.snapshot().write(buffer);
    }, buffer -> new ChunkStabilizerSyncPacket(buffer.readVarInt(), ChunkStabilizerSnapshot.read(buffer)));

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        if (!(player.containerMenu instanceof ChunkStabilizerMenu menu) || menu.containerId != containerId) {
            return;
        }

        menu.acceptSnapshot(snapshot);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.CHUNK_STABILIZER_SYNC;
    }
}
