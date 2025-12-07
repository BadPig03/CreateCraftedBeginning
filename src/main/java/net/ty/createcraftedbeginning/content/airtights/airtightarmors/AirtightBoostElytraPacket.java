package net.ty.createcraftedbeginning.content.airtights.airtightarmors;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.registry.CCBPackets;
import org.jetbrains.annotations.NotNull;

public enum AirtightBoostElytraPacket implements ServerboundPacketPayload {
    INSTANCE;

    public static final StreamCodec<ByteBuf, AirtightBoostElytraPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public void handle(@NotNull ServerPlayer player) {
        float multiplier = AirtightArmorsUtils.getBoostMultiplier(player);
        if (multiplier == 0) {
            return;
        }

        Vec3 lookAngle = player.getLookAngle().scale(0.85f * multiplier);
        Vec3 movement = player.getDeltaMovement().scale(0.5f * multiplier);
        player.setDeltaMovement(movement.add(lookAngle));

        Item chestplate = player.getItemBySlot(EquipmentSlot.CHEST).getItem();
        player.getCooldowns().addCooldown(chestplate, 40);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.AIRTIGHT_BOOST_ELYTRA;
    }
}
