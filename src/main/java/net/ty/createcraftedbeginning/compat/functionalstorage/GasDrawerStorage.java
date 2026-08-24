package net.ty.createcraftedbeginning.compat.functionalstorage;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasDrawerStorage implements INBTSerializable<CompoundTag> {
    public static final String COMPOUND_KEY_STORAGE = "gasStorage";
    private static final String COMPOUND_KEY_GAS = "Gas";

    private final GasDrawerHandler handler;

    public GasDrawerStorage(GasDrawerHandler handler) {
        this.handler = handler;
    }

    public static GasStack readStoredGas(CompoundTag storageTag, int slot, Provider provider) {
        CompoundTag tankTag = storageTag.getCompound(Integer.toString(slot));
        if (!tankTag.contains(COMPOUND_KEY_GAS)) {
            return GasStack.EMPTY;
        }
        return GasStack.parseOptional(provider, tankTag.getCompound(COMPOUND_KEY_GAS));
    }

    @Override
    public CompoundTag serializeNBT(Provider provider) {
        CompoundTag storageTag = new CompoundTag();
        GasDrawerTank[] drawerTanks = handler.getInternalTanks();
        for (int tankIndex = 0; tankIndex < drawerTanks.length; tankIndex++) {
            GasDrawerTank drawerTank = drawerTanks[tankIndex];
            if (drawerTank.getStoredStack().isEmpty()) {
                continue;
            }

            storageTag.put(Integer.toString(tankIndex), drawerTank.write(provider, new CompoundTag()));
        }
        return storageTag;
    }

    @Override
    public void deserializeNBT(Provider provider, CompoundTag nbt) {
        GasDrawerTank[] drawerTanks = handler.getInternalTanks();
        for (int tankIndex = 0; tankIndex < drawerTanks.length; tankIndex++) {
            String tankKey = Integer.toString(tankIndex);
            if (nbt.contains(tankKey)) {
                drawerTanks[tankIndex].read(provider, nbt.getCompound(tankKey));
                continue;
            }

            drawerTanks[tankIndex].setGasStack(GasStack.EMPTY);
        }
    }
}
