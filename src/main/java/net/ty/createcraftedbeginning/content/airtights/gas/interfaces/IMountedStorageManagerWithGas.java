package net.ty.createcraftedbeginning.content.airtights.gas.interfaces;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.content.airtights.gas.mounted.MountedGasStorageWrapper;
import net.ty.createcraftedbeginning.content.airtights.gas.mounted.MountedStorageSyncWithGasPacket;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IMountedStorageManagerWithGas {
    MountedGasStorageWrapper ccb$getGases();

    void ccb$handleSyncWithGas(MountedStorageSyncWithGasPacket packet, AbstractContraptionEntity entity);

    void ccb$setGases(MountedGasStorageWrapper gases);
}
