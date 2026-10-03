package net.ty.createcraftedbeginning.compat.jei.recipe;

import com.simibubi.create.content.fluids.potion.PotionFluid;
import com.simibubi.create.content.fluids.potion.PotionFluid.BottleType;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.crafting.CompoundFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.DataComponentFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.airtights.potiongas.PotionGas;
import net.ty.createcraftedbeginning.recipe.FractionationTowerOutput;
import net.ty.createcraftedbeginning.recipe.FractionationTowerRecipe;
import net.ty.createcraftedbeginning.recipe.FractionationTowerRecipe.Builder;
import net.ty.createcraftedbeginning.recipe.PotionFractionationRecipes;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class PotionFractionationDisplayRecipes {
    private PotionFractionationDisplayRecipes() {
    }

    public static List<RecipeHolder<FractionationTowerRecipe>> createDisplayRecipes(Level level) {
        int batchAmount = PotionFractionationRecipes.BATCH_AMOUNT;
        List<RecipeHolder<FractionationTowerRecipe>> recipes = new ArrayList<>();
        for (Reference<Potion> potion : level.registryAccess().lookupOrThrow(Registries.POTION).listElements().toList()) {
            PotionContents contents = new PotionContents(potion);
            if (PotionGas.findReleaseEffect(contents) == null) {
                continue;
            }

            FractionationTowerRecipe recipe = PotionFractionationRecipes.create(PotionFluid.of(batchAmount, contents, BottleType.REGULAR));
            if (recipe == null) {
                continue;
            }

            List<FluidIngredient> alternatives = new ArrayList<>();
            for (BottleType bottleType : BottleType.values()) {
                alternatives.add(DataComponentFluidIngredient.of(true, PotionFluid.of(batchAmount, contents, bottleType)));
            }

            ResourceLocation potionId = potion.key().location();
            ResourceLocation id = CCBAPI.asResource("potion_fractionation/" + potionId.getNamespace() + '/' + potionId.getPath());
            List<FractionationTowerOutput> outputs = recipe.getLayerOutputs();
            FractionationTowerRecipe displayRecipe = new Builder(id).require(new SizedFluidIngredient(CompoundFluidIngredient.of(alternatives), batchAmount)).temperatureCondition(recipe.getTemperatureCondition()).duration(recipe.getProcessingDuration()).outputAtLayer(1, outputs.getFirst().fluid()).outputAtLayer(2, outputs.get(1).gas()).build();
            recipes.add(new RecipeHolder<>(id, displayRecipe));
        }
        return List.copyOf(recipes);
    }
}
