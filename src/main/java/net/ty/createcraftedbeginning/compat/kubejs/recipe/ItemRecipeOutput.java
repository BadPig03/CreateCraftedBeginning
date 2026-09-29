package net.ty.createcraftedbeginning.compat.kubejs.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record ItemRecipeOutput(ProcessingOutput output) {
    public static ItemRecipeOutput of(ItemStack stack) {
        return of(stack, stack.getCount());
    }

    public static ItemRecipeOutput of(ItemStack stack, int count) {
        if (stack.isEmpty() || count <= 0) {
            throw new IllegalArgumentException("Item output must not be empty and its count must be positive; got item '" + stack.getItem() + "', source count=" + stack.getCount() + ", output count=" + count + '.');
        }

        return new ItemRecipeOutput(new ProcessingOutput(stack.copyWithCount(count), 1));
    }

    @SuppressWarnings("unused")
    public ItemRecipeOutput withChance(float chance) {
        if (!Float.isFinite(chance) || chance <= 0 || chance > 1) {
            throw new IllegalArgumentException("Output chance must be finite and in (0, 1]; got " + chance + '.');
        }

        return new ItemRecipeOutput(new ProcessingOutput(output.getStack(), chance));
    }
}
