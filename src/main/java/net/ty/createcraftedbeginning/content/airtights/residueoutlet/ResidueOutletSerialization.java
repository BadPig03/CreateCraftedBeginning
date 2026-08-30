package net.ty.createcraftedbeginning.content.airtights.residueoutlet;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.ty.createcraftedbeginning.foundation.CCBNbtUtils;

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
        CCBNbtUtils.putTag(compoundTag, COMPOUND_KEY_INVENTORY, inventory.serializeNBT(provider));
    }

    void read(CompoundTag compoundTag, Provider provider) {
        if (!CCBNbtUtils.contains(compoundTag, COMPOUND_KEY_INVENTORY)) {
            return;
        }

        inventory.deserializeNBT(provider, CCBNbtUtils.getCompound(compoundTag, COMPOUND_KEY_INVENTORY));
    }
}
