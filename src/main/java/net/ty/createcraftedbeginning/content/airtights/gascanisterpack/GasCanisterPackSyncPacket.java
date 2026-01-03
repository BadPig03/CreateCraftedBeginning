package net.ty.createcraftedbeginning.content.airtights.gascanisterpack;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.registry.CCBPackets;
import org.jetbrains.annotations.Contract;

import java.util.UUID;

public record GasCanisterPackSyncPacket(UUID uuid, GasCanisterPackContents contents) implements ClientboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, GasCanisterPackSyncPacket> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC, GasCanisterPackSyncPacket::uuid, GasCanisterPackContents.STREAM_CODEC, GasCanisterPackSyncPacket::contents, GasCanisterPackSyncPacket::new);

    @Override
    public void handle(LocalPlayer player) {
        CreateCraftedBeginning.GAS_CANISTER_PACK_CONTENTS_DATA_MANAGER.getContentsMap().put(uuid, contents);
    }

    @Contract(pure = true)
    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.GAS_CANISTER_PACK_SYNC;
    }
}
