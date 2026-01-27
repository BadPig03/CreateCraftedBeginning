package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBPackets;
import org.jetbrains.annotations.NotNull;

public record AirtightHandheldDrillInventoryPacket(int optionFlags) implements ServerboundPacketPayload {
    public static final StreamCodec<ByteBuf, AirtightHandheldDrillInventoryPacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, AirtightHandheldDrillInventoryPacket::optionFlags, AirtightHandheldDrillInventoryPacket::new);

    @Override
    public void handle(@NotNull ServerPlayer player) {
        ItemStack drill = player.getMainHandItem();
        if (!drill.is(CCBItems.AIRTIGHT_HANDHELD_DRILL) || !(player.containerMenu instanceof AirtightHandheldDrillMenu menu)) {
            return;
        }

        menu.getDrillInventory().setStackInSlot(AirtightHandheldDrillMenu.UPGRADE_SLOT_INDEX, ItemStack.EMPTY);
        menu.updateFlags(optionFlags);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.AIRTIGHT_HANDHELD_DRILL_INVENTORY;
    }
}
