package net.ty.createcraftedbeginning.compat.jei.utils;

import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasFilterMenu;
import net.ty.createcraftedbeginning.registry.CCBPackets;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record GasFilterGhostItemSubmitPacket(GasStack gas, int slot) implements ServerboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, GasFilterGhostItemSubmitPacket> STREAM_CODEC = StreamCodec.composite(GasStack.OPTIONAL_STREAM_CODEC, GasFilterGhostItemSubmitPacket::gas, ByteBufCodecs.VAR_INT, GasFilterGhostItemSubmitPacket::slot, GasFilterGhostItemSubmitPacket::new);

    @Override
    public void handle(ServerPlayer player) {
        if (!(player.containerMenu instanceof GasFilterMenu menu) || !menu.stillValid(player)) {
            return;
        }

        menu.setGas(slot, gas);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.GAS_FILTER_GHOST_ITEM_SUBMIT;
    }
}
