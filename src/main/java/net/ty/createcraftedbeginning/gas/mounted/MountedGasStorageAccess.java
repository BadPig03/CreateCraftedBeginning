package net.ty.createcraftedbeginning.gas.mounted;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface MountedGasStorageAccess {
    MountedGasStorageWrapper ccb$getGasStorage();

    void ccb$handleGasStorageSync(MountedGasStorageSyncPacket packet, AbstractContraptionEntity entity);

    void ccb$setGasStorage(MountedGasStorageWrapper gasStorage);
}
