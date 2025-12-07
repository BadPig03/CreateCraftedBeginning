package net.ty.createcraftedbeginning.api.gas.recipes;

import net.createmod.catnip.theme.Color;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.NotNull;

public class SequencedAssemblyWithGasItem extends Item {
    public SequencedAssemblyWithGasItem(@NotNull Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        return Math.round(getProgress(stack) * 13);
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        return Color.mixColors(0xFFFFC074, 0xFF46FFE0, getProgress(stack));
    }

    @SuppressWarnings("DataFlowIssue")
    public float getProgress(@NotNull ItemStack stack) {
        return stack.has(CCBDataComponents.SEQUENCED_ASSEMBLY_WITH_GAS) ? stack.get(CCBDataComponents.SEQUENCED_ASSEMBLY_WITH_GAS).progress() : 0;
    }
}
