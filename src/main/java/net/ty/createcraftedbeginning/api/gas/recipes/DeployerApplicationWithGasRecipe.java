package net.ty.createcraftedbeginning.api.gas.recipes;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.gas.gases.IAssemblyRecipeWithGas;
import net.ty.createcraftedbeginning.api.gas.recipes.SequencedAssemblyWithGasSubCategory.AssemblyDeploying;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class DeployerApplicationWithGasRecipe extends ItemApplicationWithGasRecipe implements IAssemblyRecipeWithGas {
    public DeployerApplicationWithGasRecipe(ItemApplicationWithGasRecipeParams params) {
        super(CCBRecipeTypes.DEPLOYING_WITH_GAS, params);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Component getDescriptionForAssembly() {
        ItemStack[] matchingStacks = ingredients.get(1).getItems();
        return matchingStacks.length == 0 ? Component.literal("Invalid") : CreateLang.translateDirect("recipe.assembly.deploying_item", Component.translatable(matchingStacks[0].getDescriptionId()).getString());
    }

    @Override
    public void addRequiredMachines(@NotNull Set<ItemLike> list) {
        list.add(AllBlocks.DEPLOYER.get());
    }

    @Override
    public void addAssemblyIngredients(@NotNull List<Ingredient> list) {
        list.add(ingredients.get(1));
    }

    @Override
    public Supplier<Supplier<SequencedAssemblyWithGasSubCategory>> getJEISubCategory() {
        return () -> AssemblyDeploying::new;
    }
}
