package net.ty.createcraftedbeginning.content.airtights.airtighthatch;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.foundation.CCBNbtUtils;

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
            CCBNbtUtils.putLong(compoundTag, COMPOUND_KEY_CAPACITY, hatch.getHatchCapacity());
            return;
        }

        if (canisterManager.isEmpty()) {
            return;
        }

        CCBNbtUtils.putTag(compoundTag, COMPOUND_KEY_CANISTER, canisterManager.getStoredCanister().saveOptional(provider));
        CCBNbtUtils.putLong(compoundTag, COMPOUND_KEY_CAPACITY, hatch.getHatchCapacity());
    }

    void read(CompoundTag compoundTag, Provider provider, boolean clientPacket) {
        if (clientPacket) {
            if (CCBNbtUtils.contains(compoundTag, COMPOUND_KEY_CAPACITY)) {
                hatch.getGasTankBehaviour().getPrimaryHandler().setCapacity(Math.max(0, CCBNbtUtils.getLong(compoundTag, COMPOUND_KEY_CAPACITY)));
            }
            return;
        }

        ItemStack storedCanister = CCBNbtUtils.contains(compoundTag, COMPOUND_KEY_CANISTER) ? ItemStack.parseOptional(provider, CCBNbtUtils.getCompound(compoundTag, COMPOUND_KEY_CANISTER)) : ItemStack.EMPTY;
        canisterManager.setStoredCanister(storedCanister);
        if (!canisterManager.isEmpty() && CCBNbtUtils.contains(compoundTag, COMPOUND_KEY_CAPACITY)) {
            hatch.getGasTankBehaviour().getPrimaryHandler().setCapacity(Math.max(0, CCBNbtUtils.getLong(compoundTag, COMPOUND_KEY_CAPACITY)));
        }
        canisterManager.updateCapacity(false);
    }
}
