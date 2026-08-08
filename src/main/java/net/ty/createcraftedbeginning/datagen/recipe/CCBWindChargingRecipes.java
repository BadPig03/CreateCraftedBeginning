package net.ty.createcraftedbeginning.datagen.recipe;

import com.simibubi.create.AllItems;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.datagen.recipe.generator.WindChargingRecipeGen;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;

import static net.ty.createcraftedbeginning.recipe.WindChargingRecipe.WindChargingAction.CHARGE;
import static net.ty.createcraftedbeginning.recipe.WindChargingRecipe.WindChargingAction.CLEAR_ILL;
import static net.ty.createcraftedbeginning.recipe.WindChargingRecipe.WindChargingAction.CYCLE_CREATIVE;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class CCBWindChargingRecipes extends WindChargingRecipeGen {
    GeneratedRecipe CAKE = create("cake", CHARGE, b -> b.require(Items.CAKE).duration(2434));
    GeneratedRecipe BUILDERS_TEA = create("builders_tea", CHARGE, b -> b.require(AllItems.BUILDERS_TEA).duration(288).output(Items.GLASS_BOTTLE));
    GeneratedRecipe MILK_BUCKET = create("milk_bucket", CLEAR_ILL, b -> b.require(Items.MILK_BUCKET));
    GeneratedRecipe MILK_ICE_CREAM = create("milk_ice_cream", CLEAR_ILL, b -> b.require(CCBItems.MILK_ICE_CREAM));
    GeneratedRecipe CREATIVE_ICE_CREAM = create("creative_ice_cream", CYCLE_CREATIVE, b -> b.require(CCBItems.CREATIVE_ICE_CREAM));

    public CCBWindChargingRecipes(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries, CCBAPI.MOD_ID);
    }
}
