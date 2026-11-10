package net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor;

import net.createmod.catnip.codecs.stream.CatnipStreamCodecs;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.registry.CCBPackets;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record AmethystCrystalGrowthPacket(Vec3 crystalCenter, Vec3 suppressorTip, ResourceLocation dimension) implements ClientboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, AmethystCrystalGrowthPacket> STREAM_CODEC = StreamCodec.composite(CatnipStreamCodecs.VEC3, AmethystCrystalGrowthPacket::crystalCenter, CatnipStreamCodecs.VEC3, AmethystCrystalGrowthPacket::suppressorTip, ResourceLocation.STREAM_CODEC, AmethystCrystalGrowthPacket::dimension, AmethystCrystalGrowthPacket::new);

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        Level level = player.level();
        if (!level.dimension().location().equals(dimension)) {
            return;
        }

        AmethystSoundSuppressorClient.showGrowthParticles(level, crystalCenter, suppressorTip);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.AMETHYST_CRYSTAL_GROWTH;
    }
}
