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

public record AirtightHandheldDrillAnimationPacket(float progress) implements ServerboundPacketPayload {
    public static final String COMPOUND_KEY_ANIMATION = "CreateCraftedBeginningDrillAnimation";
    public static final StreamCodec<ByteBuf, AirtightHandheldDrillAnimationPacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.FLOAT, AirtightHandheldDrillAnimationPacket::progress, AirtightHandheldDrillAnimationPacket::new);

    @Override
    public void handle(@NotNull ServerPlayer player) {
        ItemStack drill = player.getMainHandItem();
        if (!drill.is(CCBItems.AIRTIGHT_HANDHELD_DRILL)) {
            return;
        }

        player.getPersistentData().putFloat(COMPOUND_KEY_ANIMATION, progress);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.AIRTIGHT_HANDHELD_DRILL_ANIMATION;
    }
}
