package net.ty.createcraftedbeginning.compat.functionalstorage;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasDrawerStorage implements INBTSerializable<CompoundTag> {
    static final String COMPOUND_KEY_STORAGE = "gasStorage";
    private static final String COMPOUND_KEY_GAS = "Gas";

    private final GasDrawerHandler handler;

    GasDrawerStorage(GasDrawerHandler handler) {
        this.handler = handler;
    }

    static GasStack readStoredGas(CompoundTag storage, int slot, Provider provider) {
        CompoundTag tank = storage.getCompound(Integer.toString(slot));
        if (!tank.contains(COMPOUND_KEY_GAS)) {
            return GasStack.EMPTY;
        }
        return GasStack.parseOptional(provider, tank.getCompound(COMPOUND_KEY_GAS));
    }

    @Override
    public CompoundTag serializeNBT(Provider provider) {
        CompoundTag tag = new CompoundTag();
        GasDrawerTank[] tanks = handler.getInternalTanks();
        for (int slot = 0; slot < tanks.length; slot++) {
            GasDrawerTank tank = tanks[slot];
            if (tank.getStoredStack().isEmpty()) {
                continue;
            }

            tag.put(Integer.toString(slot), tank.write(provider, new CompoundTag()));
        }
        return tag;
    }

    @Override
    public void deserializeNBT(Provider provider, CompoundTag nbt) {
        GasDrawerTank[] tanks = handler.getInternalTanks();
        for (int slot = 0; slot < tanks.length; slot++) {
            String key = Integer.toString(slot);
            if (nbt.contains(key)) {
                tanks[slot].read(provider, nbt.getCompound(key));
                continue;
            }

            tanks[slot].setGasStack(GasStack.EMPTY);
        }
    }
}
