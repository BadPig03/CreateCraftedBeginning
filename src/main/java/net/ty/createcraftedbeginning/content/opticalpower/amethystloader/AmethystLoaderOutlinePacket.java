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
public record AmethystLoaderOutlinePacket(int entityId, BlockPos localPos, boolean showOutline) implements ClientboundPacketPayload {
    public static final StreamCodec<ByteBuf, AmethystLoaderOutlinePacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.INT, AmethystLoaderOutlinePacket::entityId, BlockPos.STREAM_CODEC, AmethystLoaderOutlinePacket::localPos, ByteBufCodecs.BOOL, AmethystLoaderOutlinePacket::showOutline, AmethystLoaderOutlinePacket::new);

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.AMETHYST_LOADER_OUTLINE;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        AmethystLoaderClient.updateOutline(player, this);
    }
}
