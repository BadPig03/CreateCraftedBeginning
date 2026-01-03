package net.ty.createcraftedbeginning.api.gas.gases;

import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.MountedStorageManager;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.registry.CCBPackets;

import java.util.HashMap;
import java.util.Map;

public record MountedStorageSyncPacketWithGas(int contraptionId, Map<BlockPos, MountedItemStorage> items, Map<BlockPos, MountedFluidStorage> fluids, Map<BlockPos, MountedGasStorage> gases) implements ClientboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, MountedStorageSyncPacketWithGas> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.INT, MountedStorageSyncPacketWithGas::contraptionId, ByteBufCodecs.map(HashMap::new, BlockPos.STREAM_CODEC, MountedItemStorage.STREAM_CODEC), MountedStorageSyncPacketWithGas::items, ByteBufCodecs.map(HashMap::new, BlockPos.STREAM_CODEC, MountedFluidStorage.STREAM_CODEC), MountedStorageSyncPacketWithGas::fluids, ByteBufCodecs.map(HashMap::new, BlockPos.STREAM_CODEC, MountedGasStorage.STREAM_CODEC), MountedStorageSyncPacketWithGas::gases, MountedStorageSyncPacketWithGas::new);

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.MOUNTED_STORAGE_SYNC_WITH_GAS;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (!(level.getEntity(contraptionId) instanceof AbstractContraptionEntity contraption)) {
            return;
        }

        MountedStorageManager storageManager = contraption.getContraption().getStorage();
        if (!(storageManager instanceof IMountedStorageManagerWithGas withGas)) {
            return;
        }

        withGas.handleSyncWithGas(this, contraption);
    }
}
