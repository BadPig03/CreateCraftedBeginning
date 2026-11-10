package net.ty.createcraftedbeginning.content.opticalpower.amethystloader.management;

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
public record AmethystLoaderRenamePacket(int containerId, String name) implements ServerboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, AmethystLoaderRenamePacket> STREAM_CODEC = StreamCodec.of((buffer, packet) -> {
        buffer.writeVarInt(packet.containerId());
        buffer.writeUtf(packet.name(), LoadingNetworkBinding.MAX_NAME_LENGTH);
    }, buffer -> new AmethystLoaderRenamePacket(buffer.readVarInt(), buffer.readUtf(LoadingNetworkBinding.MAX_NAME_LENGTH)));

    @Override
    public void handle(ServerPlayer player) {
        if (!(player.containerMenu instanceof AmethystLoaderMenu menu) || menu.containerId != containerId || !menu.stillValid(player)) {
            return;
        }

        menu.rename(name);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.AMETHYST_LOADER_RENAME;
    }
}
