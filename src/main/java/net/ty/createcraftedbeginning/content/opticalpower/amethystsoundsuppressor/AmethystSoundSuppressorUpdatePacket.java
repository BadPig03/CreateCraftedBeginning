package net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor;

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
public record AmethystSoundSuppressorUpdatePacket(AmethystSoundSuppressorIdentity identity, BlockPos effectCenter, ResourceLocation dimension, short range, boolean active) implements ClientboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, AmethystSoundSuppressorUpdatePacket> STREAM_CODEC = StreamCodec.composite(AmethystSoundSuppressorIdentity.STREAM_CODEC, AmethystSoundSuppressorUpdatePacket::identity, BlockPos.STREAM_CODEC, AmethystSoundSuppressorUpdatePacket::effectCenter, ResourceLocation.STREAM_CODEC, AmethystSoundSuppressorUpdatePacket::dimension, ByteBufCodecs.SHORT, AmethystSoundSuppressorUpdatePacket::range, ByteBufCodecs.BOOL, AmethystSoundSuppressorUpdatePacket::active, AmethystSoundSuppressorUpdatePacket::new);

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        if (!active || range <= 0) {
            ClientAmethystSoundSuppressorCache.INSTANCE.remove(identity, dimension);
            return;
        }

        ClientAmethystSoundSuppressorCache.INSTANCE.add(identity, effectCenter, dimension, range);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.AMETHYST_SOUND_SUPPRESSOR_UPDATE;
    }
}
