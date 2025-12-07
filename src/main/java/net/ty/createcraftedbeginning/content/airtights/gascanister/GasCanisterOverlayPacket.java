package net.ty.createcraftedbeginning.content.airtights.gascanister;

import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.registry.CCBPackets;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public record GasCanisterOverlayPacket(GasStack content, long capacity) implements ClientboundPacketPayload {
    public static final String COMPOUND_KEY_CONTENT = CreateCraftedBeginning.MOD_ID + ":gas_canister_overlay_content";
    public static final String COMPOUND_KEY_CAPACITY = CreateCraftedBeginning.MOD_ID + ":gas_canister_overlay_capacity";
    public static final StreamCodec<RegistryFriendlyByteBuf, GasCanisterOverlayPacket> STREAM_CODEC = StreamCodec.composite(GasStack.OPTIONAL_STREAM_CODEC, GasCanisterOverlayPacket::content, ByteBufCodecs.VAR_LONG, GasCanisterOverlayPacket::capacity, GasCanisterOverlayPacket::new);

    @Override
    public void handle(@NotNull LocalPlayer player) {
        CompoundTag data = player.getPersistentData();
        data.put(COMPOUND_KEY_CONTENT, content.saveOptional(player.level().registryAccess()));
        data.putLong(COMPOUND_KEY_CAPACITY, capacity);
    }

    @Contract(pure = true)
    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.GAS_CANISTER_OVERLAY;
    }
}
