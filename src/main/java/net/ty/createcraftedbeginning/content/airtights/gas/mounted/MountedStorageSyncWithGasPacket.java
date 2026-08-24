package net.ty.createcraftedbeginning.content.airtights.gas.mounted;

import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IMountedStorageManagerWithGas;
import net.ty.createcraftedbeginning.registry.CCBPackets;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record MountedStorageSyncWithGasPacket(int contraptionId, Map<BlockPos, MountedItemStorage> items, Map<BlockPos, MountedFluidStorage> fluids, Map<BlockPos, MountedGasStorage> gases) implements ClientboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, MountedStorageSyncWithGasPacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.INT, MountedStorageSyncWithGasPacket::contraptionId, ByteBufCodecs.map(HashMap::new, BlockPos.STREAM_CODEC, MountedItemStorage.STREAM_CODEC), MountedStorageSyncWithGasPacket::items, ByteBufCodecs.map(HashMap::new, BlockPos.STREAM_CODEC, MountedFluidStorage.STREAM_CODEC), MountedStorageSyncWithGasPacket::fluids, ByteBufCodecs.map(HashMap::new, BlockPos.STREAM_CODEC, MountedGasStorage.STREAM_CODEC), MountedStorageSyncWithGasPacket::gases, MountedStorageSyncWithGasPacket::new);

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.MOUNTED_STORAGE_SYNC_WITH_GAS;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        if (!(player.level().getEntity(contraptionId) instanceof AbstractContraptionEntity contraption) || !(contraption.getContraption().getStorage() instanceof IMountedStorageManagerWithGas gasStorageManager)) {
            return;
        }

        gasStorageManager.ccb$handleSyncWithGas(this, contraption);
    }
}
