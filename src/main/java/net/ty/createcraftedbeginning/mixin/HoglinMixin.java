package net.ty.createcraftedbeginning.mixin;

import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.ty.createcraftedbeginning.registry.CCBMobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Hoglin.class)
public class HoglinMixin {
    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "isConverting", at = @At("RETURN"), cancellable = true)
    private void ccb$isConverting(CallbackInfoReturnable<Boolean> cir) {
        Hoglin hoglin = (Hoglin) (Object) this;
        if (hoglin.hasEffect(CCBMobEffects.ZOMBIFICATION_IMMUNITY)){
            cir.setReturnValue(false);
            return;
        }
        if (!hoglin.hasEffect(CCBMobEffects.ZOMBIFICATION)) {
            return;
        }

        cir.setReturnValue(true);
    }
}