package net.ty.createcraftedbeginning.api.gas.gases;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;

@FunctionalInterface
public interface IMountedStorageManagerWithGas {
    void handleSyncWithGas(MountedStorageSyncPacketWithGas packet, AbstractContraptionEntity entity);
}
