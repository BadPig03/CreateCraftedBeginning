package net.ty.createcraftedbeginning.mixin.common.create;

import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.content.crates.FilteredCrateMountedItemStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = CarriageContraption.class, remap = false)
abstract class CarriageContraptionMixin {
    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "swapStorageAfterAssembly", at = @At("RETURN"))
    private void ccb$swapStorageAfterAssembly(CarriageContraptionEntity entity, CallbackInfo callback) {
        CarriageContraption contraption = (CarriageContraption) (Object) this;
        FilteredCrateMountedItemStorage.bindLevel(contraption.getStorage(), entity.level());
    }
}
