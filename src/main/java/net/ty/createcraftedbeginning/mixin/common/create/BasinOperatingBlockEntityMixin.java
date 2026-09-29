package net.ty.createcraftedbeginning.mixin.common.create;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.crafting.Recipe;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBasinCooling;
import net.ty.createcraftedbeginning.recipe.ChilledBasinProcessing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = BasinOperatingBlockEntity.class, remap = false)
public abstract class BasinOperatingBlockEntityMixin {
    @Shadow
    protected abstract Optional<BasinBlockEntity> getBasin();

    @ModifyReturnValue(method = "getMatchingRecipes", at = @At("RETURN"))
    private List<Recipe<?>> ccb$getMatchingRecipes(List<Recipe<?>> recipes) {
        if (getBasin().filter(BreezeCoolerBasinCooling::hasChilledSource).isEmpty()) {
            return recipes;
        }

        return ChilledBasinProcessing.prioritizeChilledRecipes(recipes);
    }
}
