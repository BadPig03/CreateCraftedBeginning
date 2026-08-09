package net.ty.createcraftedbeginning.recipe.gas;

import net.createmod.catnip.theme.Color;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.recipe.CCBRecipeDataComponents;

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

    @SuppressWarnings("DataFlowIssue")
    public float getProgress(ItemStack stack) {
        if (!stack.has(CCBRecipeDataComponents.SEQUENCED_ASSEMBLY_WITH_GAS)) {
            return 0;
        }
        return stack.get(CCBRecipeDataComponents.SEQUENCED_ASSEMBLY_WITH_GAS).progress();
    }
}
