package net.ty.createcraftedbeginning.mixin.create;

import com.simibubi.create.content.kinetics.belt.transport.BeltMovementHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades.MovementEfficiencyUpgrade;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(BeltMovementHandler.class)
public abstract class BeltMovementHandlerMixin {
    @Inject(method = "canBeTransported", at = @At("RETURN"), cancellable = true)
    private static void ccb$canBeTransported(Entity entity, @NotNull CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() || !(entity instanceof Player player)) {
            return;
        }

        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (!boots.is(CCBItems.AIRTIGHT_BOOTS) || !MovementEfficiencyUpgrade.INSTANCE.isEnabled(boots)) {
            return;
        }

        cir.setReturnValue(false);
    }
}
