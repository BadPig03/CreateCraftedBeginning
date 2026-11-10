package net.ty.createcraftedbeginning.content.opticalpower.amethystmirror;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.registry.CCBPackets;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record AmethystMirrorAimReplyPacket(ResourceLocation dimension, BlockPos pos, int requestId, boolean applied, AmethystMirrorAimResult result) implements ClientboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, AmethystMirrorAimReplyPacket> STREAM_CODEC = StreamCodec.composite(ResourceLocation.STREAM_CODEC, AmethystMirrorAimReplyPacket::dimension, BlockPos.STREAM_CODEC, AmethystMirrorAimReplyPacket::pos, ByteBufCodecs.VAR_INT, AmethystMirrorAimReplyPacket::requestId, ByteBufCodecs.BOOL, AmethystMirrorAimReplyPacket::applied, AmethystMirrorAimResult.STREAM_CODEC, AmethystMirrorAimReplyPacket::result, AmethystMirrorAimReplyPacket::new);

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        AmethystMirrorAimControls.accept(this, player);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.AMETHYST_MIRROR_AIM_REPLY;
    }
}
