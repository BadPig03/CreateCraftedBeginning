package net.ty.createcraftedbeginning.content.airtights.gasfilter;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.ty.createcraftedbeginning.registry.CCBPackets;
import org.jetbrains.annotations.NotNull;

public record GasFilterScreenPacket(boolean blacklist) implements ServerboundPacketPayload {
    public static final StreamCodec<ByteBuf, GasFilterScreenPacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, GasFilterScreenPacket::blacklist, GasFilterScreenPacket::new);

    @Override
    public void handle(@NotNull ServerPlayer player) {
        if (!(player.containerMenu instanceof GasFilterMenu menu)) {
            return;
        }

        menu.blacklist = blacklist;
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.GAS_FILTER_SCREEN;
    }
}
