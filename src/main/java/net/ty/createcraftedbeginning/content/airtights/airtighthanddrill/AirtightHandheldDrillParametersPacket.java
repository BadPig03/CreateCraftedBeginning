package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.templates.AirtightHandheldDrillMiningTemplates;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBPackets;
import org.jetbrains.annotations.NotNull;

public record AirtightHandheldDrillParametersPacket(AirtightHandheldDrillMiningTemplates template, BlockPos sizeParams, Direction direction, BlockPos relativeParams) implements ServerboundPacketPayload {
    public static final StreamCodec<ByteBuf, AirtightHandheldDrillParametersPacket> STREAM_CODEC = StreamCodec.composite(AirtightHandheldDrillMiningTemplates.STREAM_CODEC, AirtightHandheldDrillParametersPacket::template, BlockPos.STREAM_CODEC, AirtightHandheldDrillParametersPacket::sizeParams, Direction.STREAM_CODEC, AirtightHandheldDrillParametersPacket::direction, BlockPos.STREAM_CODEC, AirtightHandheldDrillParametersPacket::relativeParams, AirtightHandheldDrillParametersPacket::new);

    @Override
    public void handle(@NotNull ServerPlayer player) {
        ItemStack drill = player.getMainHandItem();
        if (!drill.is(CCBItems.AIRTIGHT_HANDHELD_DRILL)) {
            return;
        }

        drill.set(CCBDataComponents.DRILL_MINING_TEMPLATE, template);
        drill.set(CCBDataComponents.DRILL_MINING_SIZE, sizeParams);
        drill.set(CCBDataComponents.DRILL_MINING_DIRECTION, direction);
        drill.set(CCBDataComponents.DRILL_MINING_RELATIVE_POSITION, relativeParams);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.AIRTIGHT_HANDHELD_DRILL_PARAMETERS;
    }
}
