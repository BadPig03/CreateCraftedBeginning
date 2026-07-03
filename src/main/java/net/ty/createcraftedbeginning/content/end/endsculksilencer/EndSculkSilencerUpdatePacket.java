package net.ty.createcraftedbeginning.content.end.endsculksilencer;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.registry.CCBPackets;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record EndSculkSilencerUpdatePacket(BlockPos blockPos, String dimension, short range, boolean active) implements ClientboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, EndSculkSilencerUpdatePacket> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, EndSculkSilencerUpdatePacket::blockPos, ByteBufCodecs.STRING_UTF8, EndSculkSilencerUpdatePacket::dimension, ByteBufCodecs.SHORT, EndSculkSilencerUpdatePacket::range, ByteBufCodecs.BOOL, EndSculkSilencerUpdatePacket::active, EndSculkSilencerUpdatePacket::new);

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        if (active) {
            ClientEndSculkSilencerCache.INSTANCE.add(blockPos, dimension, range);
        } else {
            ClientEndSculkSilencerCache.INSTANCE.remove(blockPos, dimension);
        }
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.END_SCULK_SILENCER_UPDATE;
    }
}
