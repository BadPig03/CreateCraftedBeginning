package net.ty.createcraftedbeginning.recipe;

import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.sequenced.IAssemblyRecipe;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.compat.jei.category.GasInjectionSequencedAssemblySubCategory;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.recipe.gas.GasRecipeRequirement;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlanner;
import net.ty.createcraftedbeginning.recipe.gas.processing.GasProcessingRecipeParams;
import net.ty.createcraftedbeginning.recipe.gas.processing.StandardGasProcessingRecipe;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasInjectionRecipe extends StandardGasProcessingRecipe<SingleRecipeInput> implements IAssemblyRecipe {

    public GasInjectionRecipe(GasProcessingRecipeParams params) {
        super(CCBRecipeTypes.GAS_INJECTION, params);
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return fluidIngredients.isEmpty() && !ingredients.isEmpty() && !input.isEmpty() && ingredients.getFirst().test(input.getItem(0));
    }

    @Override
    protected int getMaxInputCount() {
        return 1;
    }

    @Override
    protected int getMaxOutputCount() {
        return 1;
    }

    @Override
    protected int getMaxFluidInputCount() {
        return 1;
    }

    @Override
    protected int getMaxFluidOutputCount() {
        return 1;
    }

    @Override
    protected int getMaxGasInputCount() {
        return 1;
    }

    @Override
    protected void validateSpecial(List<String> errors) {
        if (gasIngredients.size() != 1) {
            errors.add("Gas injection recipes require exactly one gas ingredient.");
        }

        int inputMediumCount = (ingredients.isEmpty() ? 0 : 1) + (fluidIngredients.isEmpty() ? 0 : 1);
        if (inputMediumCount != 1) {
            errors.add("Gas injection recipes require exactly one item or fluid input.");
        }

        int outputMediumCount = (results.isEmpty() ? 0 : 1) + (fluidResults.isEmpty() ? 0 : 1);
        if (outputMediumCount != 1) {
            errors.add("Gas injection recipes require exactly one item or fluid output.");
        }

        if (!fluidIngredients.isEmpty() && fluidIngredients.getFirst().amount() <= 0) {
            errors.add("Fluid gas injection recipe input amount must be greater than zero.");
        }

        if (!(!fluidResults.isEmpty() && (fluidResults.getFirst().isEmpty() || fluidResults.getFirst().getAmount() <= 0))) {
            return;
        }

        errors.add("Fluid gas injection recipe output must not be empty.");
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Component getDescriptionForAssembly() {
        String gasName = gasIngredients.getFirst().getFirstGas().getHoverName().getString();
        return CCBLang.translateDirect("recipe.assembly.gas_injection_injecting_gas", gasName);
    }

    @Override
    public void addRequiredMachines(Set<ItemLike> list) {
        list.add(CCBBlocks.GAS_INJECTION_CHAMBER_BLOCK.get());
    }

    @Override
    public void addAssemblyIngredients(List<Ingredient> list) {
    }

    @Override
    public Supplier<Supplier<SequencedAssemblySubCategory>> getJEISubCategory() {
        return () -> GasInjectionSequencedAssemblySubCategory::new;
    }

    public boolean isFluidInjection() {
        return ingredients.isEmpty() && results.isEmpty() && fluidIngredients.size() == 1 && fluidResults.size() == 1;
    }

    public boolean hasItemInput() {
        return !ingredients.isEmpty();
    }

    public boolean hasFluidInput() {
        return !fluidIngredients.isEmpty();
    }

    public boolean hasItemOutput() {
        return !results.isEmpty();
    }

    public boolean hasFluidOutput() {
        return !fluidResults.isEmpty();
    }

    public boolean canProcessOnBelt() {
        return hasItemInput() && hasItemOutput();
    }

    public ItemStack rollFirstResult(Level level) {
        return rollResults(level.random).stream().findFirst().orElse(ItemStack.EMPTY);
    }

    public GasRecipeRequirement getGasRequirement() {
        if (getGasRequirements().isEmpty()) {
            throw new IllegalStateException("Gas injection recipe has no gas requirement.");
        }

        return getGasRequirements().getFirst();
    }

    public SizedFluidIngredient getFluidIngredient() {
        if (fluidIngredients.isEmpty()) {
            throw new IllegalStateException("Gas injection recipe has no fluid ingredient.");
        }

        return fluidIngredients.getFirst();
    }

    public FluidStack getFluidResult() {
        if (fluidResults.isEmpty()) {
            return FluidStack.EMPTY;
        }

        return fluidResults.getFirst();
    }

    public Ingredient getIngredient() {
        if (ingredients.isEmpty()) {
            return Ingredient.EMPTY;
        }

        return ingredients.getFirst();
    }

    boolean matchesGas(GasPressureCompartment gasSource) {
        return GasConsumptionPlanner.plan(getGasRequirement(), gasSource).isPresent();
    }

    boolean matchesBasinInput(IItemHandler items, IFluidHandler fluids) {
        if (hasItemInput()) {
            Ingredient ingredient = getIngredient();
            for (int slot = 0; slot < items.getSlots(); slot++) {
                ItemStack itemStack = items.getStackInSlot(slot);
                if (!itemStack.isEmpty() && ingredient.test(itemStack)) {
                    return true;
                }
            }

            return false;
        }

        return hasFluidInput() && matchesFluid(fluids);
    }

    boolean matchesFluid(IFluidHandler fluids) {
        if (!isFluidInjection()) {
            return false;
        }

        SizedFluidIngredient fluidIngredient = getFluidIngredient();
        int remainingAmount = fluidIngredient.amount();
        for (int tankIndex = 0; tankIndex < fluids.getTanks() && remainingAmount > 0; tankIndex++) {
            FluidStack fluidStack = fluids.getFluidInTank(tankIndex);
            if (fluidStack.isEmpty() || !fluidIngredient.test(fluidStack)) {
                continue;
            }

            remainingAmount -= Math.min(remainingAmount, fluidStack.getAmount());
        }
        return remainingAmount <= 0;
    }
}
