package net.ty.createcraftedbeginning.recipe.gas;

import net.createmod.catnip.theme.Color;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SequencedAssemblyWithGasItem extends Item {
    /**
     * Creates a new {@code SequencedAssemblyWithGasItem} instance.
     *
     * @param properties the properties to use
     */
    public SequencedAssemblyWithGasItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(getProgress(stack) * 13);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBarColor(ItemStack stack) {
        return Color.mixColors(0xFFFFC074, 0xFF46FFE0, getProgress(stack));
    }

    /**
     * Returns the progress.
     *
     * @param stack the stack to inspect or process
     * @return the progress
     */
    @SuppressWarnings("DataFlowIssue")
    public float getProgress(ItemStack stack) {
        if (!stack.has(CCBDataComponents.SEQUENCED_ASSEMBLY_WITH_GAS)) {
            return 0;
        }
        return stack.get(CCBDataComponents.SEQUENCED_ASSEMBLY_WITH_GAS).progress();
    }
}
