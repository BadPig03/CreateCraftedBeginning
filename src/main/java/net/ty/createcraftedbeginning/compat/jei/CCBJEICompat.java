package net.ty.createcraftedbeginning.compat.jei;

import com.simibubi.create.compat.jei.ConversionRecipe;
import com.simibubi.create.compat.jei.category.MysteriousItemConversionCategory;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBJEICompat {
    private CCBJEICompat() {
    }

    public static void registerMysteriousItemConversions() {
        MysteriousItemConversionCategory.RECIPES.add(ConversionRecipe.create(new ItemStack(CCBBlocks.EMPTY_BREEZE_COOLER_BLOCK), new ItemStack(CCBBlocks.BREEZE_COOLER_BLOCK)));
    }
}
