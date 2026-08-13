package net.ty.createcraftedbeginning.mixin.compat.functionalstorage;

import com.buuz135.functionalstorage.FunctionalStorage;
import com.buuz135.functionalstorage.block.tile.ControllableDrawerTile;
import com.buuz135.functionalstorage.item.UpgradeItem;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.compat.functionalstorage.GasDrawerBlockEntity;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = UpgradeItem.class, remap = false)
public abstract class UpgradeItemMixin {
    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "getDescription", at = @At("HEAD"), cancellable = true)
    private void ccb$getDescription(ItemStack stack, ControllableDrawerTile<?> tile, CallbackInfoReturnable<Component> cir) {
        if (!(tile instanceof GasDrawerBlockEntity)) {
            return;
        }

        UpgradeItem upgrade = (UpgradeItem) (Object) this;
        if (upgrade == FunctionalStorage.PUSHING_UPGRADE.get()) {
            cir.setReturnValue(CCBLang.translateDirect("compat.functional_storage.drawer_upgrade.push.gas", UpgradeItem.getRelativeDirection(UpgradeItem.getDirection(stack), tile.getFacingDirection()).withStyle(ChatFormatting.GOLD)));
            return;
        }

        if (upgrade == FunctionalStorage.PULLING_UPGRADE.get()) {
            cir.setReturnValue(CCBLang.translateDirect("compat.functional_storage.drawer_upgrade.pull.gas", UpgradeItem.getRelativeDirection(UpgradeItem.getDirection(stack), tile.getFacingDirection()).withStyle(ChatFormatting.GOLD)));
            return;
        }

        if (upgrade != FunctionalStorage.VOID_UPGRADE.get()) {
            return;
        }

        cir.setReturnValue(CCBLang.translateDirect("compat.functional_storage.drawer_upgrade.void.gas"));
    }
}
