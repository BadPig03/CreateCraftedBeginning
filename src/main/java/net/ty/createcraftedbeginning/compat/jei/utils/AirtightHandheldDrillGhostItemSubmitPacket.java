package net.ty.createcraftedbeginning.compat.jei.utils;

import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.AirtightHandheldDrillMenu;
import net.ty.createcraftedbeginning.registry.CCBPackets;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record AirtightHandheldDrillGhostItemSubmitPacket(ItemStack item) implements ServerboundPacketPayload {
    private static final int PLAYER_INVENTORY_SLOTS = Inventory.INVENTORY_SIZE;

    public static final StreamCodec<RegistryFriendlyByteBuf, AirtightHandheldDrillGhostItemSubmitPacket> STREAM_CODEC = StreamCodec.composite(ItemStack.OPTIONAL_STREAM_CODEC, AirtightHandheldDrillGhostItemSubmitPacket::item, AirtightHandheldDrillGhostItemSubmitPacket::new);

    @Override
    public void handle(ServerPlayer player) {
        if (!(player.containerMenu instanceof AirtightHandheldDrillMenu menu)) {
            return;
        }

        menu.getMenuInventory().setStackInSlot(AirtightHandheldDrillMenu.FILTER_SLOT_INDEX, item);
        menu.getSlot(PLAYER_INVENTORY_SLOTS + AirtightHandheldDrillMenu.FILTER_SLOT_INDEX).setChanged();
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.AIRTIGHT_HANDHELD_DRILL_GHOST_ITEM_SUBMIT;
    }
}
