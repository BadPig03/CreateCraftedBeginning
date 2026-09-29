package net.ty.createcraftedbeginning.mixin.common.create;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.crafting.Recipe;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBasinCooling;
import net.ty.createcraftedbeginning.recipe.ChilledBasinProcessing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = BasinRecipe.class, remap = false)
public abstract class BasinRecipeMixin {
    @Inject(method = "apply(Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;Lnet/minecraft/world/item/crafting/Recipe;Z)Z", at = @At("HEAD"), cancellable = true)
    private static void ccb$apply(BasinBlockEntity basin, Recipe<?> recipe, boolean simulate, CallbackInfoReturnable<Boolean> callback) {
        if (!ChilledBasinProcessing.isChilledRecipe(recipe) || BreezeCoolerBasinCooling.hasChilledSource(basin)) {
            return;
        }

        callback.setReturnValue(false);
    }
}
