package net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.ty.createcraftedbeginning.registry.CCBPackets;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record AirtightHandheldDrillSingleBlockPacket(int hotbarSlot, boolean pressed) implements ServerboundPacketPayload {
    public static final StreamCodec<ByteBuf, AirtightHandheldDrillSingleBlockPacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, AirtightHandheldDrillSingleBlockPacket::hotbarSlot, ByteBufCodecs.BOOL, AirtightHandheldDrillSingleBlockPacket::pressed, AirtightHandheldDrillSingleBlockPacket::new);

    @Override
    public void handle(ServerPlayer player) {
        if (pressed && (!Inventory.isHotbarSlot(hotbarSlot) || hotbarSlot != player.getInventory().selected)) {
            return;
        }

        AirtightHandheldDrillControls.setSingleBlockMode(player, pressed);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.AIRTIGHT_HANDHELD_DRILL_SINGLE_BLOCK;
    }
}
