package net.ty.createcraftedbeginning.mixin.common.create;

import com.simibubi.create.content.logistics.packager.repackager.PackageRepackageHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = PackageRepackageHelper.class, remap = false)
public abstract class PackageRepackageHelperMixin {
    @SuppressWarnings("MethodMayBeStatic")
    @Inject(method = "isFragmented", at = @At("HEAD"), cancellable = true)
    private void ccb$isFragmented(ItemStack box, CallbackInfoReturnable<Boolean> callback) {
        if (!BalloonItem.containsGas(box)) {
            return;
        }

        callback.setReturnValue(false);
    }
}
