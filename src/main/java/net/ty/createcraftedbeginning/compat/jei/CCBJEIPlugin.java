package net.ty.createcraftedbeginning.compat.jei;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterScreen;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IAdvancedRegistration;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IModIngredientRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.FluidType;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasRegistries;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.client.CCBClientRecipeUtils;
import net.ty.createcraftedbeginning.compat.jei.category.CCBRecipeCategory;
import net.ty.createcraftedbeginning.compat.jei.category.CCBRecipeCategory.Builder;
import net.ty.createcraftedbeginning.compat.jei.category.CCBRecipeCategory.Factory;
import net.ty.createcraftedbeginning.compat.jei.category.ChillingCategory;
import net.ty.createcraftedbeginning.compat.jei.category.CoolingCategory;
import net.ty.createcraftedbeginning.compat.jei.category.DissipationCategory;
import net.ty.createcraftedbeginning.compat.jei.category.EnergizationCategory;
import net.ty.createcraftedbeginning.compat.jei.category.ForgingPressCategory;
import net.ty.createcraftedbeginning.compat.jei.category.GasInjectionCategory;
import net.ty.createcraftedbeginning.compat.jei.category.PressurizationCategory;
import net.ty.createcraftedbeginning.compat.jei.category.ReactorKettleCategory;
import net.ty.createcraftedbeginning.compat.jei.category.ResidueGenerationCategory;
import net.ty.createcraftedbeginning.compat.jei.category.SequencedAssemblyWithGasCategory;
import net.ty.createcraftedbeginning.compat.jei.category.WindChargingCategory;
import net.ty.createcraftedbeginning.compat.jei.category.gas.GasStackHelper;
import net.ty.createcraftedbeginning.compat.jei.category.gas.GasStackRenderer;
import net.ty.createcraftedbeginning.compat.jei.utils.AirtightHandheldDrillGhostIngredientHandler;
import net.ty.createcraftedbeginning.compat.jei.utils.FanProcessingFilterRecipeUtils;
import net.ty.createcraftedbeginning.compat.jei.utils.GasFilterGhostIngredientHandler;
import net.ty.createcraftedbeginning.compat.jei.utils.RedstoneRequesterGhostIngredientHandler;
import net.ty.createcraftedbeginning.compat.jei.utils.StockKeeperRequestGasGuiHandler;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.AirtightHandheldDrillScreen;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasFilterScreen;
import net.ty.createcraftedbeginning.recipe.CCBRecipeTypes;
import net.ty.createcraftedbeginning.recipe.ChillingRecipe;
import net.ty.createcraftedbeginning.recipe.CoolingRecipe;
import net.ty.createcraftedbeginning.recipe.DissipationRecipe;
import net.ty.createcraftedbeginning.recipe.EnergizationRecipe;
import net.ty.createcraftedbeginning.recipe.ForgingPressRecipe;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipe;
import net.ty.createcraftedbeginning.recipe.PressurizationRecipe;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe;
import net.ty.createcraftedbeginning.recipe.ResidueGenerationRecipe;
import net.ty.createcraftedbeginning.recipe.SequencedAssemblyWithGasRecipe;
import net.ty.createcraftedbeginning.recipe.WindChargingRecipe;
import net.ty.createcraftedbeginning.recipe.WindChargingRecipe.WindChargingData;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@JeiPlugin
public class CCBJEIPlugin implements IModPlugin {
    public static final IIngredientType<GasStack> GAS_STACK = () -> GasStack.class;
    public static final GasStackHelper GAS_STACK_HELPER = new GasStackHelper();

    public static IJeiRuntime runtime;
    private final List<CCBRecipeCategory<?>> allCategories = new ArrayList<>();

    public static void consumeAllRecipes(Consumer<? super RecipeHolder<?>> consumer) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return;
        }

        connection.getRecipeManager().getRecipes().forEach(consumer);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void consumeTypedRecipes(Consumer<RecipeHolder<?>> consumer, RecipeType<?> type) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return;
        }

        List<? extends RecipeHolder<?>> recipes = connection.getRecipeManager().getAllRecipesFor((RecipeType) type);
        recipes.forEach(consumer);
    }

    private static void registerGasStackIngredients(IModIngredientRegistration registry) {
        GAS_STACK_HELPER.setColorHelper(registry.getColorHelper());
        List<GasStack> gasStacks = GasRegistries.GAS_REGISTRY.holders().filter(Objects::nonNull).filter(holder -> !holder.value().isEmpty()).map(holder -> new GasStack(holder, FluidType.BUCKET_VOLUME)).toList();
        registry.register(GAS_STACK, gasStacks, GAS_STACK_HELPER, new GasStackRenderer(), Gas.HOLDER_CODEC.xmap(holder -> new GasStack(holder, FluidType.BUCKET_VOLUME), GasStack::getGasHolder));
    }

    private static boolean isAutomatableMixingRecipe(RecipeHolder<?> holder) {
        Recipe<?> recipe = holder.value();
        return recipe instanceof ShapelessRecipe && recipe.getIngredients().size() > 1 && !MechanicalPressBlockEntity.canCompress(recipe) && !AllRecipeTypes.shouldIgnoreInAutomation(holder);
    }

    private static void addAutomaticWindChargingRecipes(List<RecipeHolder<WindChargingRecipe>> recipes) {
        List<WindChargingRecipe> overrides = recipes.stream().map(RecipeHolder::value).toList();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = item.getDefaultInstance();
            if (stack.isEmpty() || overrides.stream().anyMatch(recipe -> recipe.getIngredient().test(stack))) {
                continue;
            }

            WindChargingData data = WindChargingRecipe.getAutomaticWindChargingTime(stack);
            if (data.amount() <= 0) {
                continue;
            }

            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            ResourceLocation recipeId = CCBAPI.asResource("jei/wind_charging/" + itemId.getNamespace() + '/' + itemId.getPath());
            WindChargingRecipe recipe = new StandardProcessingRecipe.Builder<>(WindChargingRecipe::new, recipeId).withItemIngredients(Ingredient.of(item)).duration(data.time()).build();
            recipes.add(new RecipeHolder<>(recipeId, recipe));
        }
    }

    @Override
    public ResourceLocation getPluginUid() {
        return CCBAPI.asResource("jei_plugin");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(CCBItems.GAS_INJECTION_CHAMBER_FILTER.get(), FanProcessingFilterSubtypeInterpreter.INSTANCE);
    }

    @Override
    public void registerIngredients(IModIngredientRegistration registry) {
        registerGasStackIngredients(registry);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        loadCategories();
        registration.addRecipeCategories(allCategories.toArray(IRecipeCategory[]::new));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        allCategories.forEach(category -> category.registerRecipes(registration));
        FanProcessingFilterRecipeUtils.registerRecipes(registration);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        allCategories.forEach(category -> category.registerCatalysts(registration));
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(AirtightHandheldDrillScreen.class, new AirtightHandheldDrillGhostIngredientHandler());
        registration.addGhostIngredientHandler(GasFilterScreen.class, new GasFilterGhostIngredientHandler());
        registration.addGhostIngredientHandler(RedstoneRequesterScreen.class, new RedstoneRequesterGhostIngredientHandler());

        registration.addGuiContainerHandler(StockKeeperRequestScreen.class, new StockKeeperRequestGasGuiHandler());
    }

    @Override
    public void registerAdvanced(IAdvancedRegistration registration) {
        registration.addRecipeManagerPlugin(new VirtualGasItemRecipeLookupPlugin(registration.getJeiHelpers(), () -> runtime));
        registration.addRecipeManagerPlugin(new FanProcessingFilterRecipeLookupPlugin(() -> runtime));
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        CCBJEIPlugin.runtime = runtime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    @SuppressWarnings("unused")
    private void loadCategories() {
        allCategories.clear();
        builder(CoolingRecipe.class).addTypedRecipes(CCBRecipeTypes.COOLING).catalyst(CCBBlocks.BREEZE_COOLER_BLOCK::get).itemIcon(CCBBlocks.BREEZE_COOLER_BLOCK).emptyBackground(177, 50).build("cooling", CoolingCategory::new);
        builder(DissipationRecipe.class).addTypedRecipes(CCBRecipeTypes.DISSIPATION).catalyst(CCBBlocks.BREEZE_CHAMBER_BLOCK::get).catalyst(CCBBlocks.AIRTIGHT_TANK_BLOCK::get).doubleItemIcon(CCBBlocks.BREEZE_CHAMBER_BLOCK, CCBBlocks.AIRTIGHT_TANK_BLOCK).emptyBackground(177, 70).build("dissipation", DissipationCategory::new);
        builder(EnergizationRecipe.class).addTypedRecipes(CCBRecipeTypes.ENERGIZATION).catalyst(CCBBlocks.BREEZE_CHAMBER_BLOCK::get).catalyst(CCBBlocks.AIRTIGHT_TANK_BLOCK::get).doubleItemIcon(CCBBlocks.BREEZE_CHAMBER_BLOCK, CCBBlocks.AIRTIGHT_TANK_BLOCK).emptyBackground(177, 70).build("energization", EnergizationCategory::new);
        builder(ForgingPressRecipe.class).addTypedRecipes(CCBRecipeTypes.FORGING_PRESS).catalyst(CCBBlocks.AIRTIGHT_FORGING_PRESS_BLOCK::get).emptyBackground(177, 103).build("forging_press", ForgingPressCategory::new);
        builder(ForgingPressRecipe.class).enableWhen(CCBConfig.server().airtights.enableAutomaticPressingRecipes).addAllRecipesIf(holder -> holder.value() instanceof PressingRecipe, ForgingPressRecipe::convertPressingToForgingPressRecipe).catalyst(CCBBlocks.AIRTIGHT_FORGING_PRESS_BLOCK::get).doubleItemIcon(CCBBlocks.AIRTIGHT_FORGING_PRESS_BLOCK, AllBlocks.MECHANICAL_PRESS).emptyBackground(177, 103).build("forging_press_auto_pressing", ForgingPressCategory::new);
        builder(ForgingPressRecipe.class).enableWhen(CCBConfig.server().airtights.enableAutomaticSmithingRecipes).addAllRecipesIf(holder -> holder.value() instanceof SmithingRecipe, ForgingPressRecipe::convertToForgingPressRecipe).catalyst(CCBBlocks.AIRTIGHT_FORGING_PRESS_BLOCK::get).doubleItemIcon(CCBBlocks.AIRTIGHT_FORGING_PRESS_BLOCK, Blocks.SMITHING_TABLE).emptyBackground(177, 103).build("forging_press_auto_smithing", ForgingPressCategory::new);
        builder(ChillingRecipe.class).addTypedRecipes(CCBRecipeTypes.CHILLING).catalystStack(ChillingCategory.getCatalystStack()).doubleItemIcon(AllItems.PROPELLER.get(), CCBBlocks.BREEZE_COOLER_BLOCK).emptyBackground(178, 72).build("chilling", ChillingCategory::new);
        builder(GasInjectionRecipe.class).addTypedRecipes(CCBRecipeTypes.GAS_INJECTION).catalyst(CCBBlocks.GAS_INJECTION_CHAMBER_BLOCK::get).doubleItemIcon(CCBBlocks.GAS_INJECTION_CHAMBER_BLOCK, CCBItems.GAS_CANISTER).emptyBackground(177, 70).build("gas_injection", GasInjectionCategory::new);
        builder(PressurizationRecipe.class).addTypedRecipes(CCBRecipeTypes.PRESSURIZATION).catalyst(CCBBlocks.AIR_COMPRESSOR_BLOCK::get).catalyst(CCBBlocks.BREEZE_COOLER_BLOCK::get).doubleItemIcon(CCBBlocks.AIR_COMPRESSOR_BLOCK, CCBBlocks.BREEZE_COOLER_BLOCK).emptyBackground(177, 70).build("pressurization", PressurizationCategory::new);
        builder(ReactorKettleRecipe.class).addTypedRecipes(CCBRecipeTypes.REACTOR_KETTLE).catalyst(CCBBlocks.AIRTIGHT_REACTOR_KETTLE_BLOCK::get).emptyBackground(177, 103).build("reactor_kettle", ReactorKettleCategory::new);
        builder(ReactorKettleRecipe.class).enableWhen(CCBConfig.server().airtights.enableAutomaticMixingRecipes).addAllRecipesIf(CCBJEIPlugin::isAutomatableMixingRecipe, CCBClientRecipeUtils::convertToReactorKettleRecipe).catalyst(CCBBlocks.AIRTIGHT_REACTOR_KETTLE_BLOCK::get).doubleItemIcon(CCBBlocks.AIRTIGHT_REACTOR_KETTLE_BLOCK, Blocks.CRAFTING_TABLE).emptyBackground(177, 103).build("reactor_kettle_auto_mixing", ReactorKettleCategory::new);
        builder(ResidueGenerationRecipe.class).addTypedRecipes(CCBRecipeTypes.RESIDUE_GENERATION).catalyst(CCBBlocks.RESIDUE_OUTLET_BLOCK::get).catalyst(CCBBlocks.AIRTIGHT_ENGINE_BLOCK::get).emptyBackground(177, 103).build("residue_generation", ResidueGenerationCategory::new);
        builder(SequencedAssemblyWithGasRecipe.class).addTypedRecipes(CCBRecipeTypes.SEQUENCED_ASSEMBLY_WITH_GAS).doubleItemIcon(AllItems.PRECISION_MECHANISM.get(), CCBItems.GAS_CANISTER).emptyBackground(180, 115).build("sequenced_assembly_with_gas", SequencedAssemblyWithGasCategory::new);
        builder(WindChargingRecipe.class).addTypedRecipes(CCBRecipeTypes.WIND_CHARGING).addRecipeListConsumer(CCBJEIPlugin::addAutomaticWindChargingRecipes).catalyst(CCBBlocks.BREEZE_CHAMBER_BLOCK::get).itemIcon(CCBBlocks.BREEZE_CHAMBER_BLOCK).emptyBackground(177, 50).build("wind_charging", WindChargingCategory::new);
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
        public CCBRecipeCategory<T> build(ResourceLocation id, Factory<T> factory) {
            CCBRecipeCategory<T> category = super.build(id, factory);
            allCategories.add(category);
            return category;
        }
    }
}
