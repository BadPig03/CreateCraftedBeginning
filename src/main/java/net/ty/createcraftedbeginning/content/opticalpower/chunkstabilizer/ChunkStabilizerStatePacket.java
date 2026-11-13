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
public record ChunkStabilizerStatePacket(int entityId, BlockPos localPos, boolean active, int color, boolean beamEnabled) implements ClientboundPacketPayload {
    public static final StreamCodec<ByteBuf, ChunkStabilizerStatePacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.INT, ChunkStabilizerStatePacket::entityId, BlockPos.STREAM_CODEC, ChunkStabilizerStatePacket::localPos, ByteBufCodecs.BOOL, ChunkStabilizerStatePacket::active, ByteBufCodecs.INT, ChunkStabilizerStatePacket::color, ByteBufCodecs.BOOL, ChunkStabilizerStatePacket::beamEnabled, ChunkStabilizerStatePacket::new);

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.CHUNK_STABILIZER_STATE;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        ChunkStabilizerClient.updateMovingState(player, this);
    }
}
