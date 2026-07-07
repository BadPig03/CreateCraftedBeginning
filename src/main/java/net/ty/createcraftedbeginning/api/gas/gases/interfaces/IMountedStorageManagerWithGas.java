package net.ty.createcraftedbeginning.api.gas.gases.interfaces;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.MountedGasStorageWrapper;
import net.ty.createcraftedbeginning.api.gas.gases.packets.MountedStorageSyncWithGasPacket;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IMountedStorageManagerWithGas {
    MountedGasStorageWrapper ccb$getGases();

    void ccb$handleSyncWithGas(MountedStorageSyncWithGasPacket packet, AbstractContraptionEntity entity);

    void ccb$setGases(MountedGasStorageWrapper gases);
}
