package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.templates.AirtightHandheldDrillMiningTemplates;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBPackets;
import org.jetbrains.annotations.NotNull;

public record AirtightHandheldDrillScreenPacket(int optionFlags, AirtightHandheldDrillMiningTemplates template, BlockPos sizeParams, Direction direction, BlockPos relativeParams) implements ServerboundPacketPayload {
    public static final StreamCodec<ByteBuf, AirtightHandheldDrillScreenPacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, AirtightHandheldDrillScreenPacket::optionFlags, AirtightHandheldDrillMiningTemplates.STREAM_CODEC, AirtightHandheldDrillScreenPacket::template, BlockPos.STREAM_CODEC, AirtightHandheldDrillScreenPacket::sizeParams, Direction.STREAM_CODEC, AirtightHandheldDrillScreenPacket::direction, BlockPos.STREAM_CODEC, AirtightHandheldDrillScreenPacket::relativeParams, AirtightHandheldDrillScreenPacket::new);

    @Override
    public void handle(@NotNull ServerPlayer player) {
        ItemStack drill = player.getMainHandItem();
        if (!drill.is(CCBItems.AIRTIGHT_HANDHELD_DRILL)) {
            return;
        }

        configureDrill(drill);
    }

    public void configureDrill(ItemStack drill) {
        AirtightHandheldDrillUtils.configureDrill(drill, optionFlags, template, sizeParams, direction, relativeParams);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.AIRTIGHT_HANDHELD_DRILL_SCREEN;
    }
}
