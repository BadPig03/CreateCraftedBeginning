package net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer;

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
public record ChunkStabilizerOutlinePacket(int entityId, BlockPos localPos, boolean showOutline) implements ClientboundPacketPayload {
    public static final StreamCodec<ByteBuf, ChunkStabilizerOutlinePacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.INT, ChunkStabilizerOutlinePacket::entityId, BlockPos.STREAM_CODEC, ChunkStabilizerOutlinePacket::localPos, ByteBufCodecs.BOOL, ChunkStabilizerOutlinePacket::showOutline, ChunkStabilizerOutlinePacket::new);

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.CHUNK_STABILIZER_OUTLINE;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        ChunkStabilizerClient.updateOutline(player, this);
    }
}
