package net.ty.createcraftedbeginning.compat.jei;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterScreen;
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
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.FluidType;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
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
import net.ty.createcraftedbeginning.compat.jei.utils.GasFilterGhostIngredientHandler;
import net.ty.createcraftedbeginning.compat.jei.utils.RedstoneRequesterGhostIngredientHandler;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.AirtightHandheldDrillScreen;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasFilterScreen;
import net.ty.createcraftedbeginning.data.CCBGasRegistries;
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
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
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

        List<? extends RecipeHolder<?>> map = connection.getRecipeManager().getAllRecipesFor((RecipeType) type);
        if (map.isEmpty()) {
            return;
        }

        map.forEach(consumer);
    }

    private static void registerGasStackIngredients(IModIngredientRegistration registry) {
        GAS_STACK_HELPER.setColorHelper(registry.getColorHelper());
        List<GasStack> types = CCBGasRegistries.GAS_REGISTRY.holders().filter(Objects::nonNull).filter(gas -> !gas.value().isEmpty()).map(gas -> new GasStack(gas, FluidType.BUCKET_VOLUME)).toList();
        registry.register(GAS_STACK, types, GAS_STACK_HELPER, new GasStackRenderer(), Gas.HOLDER_CODEC.xmap(gas -> new GasStack(gas, FluidType.BUCKET_VOLUME), GasStack::getGasHolder));
    }

    @Override
    public ResourceLocation getPluginUid() {
        return CreateCraftedBeginning.asResource("jei_plugin");
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
        allCategories.forEach(c -> c.registerRecipes(registration));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        allCategories.forEach(c -> c.registerCatalysts(registration));
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(AirtightHandheldDrillScreen.class, new AirtightHandheldDrillGhostIngredientHandler());
        registration.addGhostIngredientHandler(GasFilterScreen.class, new GasFilterGhostIngredientHandler());
        registration.addGhostIngredientHandler(RedstoneRequesterScreen.class, new RedstoneRequesterGhostIngredientHandler());
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
        CCBRecipeCategory<?> cooling = builder(CoolingRecipe.class).addTypedRecipes(CCBRecipeTypes.COOLING).catalyst(CCBBlocks.BREEZE_COOLER_BLOCK::get).itemIcon(CCBBlocks.BREEZE_COOLER_BLOCK).emptyBackground(177, 50).build("cooling", CoolingCategory::new);
        CCBRecipeCategory<?> dissipation = builder(DissipationRecipe.class).addTypedRecipes(CCBRecipeTypes.DISSIPATION).catalyst(CCBBlocks.BREEZE_CHAMBER_BLOCK::get).catalyst(CCBBlocks.AIRTIGHT_TANK_BLOCK::get).doubleItemIcon(CCBBlocks.BREEZE_CHAMBER_BLOCK, CCBBlocks.AIRTIGHT_TANK_BLOCK).emptyBackground(177, 70).build("dissipation", DissipationCategory::new);
        CCBRecipeCategory<?> energization = builder(EnergizationRecipe.class).addTypedRecipes(CCBRecipeTypes.ENERGIZATION).catalyst(CCBBlocks.BREEZE_CHAMBER_BLOCK::get).catalyst(CCBBlocks.AIRTIGHT_TANK_BLOCK::get).doubleItemIcon(CCBBlocks.BREEZE_CHAMBER_BLOCK, CCBBlocks.AIRTIGHT_TANK_BLOCK).emptyBackground(177, 70).build("energization", EnergizationCategory::new);
        CCBRecipeCategory<?> forgingPress = builder(ForgingPressRecipe.class).addTypedRecipes(CCBRecipeTypes.FORGING_PRESS).catalyst(CCBBlocks.AIRTIGHT_FORGING_PRESS_BLOCK::get).emptyBackground(177, 103).build("forging_press", ForgingPressCategory::new);
        CCBRecipeCategory<?> forgingPressAutoSmithing = builder(ForgingPressRecipe.class).addAllRecipesIf(r -> r.value() instanceof SmithingRecipe, ForgingPressRecipe::convertToForgingPressRecipe).catalyst(CCBBlocks.AIRTIGHT_FORGING_PRESS_BLOCK::get).doubleItemIcon(CCBBlocks.AIRTIGHT_FORGING_PRESS_BLOCK, Blocks.SMITHING_TABLE).emptyBackground(177, 103).build("forging_press_auto_smithing", ForgingPressCategory::new);
        CCBRecipeCategory<?> freezing = builder(ChillingRecipe.class).addTypedRecipes(CCBRecipeTypes.CHILLING).catalystStack(ChillingCategory.getCatalystStack()).doubleItemIcon(AllItems.PROPELLER.get(), CCBBlocks.BREEZE_COOLER_BLOCK).emptyBackground(178, 72).build("chilling", ChillingCategory::new);
        CCBRecipeCategory<?> gasInjection = builder(GasInjectionRecipe.class).addTypedRecipes(CCBRecipeTypes.GAS_INJECTION).catalyst(CCBBlocks.GAS_INJECTION_CHAMBER_BLOCK::get).doubleItemIcon(CCBBlocks.GAS_INJECTION_CHAMBER_BLOCK, CCBItems.GAS_CANISTER).emptyBackground(177, 70).build("gas_injection", GasInjectionCategory::new);
        CCBRecipeCategory<?> pressurization = builder(PressurizationRecipe.class).addTypedRecipes(CCBRecipeTypes.PRESSURIZATION).catalyst(CCBBlocks.AIR_COMPRESSOR_BLOCK::get).catalyst(CCBBlocks.BREEZE_COOLER_BLOCK::get).doubleItemIcon(CCBBlocks.AIR_COMPRESSOR_BLOCK, CCBBlocks.BREEZE_COOLER_BLOCK).emptyBackground(177, 70).build("pressurization", PressurizationCategory::new);
        CCBRecipeCategory<?> reactorKettle = builder(ReactorKettleRecipe.class).addTypedRecipes(CCBRecipeTypes.REACTOR_KETTLE).catalyst(CCBBlocks.AIRTIGHT_REACTOR_KETTLE_BLOCK::get).emptyBackground(177, 103).build("reactor_kettle", ReactorKettleCategory::new);
        CCBRecipeCategory<?> reactorKettleAutoMixing = builder(ReactorKettleRecipe.class).addAllRecipesIf(r -> r.value() instanceof CraftingRecipe && r.value() instanceof ShapelessRecipe && r.value().getIngredients().size() > 1 && !MechanicalPressBlockEntity.canCompress(r.value()) && !AllRecipeTypes.shouldIgnoreInAutomation(r), ReactorKettleRecipe::convertToReactorKettleRecipe).catalyst(CCBBlocks.AIRTIGHT_REACTOR_KETTLE_BLOCK::get).doubleItemIcon(CCBBlocks.AIRTIGHT_REACTOR_KETTLE_BLOCK, Blocks.CRAFTING_TABLE).emptyBackground(177, 103).build("reactor_kettle_auto_mixing", ReactorKettleCategory::new);
        CCBRecipeCategory<?> residueGeneration = builder(ResidueGenerationRecipe.class).addTypedRecipes(CCBRecipeTypes.RESIDUE_GENERATION).catalyst(CCBBlocks.RESIDUE_OUTLET_BLOCK::get).catalyst(CCBBlocks.AIRTIGHT_ENGINE_BLOCK::get).emptyBackground(177, 103).build("residue_generation", ResidueGenerationCategory::new);
        CCBRecipeCategory<?> sequencedAssemblyWithGas = builder(SequencedAssemblyWithGasRecipe.class).addTypedRecipes(CCBRecipeTypes.SEQUENCED_ASSEMBLY_WITH_GAS).doubleItemIcon(AllItems.PRECISION_MECHANISM.get(), CCBItems.GAS_CANISTER).emptyBackground(180, 115).build("sequenced_assembly_with_gas", SequencedAssemblyWithGasCategory::new);
        CCBRecipeCategory<?> windCharging = builder(WindChargingRecipe.class).addTypedRecipes(CCBRecipeTypes.WIND_CHARGING).catalyst(CCBBlocks.BREEZE_CHAMBER_BLOCK::get).itemIcon(CCBBlocks.BREEZE_CHAMBER_BLOCK).emptyBackground(177, 50).build("wind_charging", WindChargingCategory::new);
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
