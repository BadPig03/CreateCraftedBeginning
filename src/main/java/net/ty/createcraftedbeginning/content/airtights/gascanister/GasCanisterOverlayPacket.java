package net.ty.createcraftedbeginning.content.airtights.gascanister;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.registry.CCBPackets;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public record GasCanisterOverlayPacket(GasStack content, long capacity, int packType) implements ClientboundPacketPayload {
    public static final String COMPOUND_KEY_OVERLAY = "CreateCraftedBeginningGasCanisterOverlay";
    public static final String COMPOUND_KEY_CONTENT = "Content";
    public static final String COMPOUND_KEY_CAPACITY = "Capacity";
    public static final String COMPOUND_KEY_PACK_TYPE = "PackType";

    public static final StreamCodec<RegistryFriendlyByteBuf, GasCanisterOverlayPacket> STREAM_CODEC = StreamCodec.composite(GasStack.OPTIONAL_STREAM_CODEC, GasCanisterOverlayPacket::content, ByteBufCodecs.VAR_LONG, GasCanisterOverlayPacket::capacity, ByteBufCodecs.VAR_INT, GasCanisterOverlayPacket::packType, GasCanisterOverlayPacket::new);

    @Override
    public void handle(@NotNull LocalPlayer player) {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.put(COMPOUND_KEY_CONTENT, content.saveOptional(player.level().registryAccess()));
        compoundTag.putLong(COMPOUND_KEY_CAPACITY, capacity);
        compoundTag.putInt(COMPOUND_KEY_PACK_TYPE, packType);
        player.getPersistentData().put(COMPOUND_KEY_OVERLAY, compoundTag);
    }

    @Contract(pure = true)
    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.GAS_CANISTER_OVERLAY;
    }
}
