package net.ty.createcraftedbeginning.content.airtights.residueoutlet;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class ResidueOutletSerialization {
    private static final String COMPOUND_KEY_INVENTORY = "Inventory";

    private final ResidueOutletInventory inventory;

    ResidueOutletSerialization(ResidueOutletInventory inventory) {
        this.inventory = inventory;
    }

    void write(CompoundTag compoundTag, Provider provider) {
        compoundTag.put(COMPOUND_KEY_INVENTORY, inventory.serializeNBT(provider));
    }

    void read(CompoundTag compoundTag, Provider provider) {
        if (!compoundTag.contains(COMPOUND_KEY_INVENTORY)) {
            return;
        }

        inventory.deserializeNBT(provider, compoundTag.getCompound(COMPOUND_KEY_INVENTORY));
    }
}
