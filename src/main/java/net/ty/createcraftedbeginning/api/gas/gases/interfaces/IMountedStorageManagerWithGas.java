package net.ty.createcraftedbeginning.api.gas.gases.interfaces;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.packets.MountedStorageSyncWithGasPacket;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@FunctionalInterface
public interface IMountedStorageManagerWithGas {
    void handleSyncWithGas(MountedStorageSyncWithGasPacket packet, AbstractContraptionEntity entity);
}
