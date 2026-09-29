package net.ty.createcraftedbeginning.content.airtights.airtighthatch;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.gas.behaviour.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.gas.storage.GasTankLimits;
import net.ty.createcraftedbeginning.gas.storage.GasTankState;
import net.ty.createcraftedbeginning.gas.storage.SmartGasTank;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightHatchSerialization {
    private static final String COMPOUND_KEY_CANISTER = "Canister";
    private static final String COMPOUND_KEY_VOLUME = "Volume";
    private static final String COMPOUND_KEY_MAX_PRESSURE = "MaxPressure";

    private final AirtightHatchBlockEntity hatch;
    private final AirtightHatchCanisterManager canisterManager;

    AirtightHatchSerialization(AirtightHatchBlockEntity hatch, AirtightHatchCanisterManager canisterManager) {
        this.hatch = hatch;
        this.canisterManager = canisterManager;
    }

    void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        if (clientPacket) {
            SmartGasTank gasTank = hatch.getGasTankBehaviour().getPrimaryHandler();
            compoundTag.putLong(COMPOUND_KEY_VOLUME, gasTank.getVolume());
            compoundTag.putLong(COMPOUND_KEY_MAX_PRESSURE, gasTank.getMaxPressurePa());
            return;
        }

        if (canisterManager.isEmpty()) {
            return;
        }

        compoundTag.put(COMPOUND_KEY_CANISTER, canisterManager.getStoredCanister().saveOptional(provider));
    }

    void prepareForRead(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        if (clientPacket) {
            if (!compoundTag.contains(COMPOUND_KEY_VOLUME) || !compoundTag.contains(COMPOUND_KEY_MAX_PRESSURE)) {
                return;
            }

            long volume = Math.max(0, compoundTag.getLong(COMPOUND_KEY_VOLUME));
            long maxPressurePa = Math.max(0, compoundTag.getLong(COMPOUND_KEY_MAX_PRESSURE));
            prepareTankForRead(new GasTankLimits(volume, maxPressurePa));
            return;
        }

        ItemStack storedCanister = compoundTag.contains(COMPOUND_KEY_CANISTER) ? ItemStack.parseOptional(provider, compoundTag.getCompound(COMPOUND_KEY_CANISTER)) : ItemStack.EMPTY;
        canisterManager.setStoredCanister(storedCanister);
        prepareTankForRead(canisterManager.getStoredCanisterLimits());
    }

    private void prepareTankForRead(GasTankLimits limits) {
        SmartGasTankBehaviour tankBehaviour = hatch.getGasTankBehaviour();
        tankBehaviour.beginMutation();
        try {
            tankBehaviour.getPrimaryHandler().tryApplyState(new GasTankState(limits, GasStack.EMPTY)).requireAccepted();
        }
        finally {
            tankBehaviour.endMutation();
        }
    }
}
