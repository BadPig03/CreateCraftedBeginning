package net.ty.createcraftedbeginning.compat.jei.utils;

import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasFilterMenu;
import net.ty.createcraftedbeginning.registry.CCBPackets;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record GasFilterGhostItemSubmitPacket(ItemStack item) implements ServerboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, GasFilterGhostItemSubmitPacket> STREAM_CODEC = StreamCodec.composite(ItemStack.OPTIONAL_STREAM_CODEC, GasFilterGhostItemSubmitPacket::item, GasFilterGhostItemSubmitPacket::new);

    @Override
    public void handle(ServerPlayer player) {
        if (!(player.containerMenu instanceof GasFilterMenu menu)) {
            return;
        }

        menu.insertDirectly(List.of(item));
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.GAS_FILTER_GHOST_ITEM_SUBMIT;
    }
}
