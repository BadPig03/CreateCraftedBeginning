package net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.platform.client.ClientRenderBridge;
import net.ty.createcraftedbeginning.registry.CCBPackets;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record AirtightFractionationTowerFailurePacket(BlockPos min, BlockPos max, @Nullable String reason) implements ClientboundPacketPayload {
    public static final StreamCodec<ByteBuf, AirtightFractionationTowerFailurePacket> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, AirtightFractionationTowerFailurePacket::min, BlockPos.STREAM_CODEC, AirtightFractionationTowerFailurePacket::max, ByteBufCodecs.STRING_UTF8, AirtightFractionationTowerFailurePacket::reason, AirtightFractionationTowerFailurePacket::new);

    public AirtightFractionationTowerFailurePacket {
        min = min.immutable();
        max = max.immutable();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        if (reason == null) {
            return;
        }

        ClientRenderBridge.showPlacementBounds(player, "airtight_fractionation_tower", min, new AABB(min).minmax(new AABB(max)), "gui.warnings." + reason);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.AIRTIGHT_FRACTIONATION_TOWER_FAILURE;
    }
}
