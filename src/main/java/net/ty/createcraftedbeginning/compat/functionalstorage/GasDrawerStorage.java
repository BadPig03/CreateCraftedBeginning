package net.ty.createcraftedbeginning.compat.functionalstorage;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.foundation.CCBNbtUtils;

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
        CompoundTag tankTag = CCBNbtUtils.getCompound(storageTag, Integer.toString(slot));
        if (!CCBNbtUtils.contains(tankTag, COMPOUND_KEY_GAS)) {
            return GasStack.EMPTY;
        }
        return GasStack.parseOptional(provider, CCBNbtUtils.getCompound(tankTag, COMPOUND_KEY_GAS));
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

            CCBNbtUtils.putTag(storageTag, Integer.toString(tankIndex), drawerTank.write(provider, new CompoundTag()));
        }
        return storageTag;
    }

    @Override
    public void deserializeNBT(Provider provider, CompoundTag nbt) {
        GasDrawerTank[] drawerTanks = handler.getInternalTanks();
        for (int tankIndex = 0; tankIndex < drawerTanks.length; tankIndex++) {
            String tankKey = Integer.toString(tankIndex);
            if (CCBNbtUtils.contains(nbt, tankKey)) {
                drawerTanks[tankIndex].read(provider, CCBNbtUtils.getCompound(nbt, tankKey));
                continue;
            }

            drawerTanks[tankIndex].setGasStack(GasStack.EMPTY);
        }
    }
}
