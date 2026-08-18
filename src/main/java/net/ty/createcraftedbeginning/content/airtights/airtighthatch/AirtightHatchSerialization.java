package net.ty.createcraftedbeginning.content.airtights.airtighthatch;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightHatchSerialization {
    private static final String COMPOUND_KEY_CANISTER = "Canister";
    private static final String COMPOUND_KEY_CAPACITY = "Capacity";

    private final AirtightHatchBlockEntity hatch;
    private final AirtightHatchCanisterManager canisterManager;

    AirtightHatchSerialization(AirtightHatchBlockEntity hatch, AirtightHatchCanisterManager canisterManager) {
        this.hatch = hatch;
        this.canisterManager = canisterManager;
    }

    void write(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        if (clientPacket) {
            compoundTag.putLong(COMPOUND_KEY_CAPACITY, hatch.getHatchCapacity());
            return;
        }

        if (canisterManager.isEmpty()) {
            return;
        }

        ItemStack canister = canisterManager.getStoredCanister();
        compoundTag.put(COMPOUND_KEY_CANISTER, canister.saveOptional(provider));
        compoundTag.putLong(COMPOUND_KEY_CAPACITY, hatch.getHatchCapacity());
    }

    void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        if (clientPacket) {
            if (compoundTag.contains(COMPOUND_KEY_CAPACITY)) {
                hatch.getGasTankBehaviour().getPrimaryHandler().setCapacity(Math.max(0, compoundTag.getLong(COMPOUND_KEY_CAPACITY)));
            }
            return;
        }

        ItemStack canister = compoundTag.contains(COMPOUND_KEY_CANISTER) ? ItemStack.parseOptional(provider, compoundTag.getCompound(COMPOUND_KEY_CANISTER)) : ItemStack.EMPTY;
        canisterManager.setStoredCanister(canister);
        if (!canisterManager.isEmpty() && compoundTag.contains(COMPOUND_KEY_CAPACITY)) {
            hatch.getGasTankBehaviour().getPrimaryHandler().setCapacity(Math.max(0, compoundTag.getLong(COMPOUND_KEY_CAPACITY)));
        }
        canisterManager.updateCapacity(false);
    }
}
