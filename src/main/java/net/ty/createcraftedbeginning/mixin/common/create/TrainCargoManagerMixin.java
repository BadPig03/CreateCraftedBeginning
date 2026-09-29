package net.ty.createcraftedbeginning.mixin.common.create;

import com.simibubi.create.content.contraptions.MountedStorageManager;
import com.simibubi.create.content.contraptions.minecart.TrainCargoManager;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.gas.mounted.CargoTrackedMountedGasStorageWrapper;
import net.ty.createcraftedbeginning.gas.mounted.MountedGasStorageAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = TrainCargoManager.class, remap = false)
public abstract class TrainCargoManagerMixin extends MountedStorageManager {
    @Shadow
    protected abstract void changeDetected();

    @Inject(method = "initialize", at = @At("TAIL"))
    private void ccb$initialize(CallbackInfo callback) {
        MountedGasStorageAccess withGas = (MountedGasStorageAccess) this;
        withGas.ccb$setGasStorage(new CargoTrackedMountedGasStorageWrapper(withGas.ccb$getGasStorage(), this::changeDetected));
    }
}
