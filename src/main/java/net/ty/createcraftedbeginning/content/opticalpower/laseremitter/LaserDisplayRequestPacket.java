package net.ty.createcraftedbeginning.content.opticalpower.laseremitter;

import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.registry.CCBPackets;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record LaserDisplayRequestPacket(ResourceLocation dimension, BlockPos pos, UUID instance) implements ServerboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, LaserDisplayRequestPacket> STREAM_CODEC = StreamCodec.of((buffer, packet) -> {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeBlockPos(packet.pos);
        buffer.writeUUID(packet.instance);
    }, buffer -> new LaserDisplayRequestPacket(buffer.readResourceLocation(), buffer.readBlockPos(), buffer.readUUID()));

    @Override
    public void handle(ServerPlayer player) {
        Level level = player.level();
        if (!level.dimension().location().equals(dimension) || !level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof LaserEmitterBlockEntity emitter) || !emitter.getDisplayInstance().equals(instance)) {
            return;
        }

        LaserServerDisplayQueue.request(emitter, player);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.LASER_DISPLAY_REQUEST;
    }
}
