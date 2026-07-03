package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.ElytraUpgrade;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBPackets;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum AirtightChestplateElytraBoostPacket implements ServerboundPacketPayload {
    INSTANCE;

    public static final StreamCodec<ByteBuf, AirtightChestplateElytraBoostPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public void handle(ServerPlayer player) {
        if (!player.getMainHandItem().isEmpty()) {
            return;
        }

        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chestplate.is(CCBItems.AIRTIGHT_CHESTPLATE)) {
            return;
        }

        ElytraUpgrade.applySpeedBoost(player);
        player.getCooldowns().addCooldown(chestplate.getItem(), 40);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.AIRTIGHT_BOOST_ELYTRA;
    }
}
