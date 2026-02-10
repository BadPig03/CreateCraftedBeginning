package net.ty.createcraftedbeginning.mixin;

import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.ty.createcraftedbeginning.registry.CCBMobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractPiglin.class)
public abstract class AbstractPiglinMixin {
    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "isConverting", at = @At("RETURN"), cancellable = true)
    private void ccb$isConverting(CallbackInfoReturnable<Boolean> cir) {
        AbstractPiglin piglin = (AbstractPiglin) (Object) this;
        if (piglin.hasEffect(CCBMobEffects.ZOMBIFICATION_IMMUNITY)) {
            cir.setReturnValue(false);
            return;
        }
        if (!piglin.hasEffect(CCBMobEffects.ZOMBIFICATION)) {
            return;
        }

        cir.setReturnValue(true);
    }
}
