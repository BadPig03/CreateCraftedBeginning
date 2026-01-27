package net.ty.createcraftedbeginning.api.gas.gases;

import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.registry.CCBPackets;


public record GasAreaOutlinePacket(BlockPos pos, Direction direction, float inflation, int color) implements ClientboundPacketPayload {
    public static final StreamCodec<ByteBuf, GasAreaOutlinePacket> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, GasAreaOutlinePacket::pos, Direction.STREAM_CODEC, GasAreaOutlinePacket::direction, ByteBufCodecs.FLOAT, GasAreaOutlinePacket::inflation, ByteBufCodecs.VAR_INT, GasAreaOutlinePacket::color, GasAreaOutlinePacket::new);

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        if (!GogglesItem.isWearingGoggles(player) || !CCBConfig.client().enableGasAreaOutline.get()) {
            return;
        }

        AABB area = new AABB(pos.relative(direction)).inflate(inflation);
        Outliner.getInstance().chaseAABB(area, area).colored(color).withFaceTextures(AllSpecialTextures.CHECKERED, AllSpecialTextures.HIGHLIGHT_CHECKERED).lineWidth(0.0625f);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.GAS_AREA_OUTLINE;
    }
}
