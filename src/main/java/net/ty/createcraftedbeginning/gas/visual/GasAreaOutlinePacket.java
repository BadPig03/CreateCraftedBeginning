package net.ty.createcraftedbeginning.gas.visual;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.platform.client.ClientRenderBridge;
import net.ty.createcraftedbeginning.registry.CCBPackets;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record GasAreaOutlinePacket(BlockPos effectPos, float inflation, int color) implements ClientboundPacketPayload {
    public static final StreamCodec<ByteBuf, GasAreaOutlinePacket> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, GasAreaOutlinePacket::effectPos, ByteBufCodecs.FLOAT, GasAreaOutlinePacket::inflation, ByteBufCodecs.VAR_INT, GasAreaOutlinePacket::color, GasAreaOutlinePacket::new);

    public static void send(ServerLevel level, BlockPos effectPos, float inflation, int color) {
        CatnipServices.NETWORK.sendToClientsAround(level, effectPos, 64, new GasAreaOutlinePacket(effectPos, inflation, color));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        ClientRenderBridge.showGasAreaOutline(player, effectPos, inflation, color);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.GAS_AREA_OUTLINE;
    }
}
