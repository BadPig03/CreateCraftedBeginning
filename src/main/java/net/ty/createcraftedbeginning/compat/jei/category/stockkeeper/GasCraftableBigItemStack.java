package net.ty.createcraftedbeginning.compat.jei.category.stockkeeper;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.stockTicker.CraftableBigItemStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasCraftableBigItemStack extends CraftableBigItemStack {
    private final int outputPerCraft;
    private final int transferLimit;
    private final List<BigItemStack> requirements;

    public GasCraftableBigItemStack(ItemStack displayStack, Recipe<?> recipe, int outputPerCraft, int transferLimit, List<BigItemStack> requirements) {
        super(displayStack.copyWithCount(1), recipe);
        count = 0;
        this.outputPerCraft = Math.max(1, outputPerCraft);
        this.transferLimit = Math.max(this.outputPerCraft, transferLimit);
        this.requirements = requirements.stream().map(requirement -> new BigItemStack(requirement.stack.copyWithCount(1), requirement.count)).toList();
    }

    @Override
    public List<Ingredient> getIngredients() {
        return requirements.stream().map(requirement -> Ingredient.of(requirement.stack.copyWithCount(Math.max(1, requirement.count)))).toList();
    }

    @Override
    public int getOutputCount(Level level) {
        return outputPerCraft;
    }

    public int getOutputPerCraft() {
        return outputPerCraft;
    }

    public int getTransferLimit() {
        return transferLimit;
    }

    public List<BigItemStack> getRequirements() {
        return requirements;
    }

    public boolean matches(Recipe<?> recipe, ItemStack displayStack) {
        return this.recipe == recipe && ItemStack.isSameItemSameComponents(stack, displayStack);
    }
}
