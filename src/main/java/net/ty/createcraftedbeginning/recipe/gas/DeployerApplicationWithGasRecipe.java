package net.ty.createcraftedbeginning.recipe.gas;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.gas.recipes.ItemApplicationWithGasRecipeParams;
import net.ty.createcraftedbeginning.recipe.CCBRecipeTypes;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DeployerApplicationWithGasRecipe extends ItemApplicationWithGasRecipe implements IAssemblyRecipeWithGas {
    public DeployerApplicationWithGasRecipe(ItemApplicationWithGasRecipeParams params) {
        super(CCBRecipeTypes.DEPLOYING_WITH_GAS, params);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Component getDescriptionForAssembly() {
        ItemStack[] stacks = getRequiredHeldItem().getItems();
        if (stacks.length == 0) {
            return Component.literal("Invalid");
        }
        return CreateLang.translateDirect("recipe.assembly.deploying_item", Component.translatable(stacks[0].getDescriptionId()).getString());
    }

    @Override
    public void addAssemblyIngredients(List<Ingredient> list) {
        list.add(getRequiredHeldItem());
    }

    @Override
    public void addRequiredMachines(Set<ItemLike> list) {
        list.add(AllBlocks.DEPLOYER.get());
    }
}
