package net.ty.createcraftedbeginning.content.airtights.airtighthatch;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightHatchSerialization {
    public static final String COMPOUND_KEY_CANISTER = "Canister";
    private static final String COMPOUND_KEY_CAPACITY = "Capacity";

    private final AirtightHatchBlockEntity hatch;
    private final AirtightHatchCanisterManager canisterManager;

    public AirtightHatchSerialization(AirtightHatchBlockEntity hatch, AirtightHatchCanisterManager canisterManager) {
        this.hatch = hatch;
        this.canisterManager = canisterManager;
    }

    public void write(CompoundTag tag, Provider provider, boolean clientPacket) {
        if (clientPacket) {
            tag.putLong(COMPOUND_KEY_CAPACITY, hatch.getHatchCapacity());
            return;
        }

        ItemStack canister = canisterManager.getStoredCanister();
        if (canister.isEmpty()) {
            return;
        }

        tag.put(COMPOUND_KEY_CANISTER, canister.saveOptional(provider));
        tag.putLong(COMPOUND_KEY_CAPACITY, hatch.getHatchCapacity());
    }

    public void read(CompoundTag tag, Provider provider, boolean clientPacket) {
        if (clientPacket) {
            if (tag.contains(COMPOUND_KEY_CAPACITY)) {
                hatch.getGasTankBehaviour().getPrimaryHandler().setCapacity(Math.max(0, tag.getLong(COMPOUND_KEY_CAPACITY)));
            }
            return;
        }

        ItemStack canister = tag.contains(COMPOUND_KEY_CANISTER) ? ItemStack.parseOptional(provider, tag.getCompound(COMPOUND_KEY_CANISTER)) : ItemStack.EMPTY;
        canisterManager.setStoredCanister(canister);
        if (!canister.isEmpty() && tag.contains(COMPOUND_KEY_CAPACITY)) {
            hatch.getGasTankBehaviour().getPrimaryHandler().setCapacity(Math.max(0, tag.getLong(COMPOUND_KEY_CAPACITY)));
        }
        canisterManager.updateCapacity(false);
    }
}
