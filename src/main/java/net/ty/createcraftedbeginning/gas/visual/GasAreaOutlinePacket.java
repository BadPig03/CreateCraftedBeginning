package net.ty.createcraftedbeginning.gas.visual;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseArea;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.client.ClientRenderBridge;
import net.ty.createcraftedbeginning.registry.CCBPackets;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record GasAreaOutlinePacket(BlockPos sourcePos, Optional<Direction> direction, GasReleaseArea area, int color) implements ClientboundPacketPayload {
    public static final StreamCodec<ByteBuf, GasAreaOutlinePacket> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, GasAreaOutlinePacket::sourcePos, ByteBufCodecs.optional(Direction.STREAM_CODEC), GasAreaOutlinePacket::direction, ByteBufCodecs.DOUBLE.map(GasReleaseArea::new, GasReleaseArea::sideLength), GasAreaOutlinePacket::area, ByteBufCodecs.VAR_INT, GasAreaOutlinePacket::color, GasAreaOutlinePacket::new);

    public static void send(ServerLevel level, BlockPos sourcePos, Optional<Direction> direction, GasReleaseArea area, int color) {
        CatnipServices.NETWORK.sendToClientsAround(level, SubLevelBridge.resolve(level, sourcePos).blockPos(), 64, new GasAreaOutlinePacket(sourcePos, direction, area, color));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        ClientRenderBridge.showGasAreaOutline(player, sourcePos, direction, area, color);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.GAS_AREA_OUTLINE;
    }
}
