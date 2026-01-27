package net.ty.createcraftedbeginning.mixin;

import com.simibubi.create.content.contraptions.MountedStorageManager;
import com.simibubi.create.content.contraptions.minecart.TrainCargoManager;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.MountedGasStorageWrapper;
import net.ty.createcraftedbeginning.mixin.accessor.MountedStorageManagerAccessor;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TrainCargoManager.class)
public abstract class TrainCargoManagerMixin extends MountedStorageManager {
    @Shadow
    abstract void changeDetected();

    @Inject(method = "initialize", at = @At("TAIL"))
    private void ccb$initialize(CallbackInfo ci) {
        MountedStorageManagerAccessor accessor = (MountedStorageManagerAccessor) this;
        MountedGasStorageWrapper originalGases = accessor.getGases();

        if (originalGases != null) {
            CargoGasWrapper cargoGases = new CargoGasWrapper(originalGases);
            accessor.setGases(cargoGases);
        }
    }

    @Unique
    class CargoGasWrapper extends MountedGasStorageWrapper {
        CargoGasWrapper(@NotNull MountedGasStorageWrapper wrapped) {
            super(wrapped.storages);
        }

        @Override
        public long fill(@NotNull GasStack resource, GasAction action) {
            long filled = super.fill(resource, action);
            if (action.execute() && filled > 0) {
                changeDetected();
            }
            return filled;
        }

        @Override
        public GasStack drain(@NotNull GasStack resource, GasAction action) {
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
}
