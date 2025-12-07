package net.ty.createcraftedbeginning.compat.jei;

import com.simibubi.create.AllItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IModIngredientRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.fluids.FluidType;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.compat.jei.airtights.AirtightHandheldDrillGhostIngredientHandler;
import net.ty.createcraftedbeginning.compat.jei.category.CCBRecipeCategory;
import net.ty.createcraftedbeginning.compat.jei.category.CCBRecipeCategory.Builder;
import net.ty.createcraftedbeginning.compat.jei.category.CCBRecipeCategory.Factory;
import net.ty.createcraftedbeginning.compat.jei.category.CoolingCategory;
import net.ty.createcraftedbeginning.compat.jei.category.EnergizationCategory;
import net.ty.createcraftedbeginning.compat.jei.category.GasInjectionCategory;
import net.ty.createcraftedbeginning.compat.jei.category.MysteriousItemConversionCategory;
import net.ty.createcraftedbeginning.compat.jei.category.PressurizationCategory;
import net.ty.createcraftedbeginning.compat.jei.category.SequencedAssemblyWithGasCategory;
import net.ty.createcraftedbeginning.compat.jei.category.WindChargingCategory;
import net.ty.createcraftedbeginning.compat.jei.category.gas.GasStackHelper;
import net.ty.createcraftedbeginning.compat.jei.category.gas.GasStackRenderer;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.AirtightHandheldDrillScreen;
import net.ty.createcraftedbeginning.data.CCBGasRegistries;
import net.ty.createcraftedbeginning.recipe.ConversionRecipe;
import net.ty.createcraftedbeginning.recipe.CoolingRecipe;
import net.ty.createcraftedbeginning.recipe.EnergizationRecipe;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipe;
import net.ty.createcraftedbeginning.recipe.PressurizationRecipe;
import net.ty.createcraftedbeginning.recipe.SequencedAssemblyWithGasRecipe;
import net.ty.createcraftedbeginning.recipe.WindChargingRecipe;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@SuppressWarnings("unused")
@JeiPlugin
public class JEIPlugin implements IModPlugin {
    public static final IIngredientType<GasStack> GAS_STACK = () -> GasStack.class;
    public static final GasStackHelper GAS_STACK_HELPER = new GasStackHelper();

    public static IJeiRuntime runtime;
    private final List<CCBRecipeCategory<?>> allCategories = new ArrayList<>();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void consumeTypedRecipes(Consumer<RecipeHolder<?>> consumer, RecipeType<?> type) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return;
        }

        List<? extends RecipeHolder<?>> map = Minecraft.getInstance().getConnection().getRecipeManager().getAllRecipesFor((RecipeType) type);
        if (map.isEmpty()) {
            return;
        }

        map.forEach(consumer);
    }

    private static void registerGasStackIngredients(@NotNull IModIngredientRegistration registry) {
        List<GasStack> types = CCBGasRegistries.GAS_REGISTRY.holders().filter(Objects::nonNull).filter(gas -> !gas.value().isEmpty()).map(gas -> new GasStack(gas, FluidType.BUCKET_VOLUME)).toList();
        GAS_STACK_HELPER.setColorHelper(registry.getColorHelper());
        registry.register(GAS_STACK, types, GAS_STACK_HELPER, new GasStackRenderer(), Gas.HOLDER_CODEC.xmap(gas -> new GasStack(gas, FluidType.BUCKET_VOLUME), GasStack::getGasHolder));
    }

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return CreateCraftedBeginning.asResource("jei_plugin");
    }

    @Override
    public void registerIngredients(@NotNull IModIngredientRegistration registry) {
        registerGasStackIngredients(registry);
    }

    @Override
    public void registerCategories(@NotNull IRecipeCategoryRegistration registration) {
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
    public void registerGuiHandlers(@NotNull IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(AirtightHandheldDrillScreen.class, new AirtightHandheldDrillGhostIngredientHandler());
    }

    @Override
    public void onRuntimeAvailable(@NotNull IJeiRuntime runtime) {
        JEIPlugin.runtime = runtime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    private void loadCategories() {
        allCategories.clear();
        CCBRecipeCategory<?> mysteryConversion = builder(ConversionRecipe.class).addRecipes(() -> MysteriousItemConversionCategory.RECIPES).itemIcon(CCBBlocks.EMPTY_BREEZE_COOLER_BLOCK).emptyBackground(177, 50).build("mystery_conversion", MysteriousItemConversionCategory::new);
        CCBRecipeCategory<?> energization = builder(EnergizationRecipe.class).addTypedRecipes(CCBRecipeTypes.ENERGIZATION).catalyst(CCBBlocks.BREEZE_CHAMBER_BLOCK::get).catalyst(CCBBlocks.AIRTIGHT_TANK_BLOCK::get).doubleItemIcon(CCBBlocks.BREEZE_CHAMBER_BLOCK, CCBBlocks.AIRTIGHT_TANK_BLOCK).emptyBackground(177, 70).build("energization", EnergizationCategory::new);
        CCBRecipeCategory<?> pressurization = builder(PressurizationRecipe.class).addTypedRecipes(CCBRecipeTypes.PRESSURIZATION).catalyst(CCBBlocks.AIR_COMPRESSOR_BLOCK::get).catalyst(CCBBlocks.BREEZE_COOLER_BLOCK::get).doubleItemIcon(CCBBlocks.AIR_COMPRESSOR_BLOCK, CCBBlocks.BREEZE_COOLER_BLOCK).emptyBackground(177, 70).build("pressurization", PressurizationCategory::new);
        CCBRecipeCategory<?> cooling = builder(CoolingRecipe.class).addTypedRecipes(CCBRecipeTypes.COOLING).catalyst(CCBBlocks.BREEZE_COOLER_BLOCK::get).itemIcon(CCBBlocks.BREEZE_COOLER_BLOCK).emptyBackground(177, 50).build("cooling", CoolingCategory::new);
        CCBRecipeCategory<?> wind_charging = builder(WindChargingRecipe.class).addTypedRecipes(CCBRecipeTypes.WIND_CHARGING).catalyst(CCBBlocks.BREEZE_CHAMBER_BLOCK::get).itemIcon(CCBBlocks.BREEZE_CHAMBER_BLOCK).emptyBackground(177, 50).build("wind_charging", WindChargingCategory::new);
        CCBRecipeCategory<?> gas_injection = builder(GasInjectionRecipe.class).addTypedRecipes(CCBRecipeTypes.GAS_INJECTION).catalyst(CCBBlocks.GAS_INJECTION_CHAMBER_BLOCK::get).doubleItemIcon(CCBBlocks.GAS_INJECTION_CHAMBER_BLOCK, CCBItems.GAS_CANISTER).emptyBackground(177, 70).build("gas_injection", GasInjectionCategory::new);
        CCBRecipeCategory<?> sequenced_assembly_with_gas = builder(SequencedAssemblyWithGasRecipe.class).addTypedRecipes(CCBRecipeTypes.SEQUENCED_ASSEMBLY_WITH_GAS).doubleItemIcon(AllItems.PRECISION_MECHANISM.get(), CCBItems.GAS_CANISTER).emptyBackground(180, 115).build("sequenced_assembly_with_gas", SequencedAssemblyWithGasCategory::new);
    }

    @Contract("_ -> new")
    private <T extends Recipe<? extends RecipeInput>> @NotNull CategoryBuilder<T> builder(Class<T> recipeClass) {
        return new CategoryBuilder<>(recipeClass);
    }

    private class CategoryBuilder<T extends Recipe<?>> extends Builder<T> {
        public CategoryBuilder(Class<? extends T> recipeClass) {
            super(recipeClass);
        }

        @Override
        public @NotNull CCBRecipeCategory<T> build(@NotNull ResourceLocation id, @NotNull Factory<T> factory) {
            CCBRecipeCategory<T> category = super.build(id, factory);
            allCategories.add(category);
            return category;
        }
    }
}
