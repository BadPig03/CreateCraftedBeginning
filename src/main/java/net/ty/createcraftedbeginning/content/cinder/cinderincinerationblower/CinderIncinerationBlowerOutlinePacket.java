package net.ty.createcraftedbeginning.content.cinder.cinderincinerationblower;

import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.ponder.api.PonderPalette;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.registry.CCBPackets;

public record CinderIncinerationBlowerOutlinePacket(BlockPos blockPos, float speed) implements ClientboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, CinderIncinerationBlowerOutlinePacket> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, CinderIncinerationBlowerOutlinePacket::blockPos, ByteBufCodecs.FLOAT, CinderIncinerationBlowerOutlinePacket::speed, CinderIncinerationBlowerOutlinePacket::new);

    @OnlyIn(Dist.CLIENT)
    @Override
    public void handle(LocalPlayer player) {
        if (!GogglesItem.isWearingGoggles(player) || !CCBConfig.client().enableCinderIncinerationBlowerOutline.get() || !(player.level().getBlockEntity(blockPos) instanceof CinderIncinerationBlowerBlockEntity blower)) {
            return;
        }

        Outliner.getInstance().chaseAABB(blower, CinderIncinerationBlowerBlockEntity.calculateArea(blockPos, speed)).colored(PonderPalette.OUTPUT.getColor()).withFaceTextures(AllSpecialTextures.CHECKERED, AllSpecialTextures.HIGHLIGHT_CHECKERED).lineWidth(0.0625f);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.CINDER_INCINERATION_BLOWER_OUTLINE;
    }
}
