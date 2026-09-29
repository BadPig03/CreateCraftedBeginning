package net.ty.createcraftedbeginning.gas.mounted;

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
import net.ty.createcraftedbeginning.registry.CCBPackets;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record MountedGasStorageSyncPacket(int contraptionId, Map<BlockPos, MountedItemStorage> items, Map<BlockPos, MountedFluidStorage> fluids, Map<BlockPos, MountedGasStorage> gases) implements ClientboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, MountedGasStorageSyncPacket> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.INT, MountedGasStorageSyncPacket::contraptionId, ByteBufCodecs.map(HashMap::new, BlockPos.STREAM_CODEC, MountedItemStorage.STREAM_CODEC), MountedGasStorageSyncPacket::items, ByteBufCodecs.map(HashMap::new, BlockPos.STREAM_CODEC, MountedFluidStorage.STREAM_CODEC), MountedGasStorageSyncPacket::fluids, ByteBufCodecs.map(HashMap::new, BlockPos.STREAM_CODEC, MountedGasStorage.STREAM_CODEC), MountedGasStorageSyncPacket::gases, MountedGasStorageSyncPacket::new);

    @Override
    public PacketTypeProvider getTypeProvider() {
        return CCBPackets.MOUNTED_GAS_STORAGE_SYNC;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handle(LocalPlayer player) {
        if (!(player.level().getEntity(contraptionId) instanceof AbstractContraptionEntity contraption) || !(contraption.getContraption().getStorage() instanceof MountedGasStorageAccess gasStorageManager)) {
            return;
        }

        gasStorageManager.ccb$handleGasStorageSync(this, contraption);
    }
}
