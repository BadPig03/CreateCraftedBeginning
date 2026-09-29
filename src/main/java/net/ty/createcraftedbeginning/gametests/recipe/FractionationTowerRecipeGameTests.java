package net.ty.createcraftedbeginning.gametests.recipe;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtightfractionationtower.AirtightFractionationTowerRecipeLookup;
import net.ty.createcraftedbeginning.recipe.CCBRecipeTypes;
import net.ty.createcraftedbeginning.recipe.FractionationTowerOutput;
import net.ty.createcraftedbeginning.recipe.FractionationTowerRecipe;
import net.ty.createcraftedbeginning.recipe.FractionationTowerRecipe.Builder;
import net.ty.createcraftedbeginning.recipe.FractionationTowerRecipe.Serializer;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureCondition;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureMatching;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FractionationTowerRecipeGameTests {
    private FractionationTowerRecipeGameTests() {
    }

    @SuppressWarnings("deprecation")
    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void generatedRecipesRoundTripThroughDataAndNetwork(GameTestHelper helper) {
        RegistryAccess registries = helper.getLevel().registryAccess();
        RegistryOps<JsonElement> ops = registries.createSerializationContext(JsonOps.INSTANCE);
        Serializer serializer = CCBRecipeTypes.FRACTIONATION_TOWER.getSerializer();
        List<RecipeHolder<FractionationTowerRecipe>> recipes = helper.getLevel().getRecipeManager().getAllRecipesFor(CCBRecipeTypes.FRACTIONATION_TOWER.getType());
        helper.assertValueEqual(recipes.size(), 2, "production tower recipe count without JEI fixtures");
        helper.assertTrue(recipes.stream().anyMatch(holder -> holder.id().equals(CCBAPI.asResource("fractionation_tower/brimstone_fractionation"))), "Generated recipes must include brimstone fractionation.");
        helper.assertTrue(recipes.stream().anyMatch(holder -> holder.id().equals(CCBAPI.asResource("fractionation_tower/moist_air_condensation"))), "Generated recipes must include moist air condensation.");
        for (RecipeHolder<FractionationTowerRecipe> holder : recipes) {
            FractionationTowerRecipe original = holder.value();
            JsonElement encoded = serializer.codec().codec().encodeStart(ops, original).getOrThrow(error -> new IllegalStateException("Failed to encode tower recipe '" + holder.id() + "': " + error));
            FractionationTowerRecipe decoded = serializer.codec().codec().parse(ops, encoded).getOrThrow(error -> new IllegalStateException("Failed to decode tower recipe '" + holder.id() + "': " + error));
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
            try {
                serializer.streamCodec().encode(buffer, decoded);
                FractionationTowerRecipe received = serializer.streamCodec().decode(buffer);
                JsonElement receivedJson = serializer.codec().codec().encodeStart(ops, received).getOrThrow(error -> new IllegalStateException("Failed to encode received tower recipe '" + holder.id() + "': " + error));
                helper.assertTrue(encoded.equals(receivedJson), "Data and network codecs must preserve inputs, temperatures, duration and layer outputs.");
                helper.assertTrue(received.getRequiredHeight() == original.getRequiredHeight() && received.getLayerOutputs().size() == original.getLayerOutputs().size(), "Data and network codecs must preserve the required height and product count of every generated recipe.");
            }
            finally {
                buffer.release();
            }
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void recipeCacheReusesValidatedRecipesAndRefreshesOnReload(GameTestHelper helper) {
        FractionationTowerRecipe original = new Builder(CCBAPI.asResource("test/tower_cache")).require(Items.IRON_INGOT).temperatureCondition(TemperatureCondition.HEATED).duration(20).outputAtLayer(1, new ItemStack(Items.GOLD_INGOT)).outputAtLayer(2, new GasStack(CCBGases.STEAM.get(), 100)).build();
        FractionationTowerRecipe replacement = new Builder(CCBAPI.asResource("test/tower_cache")).require(Items.IRON_INGOT).temperatureCondition(TemperatureCondition.HEATED).duration(40).outputAtLayer(1, new ItemStack(Items.COPPER_INGOT)).outputAtLayer(2, new GasStack(CCBGases.STEAM.get(), 200)).build();
        FractionationTowerRecipe invalid = new Builder(CCBAPI.asResource("test/tower_cache_invalid")).require(Items.IRON_INGOT).temperatureCondition(TemperatureCondition.HEATED).duration(20).outputAtLayer(1, new ItemStack(Items.GOLD_INGOT)).outputAtLayer(1, new GasStack(CCBGases.STEAM.get(), 100)).build();
        try (RecipeIndexTestScope scope = new RecipeIndexTestScope(helper.getLevel(), List.of(original, invalid))) {
            List<RecipeHolder<FractionationTowerRecipe>> cached = AirtightFractionationTowerRecipeLookup.getRecipes(helper.getLevel().getRecipeManager());
            helper.assertTrue(cached.size() == 1 && cached.getFirst().value() == original, "Recipe caches must exclude invalid layer mappings.");
            helper.assertTrue(AirtightFractionationTowerRecipeLookup.getRecipes(helper.getLevel().getRecipeManager()) == cached, "Repeated queries must reuse the validated recipe list.");
            scope.reload(List.of(replacement));
            List<RecipeHolder<FractionationTowerRecipe>> reloaded = AirtightFractionationTowerRecipeLookup.getRecipes(helper.getLevel().getRecipeManager());
            helper.assertTrue(reloaded.size() == 1 && reloaded.getFirst().value() == replacement && reloaded.getFirst().id().equals(cached.getFirst().id()), "Reloading must replace cached recipes even when their IDs are unchanged.");
            scope.reload(List.of());
            helper.assertTrue(AirtightFractionationTowerRecipeLookup.getRecipes(helper.getLevel().getRecipeManager()).isEmpty(), "Removed recipes must not remain in the cache.");
            scope.reload(List.of(original));
            helper.assertTrue(AirtightFractionationTowerRecipeLookup.getRecipes(helper.getLevel().getRecipeManager()).getFirst().value() == original, "An empty cache must discover recipes added by a later reload.");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void invalidLayerMappingsAndTemperaturesAreRejected(GameTestHelper helper) {
        FractionationTowerRecipe duplicateLayer = new Builder(CCBAPI.asResource("test/duplicate_layer")).require(Items.IRON_INGOT).temperatureCondition(TemperatureCondition.HEATED).duration(20).outputAtLayer(1, new ItemStack(Items.GOLD_INGOT)).outputAtLayer(1, new FluidStack(Fluids.WATER, 100)).build();
        FractionationTowerRecipe sameProduct = new Builder(CCBAPI.asResource("test/same_product")).require(Items.IRON_INGOT).temperatureCondition(TemperatureCondition.HEATED).duration(20).outputAtLayer(1, new ItemStack(Items.GOLD_INGOT)).outputAtLayer(2, new ItemStack(Items.GOLD_INGOT, 2)).build();
        FractionationTowerRecipe invalidTemperature = new Builder(CCBAPI.asResource("test/invalid_temperature")).require(Items.IRON_INGOT).temperatureCondition(TemperatureCondition.SUPERHEATED).temperatureMatching(TemperatureMatching.COMPATIBLE).duration(20).outputAtLayer(1, new ItemStack(Items.GOLD_INGOT)).outputAtLayer(2, new GasStack(CCBGases.STEAM.get(), 100)).build();
        FractionationTowerRecipe invalidOffset = new Builder(CCBAPI.asResource("test/invalid_offset")).require(Items.IRON_INGOT).temperatureCondition(TemperatureCondition.HEATED).duration(20).outputAtLayer(0, new ItemStack(Items.GOLD_INGOT)).outputAtLayer(9, new GasStack(CCBGases.STEAM.get(), 100)).build();
        Serializer serializer = CCBRecipeTypes.FRACTIONATION_TOWER.getSerializer();
        RegistryOps<JsonElement> ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        for (FractionationTowerRecipe recipe : List.of(duplicateLayer, sameProduct, invalidTemperature, invalidOffset)) {
            helper.assertTrue(!recipe.validate().isEmpty(), "Invalid temperature or layer mapping must fail validation.");
            helper.assertTrue(serializer.codec().codec().encodeStart(ops, recipe).error().isPresent(), "Invalid recipes must not pass the data codec.");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 30)
    public static void layerOutputsOwnTheirStacks(GameTestHelper helper) {
        ItemStack source = new ItemStack(Items.GOLD_INGOT, 2);
        FractionationTowerOutput output = new FractionationTowerOutput(1, source, FluidStack.EMPTY, GasStack.EMPTY);
        source.setCount(40);
        output.item().setCount(50);
        helper.assertValueEqual(output.item().getCount(), 2, "output count after mutating external stacks");
        helper.assertTrue(!new FractionationTowerOutput(1, source, new FluidStack(Fluids.WATER, 100), GasStack.EMPTY).isValid(), "One layer output must describe exactly one product.");
        helper.succeed();
    }
}
