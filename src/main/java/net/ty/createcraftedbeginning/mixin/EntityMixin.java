package net.ty.createcraftedbeginning.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightArmorsUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin {
    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "fireImmune", at = @At("RETURN"), cancellable = true)
    private void ccb$fireImmune(CallbackInfoReturnable<Boolean> cir) {
        if (!((Entity) (Object) this instanceof Player player) || !AirtightArmorsUtils.isEntireArmoredUp(player)) {
            return;
        }

        cir.setReturnValue(true);
    }
}
