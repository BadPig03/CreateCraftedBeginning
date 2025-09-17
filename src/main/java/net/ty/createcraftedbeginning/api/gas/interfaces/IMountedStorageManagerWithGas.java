package net.ty.createcraftedbeginning.api.gas.interfaces;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.ty.createcraftedbeginning.api.gas.MountedStorageSyncPacketWithGas;

public interface IMountedStorageManagerWithGas {
    void handleSyncWithGas(MountedStorageSyncPacketWithGas packet, AbstractContraptionEntity entity);
}
