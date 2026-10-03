package net.ty.createcraftedbeginning.compat.functionalstorage;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.gas.storage.GasTankLimits;
import net.ty.createcraftedbeginning.gas.storage.GasTankState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasDrawerStorage implements INBTSerializable<CompoundTag> {
    public static final String COMPOUND_KEY_STORAGE = "gasStorage";
    private static final String COMPOUND_KEY_GAS = "Gas";
    private static final String COMPOUND_KEY_VOLUME = "Volume";
    private static final String COMPOUND_KEY_MAX_PRESSURE = "MaxPressure";

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
            GasTankState state = drawerTanks[tankIndex].snapshot();
            GasStack storedGas = state.contents();
            if (storedGas.isEmpty()) {
                continue;
            }

            GasTankLimits limits = state.limits();
            CompoundTag tankTag = new CompoundTag();
            tankTag.putLong(COMPOUND_KEY_VOLUME, limits.volumeLiters());
            tankTag.putLong(COMPOUND_KEY_MAX_PRESSURE, limits.maxPressurePa());
            tankTag.put(COMPOUND_KEY_GAS, storedGas.saveOptional(provider));
            storageTag.put(Integer.toString(tankIndex), tankTag);
        }
        return storageTag;
    }

    @Override
    public void deserializeNBT(Provider provider, CompoundTag nbt) {
        GasDrawerTank[] drawerTanks = handler.getInternalTanks();
        GasTankState[] states = new GasTankState[drawerTanks.length];
        for (int tankIndex = 0; tankIndex < drawerTanks.length; tankIndex++) {
            String tankKey = Integer.toString(tankIndex);
            GasDrawerTank drawerTank = drawerTanks[tankIndex];
            if (!nbt.contains(tankKey)) {
                states[tankIndex] = new GasTankState(drawerTank.getLimits(), GasStack.EMPTY);
                continue;
            }

            CompoundTag tankTag = nbt.getCompound(tankKey);
            long volume = Math.max(0, tankTag.getLong(COMPOUND_KEY_VOLUME));
            long maxPressurePa = Math.max(0, tankTag.getLong(COMPOUND_KEY_MAX_PRESSURE));
            GasStack storedGas = tankTag.contains(COMPOUND_KEY_GAS)
                ? GasStack.parseOptional(provider, tankTag.getCompound(COMPOUND_KEY_GAS))
                : GasStack.EMPTY;
            states[tankIndex] = new GasTankState(new GasTankLimits(volume, maxPressurePa), storedGas);
        }

        handler.beginTransaction();
        try {
            handler.restoreStates(states);
        }
        finally {
            handler.endTransaction(false);
        }
    }
}
