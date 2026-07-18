package net.ty.createcraftedbeginning.api.gas.gases.interfaces;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.MountedGasStorageWrapper;
import net.ty.createcraftedbeginning.api.gas.gases.packets.MountedStorageSyncWithGasPacket;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface IMountedStorageManagerWithGas {
    /**
     * Returns the gas stacks stored by the synchronized mounted storage.
     *
     * @return the resulting mounted gas storage wrapper
     */
    MountedGasStorageWrapper ccb$getGases();

    /**
     * Applies synchronized gas-storage data received from the network.
     *
     * @param packet the packet to use
     * @param entity the entity associated with the operation
     */
    void ccb$handleSyncWithGas(MountedStorageSyncWithGasPacket packet, AbstractContraptionEntity entity);

    /**
     * Replaces the gas stacks stored by the synchronized mounted storage.
     *
     * @param gases the gases to use
     */
    void ccb$setGases(MountedGasStorageWrapper gases);
}
