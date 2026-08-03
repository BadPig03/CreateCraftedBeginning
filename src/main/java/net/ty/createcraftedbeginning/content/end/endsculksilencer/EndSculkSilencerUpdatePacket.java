package net.ty.createcraftedbeginning.content.end.endsculksilencer;

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
public record EndSculkSilencerUpdatePacket(BlockPos registrationPos, BlockPos effectCenter, ResourceLocation dimension, short range, boolean active) implements ClientboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, EndSculkSilencerUpdatePacket> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, EndSculkSilencerUpdatePacket::registrationPos, BlockPos.STREAM_CODEC, EndSculkSilencerUpdatePacket::effectCenter, ResourceLocation.STREAM_CODEC, EndSculkSilencerUpdatePacket::dimension, ByteBufCodecs.SHORT, EndSculkSilencerUpdatePacket::range, ByteBufCodecs.BOOL, EndSculkSilencerUpdatePacket::active, EndSculkSilencerUpdatePacket::new);

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        if (!active || range <= 0) {
            ClientEndSculkSilencerCache.INSTANCE.remove(registrationPos, dimension);
            return;
        }

        ClientEndSculkSilencerCache.INSTANCE.add(registrationPos, effectCenter, dimension, range);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.END_SCULK_SILENCER_UPDATE;
    }
}
