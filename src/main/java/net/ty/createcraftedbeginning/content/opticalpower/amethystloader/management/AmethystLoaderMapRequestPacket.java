package net.ty.createcraftedbeginning.content.opticalpower.amethystloader.management;

import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.ty.createcraftedbeginning.registry.CCBPackets;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record AmethystLoaderMapRequestPacket(int containerId, @Nullable UUID loader, UUID request) implements ServerboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, AmethystLoaderMapRequestPacket> STREAM_CODEC = StreamCodec.of((buffer, packet) -> {
        buffer.writeVarInt(packet.containerId());
        buffer.writeUUID(packet.request());
        buffer.writeNullable(packet.loader(), (output, identity) -> output.writeUUID(Objects.requireNonNull(identity)));
    }, buffer -> {
        int containerId = buffer.readVarInt();
        UUID request = buffer.readUUID();
        return new AmethystLoaderMapRequestPacket(containerId, buffer.readNullable(input -> input.readUUID()), request);
    });

    @Override
    public void handle(ServerPlayer player) {
        if (!(player.containerMenu instanceof AmethystLoaderMenu menu) || menu.containerId != containerId || !menu.stillValid(player)) {
            return;
        }

        menu.maps().select(loader, request);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.AMETHYST_LOADER_MAP_REQUEST;
    }
}
