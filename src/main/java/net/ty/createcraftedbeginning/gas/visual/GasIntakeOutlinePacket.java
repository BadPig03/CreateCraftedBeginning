package net.ty.createcraftedbeginning.gas.visual;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.client.ClientRenderBridge;
import net.ty.createcraftedbeginning.registry.CCBPackets;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record GasIntakeOutlinePacket(BlockPos sourcePos, Direction direction) implements ClientboundPacketPayload {
    public static final StreamCodec<ByteBuf, GasIntakeOutlinePacket> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, GasIntakeOutlinePacket::sourcePos, Direction.STREAM_CODEC, GasIntakeOutlinePacket::direction, GasIntakeOutlinePacket::new);

    public static void send(ServerLevel level, BlockPos sourcePos, Direction direction) {
        CatnipServices.NETWORK.sendToClientsAround(level, SubLevelBridge.resolve(level, sourcePos).blockPos(), 64, new GasIntakeOutlinePacket(sourcePos, direction));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        ClientRenderBridge.showGasIntakeOutline(player, sourcePos, direction);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.GAS_INTAKE_OUTLINE;
    }
}
