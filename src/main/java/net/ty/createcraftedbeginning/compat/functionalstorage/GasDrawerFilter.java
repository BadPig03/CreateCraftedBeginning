package net.ty.createcraftedbeginning.compat.functionalstorage;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasDrawerFilter implements INBTSerializable<CompoundTag> {
    private final GasStack[] filters;

    GasDrawerFilter(int slots) {
        filters = new GasStack[slots];
        Arrays.fill(filters, GasStack.EMPTY);
    }

    GasStack get(int slot) {
        return filters[slot];
    }

    void set(int slot, GasStack stack) {
        filters[slot] = stack.isEmpty() ? GasStack.EMPTY : stack.copyWithAmount(1);
    }

    void clear() {
        Arrays.fill(filters, GasStack.EMPTY);
    }

    int size() {
        return filters.length;
    }

    @Override
    public CompoundTag serializeNBT(Provider provider) {
        CompoundTag tag = new CompoundTag();
        for (int slot = 0; slot < filters.length; slot++) {
            GasStack filter = filters[slot];
            if (filter.isEmpty()) {
                continue;
            }

            tag.put(Integer.toString(slot), filter.saveOptional(provider));
        }
        return tag;
    }

    @Override
    public void deserializeNBT(Provider provider, CompoundTag nbt) {
        for (int slot = 0; slot < filters.length; slot++) {
            String key = Integer.toString(slot);
            set(slot, nbt.contains(key) ? GasStack.parseOptional(provider, nbt.getCompound(key)) : GasStack.EMPTY);
        }
    }
}
