package net.ty.createcraftedbeginning.api.gas.gases;

import it.unimi.dsi.fastutil.Hash.Strategy;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class GasStackLinkedSet {
    public static final Strategy<? super GasStack> TYPE_AND_COMPONENTS = new Strategy<>() {
        @Override
        public int hashCode(@Nullable GasStack stack) {
            return GasStack.hashGasAndComponents(stack);
        }

        @Override
        public boolean equals(@Nullable GasStack first, @Nullable GasStack second) {
            return first == second || first != null && second != null && first.isEmpty() == second.isEmpty() && GasStack.isSameGasSameComponents(first, second);
        }
    };

    @Contract(value = " -> new", pure = true)
    public static @NotNull Set<GasStack> createTypeAndComponentsSet() {
        return new ObjectLinkedOpenCustomHashSet<>(TYPE_AND_COMPONENTS);
    }
}
