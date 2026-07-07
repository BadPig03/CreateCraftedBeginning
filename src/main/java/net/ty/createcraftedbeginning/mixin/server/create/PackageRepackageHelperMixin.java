package net.ty.createcraftedbeginning.mixin.server.create;

import com.simibubi.create.content.logistics.packager.repackager.PackageRepackageHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = PackageRepackageHelper.class, remap = false)
public abstract class PackageRepackageHelperMixin {
    @Inject(method = "isFragmented", at = @At("HEAD"), cancellable = true)
    private static void ccb$isFragmented(ItemStack box, CallbackInfoReturnable<Boolean> cir) {
        if (!BalloonUtils.containsGasContents(box)) {
            return;
        }

        cir.setReturnValue(false);
    }
}
