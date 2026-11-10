package net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.registry.CCBPackets;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum AmethystSoundSuppressorResetPacket implements ClientboundPacketPayload {
    INSTANCE;

    public static final StreamCodec<RegistryFriendlyByteBuf, AmethystSoundSuppressorResetPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        ClientAmethystSoundSuppressorCache.INSTANCE.clear();
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.AMETHYST_SOUND_SUPPRESSOR_RESET;
    }
}
