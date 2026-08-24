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
public final class GasDrawerFilter implements INBTSerializable<CompoundTag> {
    private final GasStack[] filters;

    public GasDrawerFilter(int slots) {
        filters = new GasStack[slots];
        Arrays.fill(filters, GasStack.EMPTY);
    }

    public GasStack get(int slot) {
        return filters[slot];
    }

    public void set(int slot, GasStack stack) {
        filters[slot] = stack.isEmpty() ? GasStack.EMPTY : stack.copyWithAmount(1);
    }

    public void clear() {
        Arrays.fill(filters, GasStack.EMPTY);
    }

    public int size() {
        return filters.length;
    }

    @Override
    public CompoundTag serializeNBT(Provider provider) {
        CompoundTag filterTag = new CompoundTag();
        for (int slot = 0; slot < filters.length; slot++) {
            GasStack filterGas = filters[slot];
            if (filterGas.isEmpty()) {
                continue;
            }

            filterTag.put(Integer.toString(slot), filterGas.saveOptional(provider));
        }
        return filterTag;
    }

    @Override
    public void deserializeNBT(Provider provider, CompoundTag nbt) {
        for (int slot = 0; slot < filters.length; slot++) {
            String slotKey = Integer.toString(slot);
            set(slot, nbt.contains(slotKey) ? GasStack.parseOptional(provider, nbt.getCompound(slotKey)) : GasStack.EMPTY);
        }
    }
}
