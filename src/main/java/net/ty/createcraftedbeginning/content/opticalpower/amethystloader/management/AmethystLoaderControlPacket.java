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
public record AmethystLoaderControlPacket(int containerId, @Nullable UUID loader, boolean enabled) implements ServerboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, AmethystLoaderControlPacket> STREAM_CODEC = StreamCodec.of((buffer, packet) -> {
        buffer.writeVarInt(packet.containerId());
        buffer.writeNullable(packet.loader(), (output, identity) -> output.writeUUID(Objects.requireNonNull(identity)));
        buffer.writeBoolean(packet.enabled());
    }, buffer -> new AmethystLoaderControlPacket(buffer.readVarInt(), buffer.readNullable(input -> input.readUUID()), buffer.readBoolean()));

    @Override
    public void handle(ServerPlayer player) {
        if (!(player.containerMenu instanceof AmethystLoaderMenu menu) || menu.containerId != containerId || !menu.stillValid(player)) {
            return;
        }

        menu.control(loader, enabled);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.AMETHYST_LOADER_CONTROL;
    }
}
