package net.ty.createcraftedbeginning.mixin.server.create;

import com.simibubi.create.content.contraptions.MountedStorageManager;
import com.simibubi.create.content.contraptions.minecart.TrainCargoManager;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.MountedGasStorageWrapper;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IMountedStorageManagerWithGas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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

    @Unique
    private class ccb$CargoGasWrapper extends MountedGasStorageWrapper {
        ccb$CargoGasWrapper(MountedGasStorageWrapper wrapped) {
            super(wrapped.storages);
        }

        @Override
        public long fill(GasStack resource, GasAction action) {
            long filled = super.fill(resource, action);
            if (action.execute() && filled > 0) {
                changeDetected();
            }
            return filled;
        }

        @Override
        public GasStack drain(GasStack resource, GasAction action) {
            GasStack drained = super.drain(resource, action);
            if (action.execute() && !drained.isEmpty()) {
                changeDetected();
            }
            return drained;
        }

        @Override
        public GasStack drain(long maxDrain, GasAction action) {
            GasStack drained = super.drain(maxDrain, action);
            if (action.execute() && !drained.isEmpty()) {
                changeDetected();
            }
            return drained;
        }
    }

    @Inject(method = "initialize", at = @At("TAIL"))
    private void ccb$initialize(CallbackInfo ci) {
        IMountedStorageManagerWithGas withGas = (IMountedStorageManagerWithGas) this;
        withGas.ccb$setGases(new ccb$CargoGasWrapper(withGas.ccb$getGases()));
    }
}
