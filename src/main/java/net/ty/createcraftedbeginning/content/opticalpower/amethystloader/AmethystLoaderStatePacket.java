package net.ty.createcraftedbeginning.content.opticalpower.amethystloader;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.registry.CCBPackets;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record AmethystLoaderStatePacket(int entityId, BlockPos localPos, boolean active, int color, boolean beamEnabled) implements ClientboundPacketPayload {
    public static final StreamCodec<ByteBuf, AmethystLoaderStatePacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.INT, AmethystLoaderStatePacket::entityId, BlockPos.STREAM_CODEC, AmethystLoaderStatePacket::localPos, ByteBufCodecs.BOOL, AmethystLoaderStatePacket::active, ByteBufCodecs.INT, AmethystLoaderStatePacket::color, ByteBufCodecs.BOOL, AmethystLoaderStatePacket::beamEnabled, AmethystLoaderStatePacket::new);

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.AMETHYST_LOADER_STATE;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        AmethystLoaderClient.updateMovingState(player, this);
    }
}
