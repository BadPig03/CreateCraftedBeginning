package net.ty.createcraftedbeginning.content.airtights.airtightupgrades;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.registry.CCBPackets;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record AirtightUpgradeSyncPacket(List<ResourceLocation> poweredIds) implements ClientboundPacketPayload {
    public static final StreamCodec<ByteBuf, AirtightUpgradeSyncPacket> STREAM_CODEC = StreamCodec.composite(CatnipStreamCodecBuilders.list(ResourceLocation.STREAM_CODEC), AirtightUpgradeSyncPacket::poweredIds, AirtightUpgradeSyncPacket::new);

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        GlobalAirtightUpgradesConsumptionManager.acceptClientSync(player, poweredIds);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.AIRTIGHT_UPGRADE_SYNC;
    }
}
