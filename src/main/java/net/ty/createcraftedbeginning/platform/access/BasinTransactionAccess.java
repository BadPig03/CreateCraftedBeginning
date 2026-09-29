package net.ty.createcraftedbeginning.platform.access;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface BasinTransactionAccess {
    List<ItemStack> ccb$copyTransactionItemOverflow();

    void ccb$restoreTransactionItemOverflow(List<ItemStack> itemOverflowSnapshot);

    List<FluidStack> ccb$copyTransactionFluidOverflow();

    void ccb$restoreTransactionFluidOverflow(List<FluidStack> fluidOverflowSnapshot);
}
