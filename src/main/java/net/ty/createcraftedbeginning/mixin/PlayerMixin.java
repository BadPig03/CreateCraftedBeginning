package net.ty.createcraftedbeginning.mixin;

import net.minecraft.world.entity.player.Player;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "getCurrentItemAttackStrengthDelay", cancellable = true, at = @At("RETURN"))
    private void getCurrentItemAttackStrengthDelayWithFasterAttackSpeed(@NotNull CallbackInfoReturnable<Float> callback) {
        Player player = (Player) (Object) this;
        if (!player.getMainHandItem().is(CCBItems.AIRTIGHT_HANDHELD_DRILL)) {
            return;
        }

        callback.setReturnValue(0.0f);
    }
}
