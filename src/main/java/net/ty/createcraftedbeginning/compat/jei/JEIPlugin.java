package net.ty.createcraftedbeginning.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.compat.jei.category.CCBRecipeCategory;
import net.ty.createcraftedbeginning.compat.jei.category.CoolingCategory;
import net.ty.createcraftedbeginning.compat.jei.category.GasInjectionCategory;
import net.ty.createcraftedbeginning.compat.jei.category.MysteriousItemConversionCategory;
import net.ty.createcraftedbeginning.compat.jei.category.PressurizationCategory;
import net.ty.createcraftedbeginning.compat.jei.category.SuperCoolingCategory;
import net.ty.createcraftedbeginning.recipe.ConversionRecipe;
import net.ty.createcraftedbeginning.recipe.CoolingRecipe;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipe;
import net.ty.createcraftedbeginning.recipe.PressurizationRecipe;
import net.ty.createcraftedbeginning.recipe.SuperCoolingRecipe;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@JeiPlugin
public class JEIPlugin implements IModPlugin {
    private static final ResourceLocation ID = CreateCraftedBeginning.asResource("jei_plugin");
    public static IJeiRuntime runtime;
    private final List<CCBRecipeCategory<?>> allCategories = new ArrayList<>();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void consumeTypedRecipes(Consumer<RecipeHolder<?>> consumer, RecipeType<?> type) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return;
        }
        List<? extends RecipeHolder<?>> map = Minecraft.getInstance().getConnection().getRecipeManager().getAllRecipesFor((RecipeType) type);
        if (!map.isEmpty()) {
            map.forEach(consumer);
        }
    }

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        loadCategories();
        registration.addRecipeCategories(allCategories.toArray(IRecipeCategory[]::new));
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        allCategories.forEach(c -> c.registerRecipes(registration));
    }

    @Override
	public void registerRecipeCatalysts(@NotNull IRecipeCatalystRegistration registration) {
		allCategories.forEach(c -> c.registerCatalysts(registration));
	}

    @Override
    public void onRuntimeAvailable(@NotNull IJeiRuntime runtime) {
        JEIPlugin.runtime = runtime;
    }

    private void loadCategories() {
        allCategories.clear();

        CCBRecipeCategory<?> mysteryConversion = builder(ConversionRecipe.class).addRecipes(() -> MysteriousItemConversionCategory.RECIPES).itemIcon(CCBBlocks.EMPTY_BREEZE_CHAMBER_BLOCK.get()).emptyBackground(177, 50).build("mystery_conversion", MysteriousItemConversionCategory::new);
        CCBRecipeCategory<?> pressurization = builder(PressurizationRecipe.class).addTypedRecipes(CCBRecipeTypes.PRESSURIZATION).catalyst(CCBBlocks.AIR_COMPRESSOR_BLOCK::get).catalyst(CCBBlocks.BREEZE_CHAMBER_BLOCK::get).itemIcon(CCBBlocks.AIR_COMPRESSOR_BLOCK.get()).emptyBackground(177, 70).build("pressurization", PressurizationCategory::new);
        CCBRecipeCategory<?> cooling = builder(CoolingRecipe.class).addTypedRecipes(CCBRecipeTypes.COOLING).catalyst(CCBBlocks.BREEZE_CHAMBER_BLOCK::get).itemIcon(CCBBlocks.BREEZE_CHAMBER_BLOCK.get()).emptyBackground(177, 50).build("cooling", CoolingCategory::new);
        CCBRecipeCategory<?> super_cooling = builder(SuperCoolingRecipe.class).addTypedRecipes(CCBRecipeTypes.SUPER_COOLING).catalyst(CCBBlocks.BREEZE_CHAMBER_BLOCK::get).itemIcon(CCBBlocks.BREEZE_CHAMBER_BLOCK.get()).emptyBackground(177, 50).build("super_cooling", SuperCoolingCategory::new);
        CCBRecipeCategory<?> gas_injection = builder(GasInjectionRecipe.class).addTypedRecipes(CCBRecipeTypes.GAS_INJECTION).catalyst(CCBBlocks.GAS_INJECTION_CHAMBER_BLOCK::get).doubleItemIcon(CCBBlocks.GAS_INJECTION_CHAMBER_BLOCK.get(), CCBItems.COMPRESSED_AIR_CANISTER).emptyBackground(177, 70).build("gas_injection", GasInjectionCategory::new);
    }

    private <T extends Recipe<? extends RecipeInput>> CategoryBuilder<T> builder(Class<T> recipeClass) {
        return new CategoryBuilder<>(recipeClass);
    }

    private class CategoryBuilder<T extends Recipe<?>> extends CCBRecipeCategory.Builder<T> {
        public CategoryBuilder(Class<? extends T> recipeClass) {
            super(recipeClass);
        }

        @Override
        public @NotNull CCBRecipeCategory<T> build(@NotNull ResourceLocation id, CCBRecipeCategory.@NotNull Factory<T> factory) {
            CCBRecipeCategory<T> category = super.build(id, factory);
            allCategories.add(category);
            return category;
        }
    }
}
