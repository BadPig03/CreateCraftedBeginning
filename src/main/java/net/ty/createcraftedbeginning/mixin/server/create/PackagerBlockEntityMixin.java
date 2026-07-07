package net.ty.createcraftedbeginning.mixin.server.create;

import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonUtils;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasPackagerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PackagerBlockEntity.class, remap = false)
public abstract class PackagerBlockEntityMixin {
    @SuppressWarnings("ConstantValue")
    @Inject(method = "unwrapBox", at = @At("HEAD"), cancellable = true)
    private void ccb$unwrapBox(ItemStack box, boolean simulate, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof GasPackagerBlockEntity || !BalloonUtils.containsGasContents(box)) {
            return;
        }

        cir.setReturnValue(false);
    }
}
