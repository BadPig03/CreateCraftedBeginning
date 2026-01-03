package net.ty.createcraftedbeginning.recipe.trie;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import org.jetbrains.annotations.NotNull;

public sealed interface AbstractVariant {
    final class AbstractItem implements AbstractVariant {
        private final @NotNull Item item;
        private final int hashCode;

        public AbstractItem(@NotNull Item item) {
            this.item = item;
            hashCode = item.hashCode();
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof AbstractItem other && item == other.item;
        }
    }

    final class AbstractFluid implements AbstractVariant {
        private final @NotNull Fluid fluid;
        private final int hashCode;

        public AbstractFluid(@NotNull Fluid fluid) {
            this.fluid = fluid;
            hashCode = fluid.hashCode();
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof AbstractFluid other && fluid == other.fluid;
        }
    }

    final class AbstractGas implements AbstractVariant {
        private final @NotNull Gas gas;
        private final int hashCode;

        public AbstractGas(@NotNull Gas gas) {
            this.gas = gas;
            hashCode = gas.hashCode();
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof AbstractGas other && gas == other.gas;
        }
    }
}
