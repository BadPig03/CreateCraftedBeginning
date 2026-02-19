package net.ty.createcraftedbeginning.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
import org.jetbrains.annotations.NotNull;

public class ConversionRecipe extends StandardProcessingRecipe<RecipeWrapper> {
    public ConversionRecipe(ProcessingRecipeParams params) {
        super(CCBRecipeTypes.CONVERSION, params);
    }

    public static @NotNull RecipeHolder<ConversionRecipe> create(ItemStack from, ItemStack to, String name) {
        ResourceLocation recipe = CreateCraftedBeginning.asResource("conversion_" + name);
        return new RecipeHolder<>(recipe, new Builder<>(ConversionRecipe::new, recipe).withItemIngredients(Ingredient.of(from)).withSingleItemOutput(to).build());
    }

    @Override
    public boolean matches(@NotNull RecipeWrapper inv, @NotNull Level worldIn) {
        return false;
    }

    @Override
    protected int getMaxInputCount() {
        return 1;
    }

    @Override
    protected int getMaxOutputCount() {
        return 1;
    }
}
