package net.ty.createcraftedbeginning.content.airtights.airtightupgrades;

import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.registry.CCBPackets;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record AirtightUpgradeMaterialsSyncPacket(Map<ResourceLocation, ResourceLocation> materials) implements ClientboundPacketPayload {
    public static final StreamCodec<ByteBuf, AirtightUpgradeMaterialsSyncPacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ResourceLocation.STREAM_CODEC), AirtightUpgradeMaterialsSyncPacket::materials, AirtightUpgradeMaterialsSyncPacket::new);

    public AirtightUpgradeMaterialsSyncPacket {
        materials = Map.copyOf(materials);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        AirtightUpgradeMaterials.acceptClientSync(materials);
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.AIRTIGHT_UPGRADE_MATERIALS_SYNC;
    }
}
