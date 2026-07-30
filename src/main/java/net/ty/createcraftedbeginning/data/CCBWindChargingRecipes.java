package net.ty.createcraftedbeginning.data;

import com.simibubi.create.AllItems;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.recipe.generators.WindChargingRecipeGen;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class CCBWindChargingRecipes extends WindChargingRecipeGen {
    GeneratedRecipe CAKE = create("cake", b -> b.require(Items.CAKE).duration(3104));
    GeneratedRecipe BUILDERS_TEA = create("builders_tea", b -> b.require(AllItems.BUILDERS_TEA).duration(445).output(Items.GLASS_BOTTLE));
    GeneratedRecipe MILK_BUCKET = create("milk_bucket", b -> b.require(Items.MILK_BUCKET).duration(0));
    GeneratedRecipe MILK_ICE_CREAM = create("milk_ice_cream", b -> b.require(CCBItems.MILK_ICE_CREAM).duration(0));
    GeneratedRecipe CREATIVE_ICE_CREAM = create("creative_ice_cream", b -> b.require(CCBItems.CREATIVE_ICE_CREAM).duration(36060));

    public CCBWindChargingRecipes(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries, CreateCraftedBeginning.MOD_ID);
    }
}
