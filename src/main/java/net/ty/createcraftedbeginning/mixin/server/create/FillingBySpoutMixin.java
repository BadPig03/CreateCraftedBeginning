package net.ty.createcraftedbeginning.mixin.server.create;

import com.simibubi.create.content.fluids.spout.FillingBySpout;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.ty.createcraftedbeginning.api.gas.recipes.FillingWithGasRecipe;
import net.ty.createcraftedbeginning.recipe.SequencedAssemblyWithGasRecipe;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = FillingBySpout.class, remap = false)
public abstract class FillingBySpoutMixin {
    @Inject(method = "canItemBeFilled", at = @At("HEAD"), cancellable = true)
    private static void ccb$canItemBeFilled(Level level, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        SingleRecipeInput input = new SingleRecipeInput(stack);
        Optional<RecipeHolder<FillingWithGasRecipe>> recipeWithGas = SequencedAssemblyWithGasRecipe.getRecipe(level, input, CCBRecipeTypes.FILLING_WITH_GAS.getType(), FillingWithGasRecipe.class);
        if (recipeWithGas.isEmpty()) {
            return;
        }

        cir.setReturnValue(true);
    }

    @Inject(method = "getRequiredAmountForItem", at = @At("HEAD"), cancellable = true)
    private static void ccb$getRequiredAmountForItem(Level level, ItemStack stack, FluidStack availableFluid, CallbackInfoReturnable<Integer> cir) {
        SingleRecipeInput input = new SingleRecipeInput(stack);
        Optional<RecipeHolder<FillingWithGasRecipe>> recipeWithGas = SequencedAssemblyWithGasRecipe.getRecipe(level, input, CCBRecipeTypes.FILLING_WITH_GAS.getType(), FillingWithGasRecipe.class, r -> r.value().matches(input, level) && r.value().getRequiredFluid().test(availableFluid));
        if (recipeWithGas.isEmpty()) {
            return;
        }

        SizedFluidIngredient requiredFluid = recipeWithGas.get().value().getRequiredFluid();
        if (!requiredFluid.test(availableFluid)) {
            return;
        }

        cir.setReturnValue(requiredFluid.amount());
    }

    @Inject(method = "fillItem", at = @At("HEAD"), cancellable = true)
    private static void ccb$fillItem(Level level, int requiredAmount, ItemStack stack, FluidStack availableFluid, CallbackInfoReturnable<ItemStack> cir) {
        SingleRecipeInput input = new SingleRecipeInput(stack);
        FluidStack toFill = availableFluid.copyWithAmount(requiredAmount);
        Optional<RecipeHolder<FillingWithGasRecipe>> recipeWithGas = SequencedAssemblyWithGasRecipe.getRecipe(level, input, CCBRecipeTypes.FILLING_WITH_GAS.getType(), FillingWithGasRecipe.class, r -> r.value().matches(input, level) && r.value().getRequiredFluid().test(availableFluid)).filter(fr -> fr.value().getRequiredFluid().test(toFill));
        if (recipeWithGas.isEmpty()) {
            return;
        }

        List<ItemStack> results = recipeWithGas.get().value().rollResults(level.random);
        availableFluid.shrink(requiredAmount);
        stack.shrink(1);
        cir.setReturnValue(results.isEmpty() ? ItemStack.EMPTY : results.getFirst());
    }
}
