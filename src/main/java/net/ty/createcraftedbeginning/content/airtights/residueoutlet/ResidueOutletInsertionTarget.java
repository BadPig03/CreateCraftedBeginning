package net.ty.createcraftedbeginning.content.airtights.residueoutlet;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@FunctionalInterface
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface ResidueOutletInsertionTarget {
    int ITEM_PROGRESS_UNITS_PER_ITEM = 65536;

    @Nullable ResidueInsertionPlan createResidueInsertionPlan(FluidStack fluidStack, ItemStack itemStack, int maxAmount);
}
