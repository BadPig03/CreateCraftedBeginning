package net.ty.createcraftedbeginning.gametests.compat.jei;

import com.simibubi.create.content.fluids.potion.PotionFluid;
import com.simibubi.create.content.fluids.potion.PotionFluid.BottleType;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.compat.jei.recipe.PotionFractionationDisplayRecipes;
import net.ty.createcraftedbeginning.content.airtights.potiongas.PotionGas;
import net.ty.createcraftedbeginning.recipe.FractionationTowerRecipe;
import net.ty.createcraftedbeginning.recipe.PotionFractionationRecipes;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@PrefixGameTestTemplate(false)
public final class PotionFractionationDisplayGameTests {
    private PotionFractionationDisplayGameTests() {
    }

    @GameTest(templateNamespace = CCBAPI.MOD_ID, template = "gametest/empty_3x3")
    public static void compoundRecipesStayHiddenAndSingleEffectBottlesStayMerged(GameTestHelper helper) {
        helper.assertTrue(PotionFractionationRecipes.create(PotionFluid.of(250, new PotionContents(Potions.TURTLE_MASTER), BottleType.REGULAR)) != null, "Turtle master must be processable before checking its JEI exclusion.");
        List<RecipeHolder<FractionationTowerRecipe>> recipes = PotionFractionationDisplayRecipes.createDisplayRecipes(helper.getLevel());
        helper.assertTrue(recipes.stream().noneMatch(holder -> holder.id().getPath().contains("turtle_master")), "Every registered turtle master variant must stay out of JEI potion fractionation.");
        helper.assertTrue(recipes.stream().filter(holder -> holder.id().equals(CCBAPI.asResource("potion_fractionation/minecraft/swiftness"))).count() == 1 && recipes.stream().filter(holder -> holder.id().equals(CCBAPI.asResource("potion_fractionation/minecraft/healing"))).count() == 1, "Single sustained and instant potions must each retain one merged display recipe.");
        FractionationTowerRecipe speed = null;
        for (RecipeHolder<FractionationTowerRecipe> holder : recipes) {
            FractionationTowerRecipe recipe = holder.value();
            helper.assertTrue(recipe.getLayerOutputs().size() == 2 && PotionGas.findReleaseEffect(recipe.getLayerOutputs().get(1).gas()) != null, "JEI must only contain water plus one supported gas product.");
            if (!holder.id().equals(CCBAPI.asResource("potion_fractionation/minecraft/swiftness"))) {
                continue;
            }

            speed = recipe;
        }
        if (speed == null) {
            throw new NullPointerException("Expected the merged swiftness JEI recipe.");
        }

        SizedFluidIngredient input = speed.getFluidIngredients().getFirst();
        helper.assertTrue(input.amount() == 250 && speed.getLayerOutputs().get(1).gas().getAmount() == 7200, "Single-effect JEI quantities must remain unchanged.");
        for (BottleType bottleType : BottleType.values()) {
            helper.assertTrue(input.ingredient().test(PotionFluid.of(250, new PotionContents(Potions.SWIFTNESS), bottleType)), "The merged JEI input must retain every bottle variant.");
        }
        helper.assertTrue(!input.ingredient().test(PotionFluid.of(250, new PotionContents(Potions.TURTLE_MASTER), BottleType.REGULAR)), "A compound potion must not enter JEI through single-effect ingredient alternatives.");
        helper.succeed();
    }
}
