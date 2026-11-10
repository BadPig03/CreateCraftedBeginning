package net.ty.createcraftedbeginning.content.opticalpower.amethystmirror;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.registry.CCBPackets;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record AmethystMirrorRotateReplyPacket(ResourceLocation dimension, BlockPos pos, int requestId, boolean accepted, @Nullable Quaternionf rotation) implements ClientboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, AmethystMirrorRotateReplyPacket> STREAM_CODEC = StreamCodec.of((buffer, reply) -> {
        buffer.writeResourceLocation(reply.dimension);
        buffer.writeBlockPos(reply.pos);
        buffer.writeVarInt(reply.requestId);
        buffer.writeBoolean(reply.accepted);
        Quaternionf rotation = reply.rotation;
        buffer.writeBoolean(rotation != null);
        if (rotation == null) {
            return;
        }

        buffer.writeFloat(rotation.x);
        buffer.writeFloat(rotation.y);
        buffer.writeFloat(rotation.z);
        buffer.writeFloat(rotation.w);
    }, buffer -> {
        ResourceLocation dimension = buffer.readResourceLocation();
        BlockPos pos = buffer.readBlockPos();
        int requestId = buffer.readVarInt();
        boolean accepted = buffer.readBoolean();
        if (!buffer.readBoolean()) {
            return new AmethystMirrorRotateReplyPacket(dimension, pos, requestId, accepted, null);
        }

        Quaternionf rotation = new Quaternionf(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
        return new AmethystMirrorRotateReplyPacket(dimension, pos, requestId, accepted, rotation);
    });

    public AmethystMirrorRotateReplyPacket {
        pos = pos.immutable();
        rotation = rotation == null ? null : new Quaternionf(rotation);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        AmethystMirrorRotationTracker.accept(this, player);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.AMETHYST_MIRROR_ROTATE_REPLY;
    }

    @Override
    public @Nullable Quaternionf rotation() {
        if (rotation == null) {
            return null;
        }

        return new Quaternionf(rotation);
    }
}
