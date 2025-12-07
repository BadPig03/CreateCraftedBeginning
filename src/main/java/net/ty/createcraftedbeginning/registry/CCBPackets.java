package net.ty.createcraftedbeginning.registry;

import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.createmod.catnip.net.base.CatnipPacketRegistry;
import net.createmod.catnip.net.base.CatnipPacketRegistry.PacketType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.MountedStorageSyncPacketWithGas;
import net.ty.createcraftedbeginning.compat.jei.airtights.AirtightHandheldDrillGhostItemSubmitPacket;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightBoostElytraPacket;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.AirtightCannonPacket;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.AirtightHandheldDrillAnimationPacket;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.AirtightHandheldDrillInventoryPacket;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.AirtightHandheldDrillScreenPacket;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterOverlayPacket;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackUUIDPacket;

import java.util.Arrays;
import java.util.Locale;

public enum CCBPackets implements PacketTypeProvider {
    AIRTIGHT_CANNON(AirtightCannonPacket.class, AirtightCannonPacket.STREAM_CODEC),
    AIRTIGHT_HANDHELD_DRILL_ANIMATION(AirtightHandheldDrillAnimationPacket.class, AirtightHandheldDrillAnimationPacket.STREAM_CODEC),
    AIRTIGHT_HANDHELD_DRILL_SCREEN(AirtightHandheldDrillScreenPacket.class, AirtightHandheldDrillScreenPacket.STREAM_CODEC),
    AIRTIGHT_HANDHELD_DRILL_INVENTORY(AirtightHandheldDrillInventoryPacket.class, AirtightHandheldDrillInventoryPacket.STREAM_CODEC),
    AIRTIGHT_HANDHELD_DRILL_GHOST_ITEM_SUBMIT(AirtightHandheldDrillGhostItemSubmitPacket.class, AirtightHandheldDrillGhostItemSubmitPacket.STREAM_CODEC),
    AIRTIGHT_BOOST_ELYTRA(AirtightBoostElytraPacket.class, AirtightBoostElytraPacket.STREAM_CODEC),

    GAS_CANISTER_OVERLAY(GasCanisterOverlayPacket.class, GasCanisterOverlayPacket.STREAM_CODEC),
    GAS_CANISTER_PACK_UUID(GasCanisterPackUUIDPacket.class, GasCanisterPackUUIDPacket.STREAM_CODEC),

    MOUNTED_STORAGE_SYNC_WITH_GAS(MountedStorageSyncPacketWithGas.class, MountedStorageSyncPacketWithGas.STREAM_CODEC);

    private final PacketType<?> type;

    <T extends BasePacketPayload> CCBPackets(Class<T> clazz, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        String name = name().toLowerCase(Locale.ROOT);
        type = new PacketType<>(new Type<>(CreateCraftedBeginning.asResource(name)), clazz, codec);
    }

    public static void register() {
        CatnipPacketRegistry packetRegistry = new CatnipPacketRegistry(CreateCraftedBeginning.MOD_ID, 1);
        Arrays.stream(values()).map(packet -> packet.type).forEach(packetRegistry::registerPacket);
        packetRegistry.registerAllPackets();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends CustomPacketPayload> Type<T> getType() {
        return (Type<T>) type.type();
    }
}
