package net.ty.createcraftedbeginning.compat.jei.airtights;

import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasFilterMenu;
import net.ty.createcraftedbeginning.registry.CCBPackets;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record GasFilterGhostItemSubmitPacket(ItemStack item) implements ServerboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, GasFilterGhostItemSubmitPacket> STREAM_CODEC = StreamCodec.composite(ItemStack.OPTIONAL_STREAM_CODEC, GasFilterGhostItemSubmitPacket::item, GasFilterGhostItemSubmitPacket::new);

    @Override
    public void handle(@NotNull ServerPlayer player) {
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
