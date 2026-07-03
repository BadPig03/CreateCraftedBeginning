package net.ty.createcraftedbeginning.mixin;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Player;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(Player.class)
public abstract class PlayerMixin {
    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "getCurrentItemAttackStrengthDelay", at = @At("RETURN"), cancellable = true)
    private void ccb$getCurrentItemAttackStrengthDelay(CallbackInfoReturnable<Float> cir) {
        Player player = (Player) (Object) this;
        if (!player.getMainHandItem().is(CCBItems.AIRTIGHT_HANDHELD_DRILL)) {
            return;
        }

        cir.setReturnValue(0.0f);
    }
}
