package net.ty.createcraftedbeginning.registry;

import net.createmod.catnip.net.base.BasePacketPayload;
import net.createmod.catnip.net.base.CatnipPacketRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.MountedStorageSyncPacketWithGas;
import net.ty.createcraftedbeginning.content.airtightcannon.AirtightCannonPacket;

import java.util.Locale;

public enum CCBPackets implements BasePacketPayload.PacketTypeProvider {
    AIRTIGHT_CANNON(AirtightCannonPacket.class, AirtightCannonPacket.STREAM_CODEC),
    MOUNTED_STORAGE_SYNC_WITH_GAS(MountedStorageSyncPacketWithGas.class, MountedStorageSyncPacketWithGas.STREAM_CODEC);

    private final CatnipPacketRegistry.PacketType<?> type;

    <T extends BasePacketPayload> CCBPackets(Class<T> clazz, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        String name = this.name().toLowerCase(Locale.ROOT);
        this.type = new CatnipPacketRegistry.PacketType<>(new CustomPacketPayload.Type<>(CreateCraftedBeginning.asResource(name)), clazz, codec);
    }

    public static void register() {
        CatnipPacketRegistry packetRegistry = new CatnipPacketRegistry(CreateCraftedBeginning.MOD_ID, 1);
        for (CCBPackets packet : CCBPackets.values()) {
            packetRegistry.registerPacket(packet.type);
        }
        packetRegistry.registerAllPackets();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends CustomPacketPayload> CustomPacketPayload.Type<T> getType() {
        return (CustomPacketPayload.Type<T>) this.type.type();
    }
}
