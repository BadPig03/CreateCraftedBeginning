package net.ty.createcraftedbeginning.content.airtights.gasfilter;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@FunctionalInterface
public interface GasFilter {
    boolean test(ItemStack filterItem, GasStack filterGasStack);

    default @Nullable GasFilterDescription describe(ItemStack filterItem) {
        return null;
    }

    default Predicate<GasStack> compile(ItemStack filterItem) {
        ItemStack filterSnapshot = filterItem.copyWithCount(1);
        return gasStack -> test(filterSnapshot, gasStack);
    }
}
