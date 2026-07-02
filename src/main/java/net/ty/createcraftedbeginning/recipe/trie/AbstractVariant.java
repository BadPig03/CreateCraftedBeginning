package net.ty.createcraftedbeginning.recipe.trie;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public sealed interface AbstractVariant {
    final class AbstractItem implements AbstractVariant {
        private final @NotNull Item item;
        private final int hashCode;

        public AbstractItem(Item item) {
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

        public AbstractFluid(Fluid fluid) {
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
        private final @NotNull Gas gasType;
        private final int hashCode;

        public AbstractGas(Gas gasType) {
            this.gasType = gasType;
            hashCode = gasType.hashCode();
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof AbstractGas other && gasType == other.gasType;
        }
    }
}
