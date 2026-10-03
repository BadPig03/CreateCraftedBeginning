package net.ty.createcraftedbeginning.mixin.common.create;

import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packagerLink.PackagerLinkBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasunpackager.GasUnpackagerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = PackagerLinkBlockEntity.class, remap = false)
public abstract class PackagerLinkBlockEntityMixin {
    @SuppressWarnings("MethodMayBeStatic")
    @Inject(method = "getPackager", at = @At("RETURN"), cancellable = true)
    private void ccb$getPackager(CallbackInfoReturnable<PackagerBlockEntity> callback) {
        if (!(callback.getReturnValue() instanceof GasUnpackagerBlockEntity)) {
            return;
        }

        callback.setReturnValue(null);
    }
}
