package net.ty.createcraftedbeginning.mixin;

import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingInventory;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.recipes.CuttingWithGasRecipe;
import net.ty.createcraftedbeginning.recipe.SequencedAssemblyWithGasRecipe;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Mixin(SawBlockEntity.class)
public abstract class SawBlockEntityMixin {
    @Shadow
    public ProcessingInventory inventory;
    @Shadow
    private FilteringBehaviour filtering;
    @Shadow
    private int recipeIndex;

    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "getRecipes", at = @At("RETURN"), cancellable = true, remap = false)
    private void injectCuttingRecipeWithGas(CallbackInfoReturnable<List<RecipeHolder<?>>> cir) {
        SawBlockEntity self = (SawBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null) {
            return;
        }

        List<RecipeHolder<?>> originalRecipes = cir.getReturnValue();
        Optional<RecipeHolder<CuttingWithGasRecipe>> assemblyRecipe = SequencedAssemblyWithGasRecipe.getRecipe(level, inventory.getStackInSlot(0), CCBRecipeTypes.CUTTING_WITH_GAS.getType(), CuttingWithGasRecipe.class);
        if (assemblyRecipe.isPresent() && filtering.test(assemblyRecipe.get().value().getResultItem(level.registryAccess()))) {
            ArrayList<RecipeHolder<?>> newRecipes = new ArrayList<>(originalRecipes);
            newRecipes.addFirst(assemblyRecipe.get());
            cir.setReturnValue(newRecipes);
        }
    }

    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "applyRecipe", at = @At("HEAD"), cancellable = true, remap = false)
    private void applyCuttingRecipeWithGas(CallbackInfo ci) {
        SawBlockEntity self = (SawBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null) {
            return;
        }

        List<RecipeHolder<?>> recipes = getRecipes();
        if (recipes.isEmpty()) {
            return;
        }
        if (recipeIndex >= recipes.size()) {
            recipeIndex = 0;
        }

        Recipe<?> currentRecipe = recipes.get(recipeIndex).value();
        if (currentRecipe instanceof CuttingWithGasRecipe recipe) {
            ItemStack input = inventory.getStackInSlot(0);
            int rolls = input.getCount();
            inventory.clear();

            List<ItemStack> results = new LinkedList<>();
            for (int roll = 0; roll < rolls; roll++) {
                List<ItemStack> rolledResults = recipe.rollResults(level.random);
                results.addAll(rolledResults);
            }

            for (int slot = 0; slot < results.size() && slot + 1 < inventory.getSlots(); slot++) {
                inventory.setStackInSlot(slot + 1, results.get(slot));
            }

            ci.cancel();
        }
    }

    @Shadow
    abstract List<RecipeHolder<? extends Recipe<?>>> getRecipes();
}
