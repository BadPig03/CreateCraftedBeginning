package net.ty.createcraftedbeginning.recipe.gas;

import net.createmod.catnip.theme.Color;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.recipe.CCBRecipeDataComponents;
import net.ty.createcraftedbeginning.recipe.SequencedAssemblyWithGasRecipe.SequencedAssemblyWithGas;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SequencedAssemblyWithGasItem extends Item {
    public SequencedAssemblyWithGasItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(getProgress(stack) * 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Color.mixColors(0xFFFFC074, 0xFF46FFE0, getProgress(stack));
    }

    public float getProgress(ItemStack stack) {
        SequencedAssemblyWithGas data = stack.get(CCBRecipeDataComponents.SEQUENCED_ASSEMBLY_WITH_GAS);
        return data == null ? 0 : data.progress();
    }
}
