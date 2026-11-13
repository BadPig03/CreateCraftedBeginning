package net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.management;

import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkBinding;
import net.ty.createcraftedbeginning.registry.CCBPackets;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record ChunkStabilizerRenamePacket(int containerId, String name) implements ServerboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, ChunkStabilizerRenamePacket> STREAM_CODEC = StreamCodec.of((buffer, packet) -> {
        buffer.writeVarInt(packet.containerId());
        buffer.writeUtf(packet.name(), LoadingNetworkBinding.MAX_NAME_LENGTH);
    }, buffer -> new ChunkStabilizerRenamePacket(buffer.readVarInt(), buffer.readUtf(LoadingNetworkBinding.MAX_NAME_LENGTH)));

    @Override
    public void handle(ServerPlayer player) {
        if (!(player.containerMenu instanceof ChunkStabilizerMenu menu) || menu.containerId != containerId || !menu.stillValid(player)) {
            return;
        }

        menu.rename(name);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.CHUNK_STABILIZER_RENAME;
    }
}
