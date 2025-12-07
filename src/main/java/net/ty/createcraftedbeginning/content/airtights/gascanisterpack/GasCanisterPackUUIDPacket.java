package net.ty.createcraftedbeginning.content.airtights.gascanisterpack;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.registry.CCBPackets;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record GasCanisterPackUUIDPacket(ItemStack pack, UUID uuid) implements ClientboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, GasCanisterPackUUIDPacket> STREAM_CODEC = StreamCodec.composite(ItemStack.STREAM_CODEC, GasCanisterPackUUIDPacket::pack, UUIDUtil.STREAM_CODEC, GasCanisterPackUUIDPacket::uuid, GasCanisterPackUUIDPacket::new);

    @Override
    public void handle(@NotNull LocalPlayer player) {
        GasCanisterPackUtils.setCanisterPackUUID(pack, uuid);
    }

    @Contract(pure = true)
    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.GAS_CANISTER_PACK_UUID;
    }
}
