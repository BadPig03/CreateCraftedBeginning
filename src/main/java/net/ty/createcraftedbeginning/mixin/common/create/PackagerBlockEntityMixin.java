package net.ty.createcraftedbeginning.mixin.common.create;

import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonItem;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasPackagerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = PackagerBlockEntity.class, remap = false)
public abstract class PackagerBlockEntityMixin {
    @SuppressWarnings("ConstantValue")
    @Inject(method = "unwrapBox", at = @At("HEAD"), cancellable = true)
    private void ccb$unwrapBox(ItemStack box, boolean simulate, CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof GasPackagerBlockEntity || !BalloonItem.containsGas(box)) {
            return;
        }

        callback.setReturnValue(false);
    }
}
