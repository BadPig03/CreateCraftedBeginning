package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasInjectionChamberFilterState {
    static final String COMPOUND_KEY_FILTER_LOCKED = "FilterLocked";
    private static final String COMPOUND_KEY_INSTALLED_FILTER = "InstalledFilter";

    private ItemStack installedFilter = ItemStack.EMPTY;
    private boolean clientLocked;

    boolean hasInstalledFilter() {
        return !installedFilter.isEmpty();
    }

    ItemStack getInstalledFilter() {
        return installedFilter;
    }

    Optional<ResourceLocation> getFanProcessingType() {
        return GasInjectionChamberUtils.getFanProcessingTypeId(installedFilter);
    }

    boolean install(ItemStack stack) {
        if (hasInstalledFilter() || !GasInjectionChamberUtils.isFilter(stack)) {
            return false;
        }

        installedFilter = stack.copyWithCount(1);
        return true;
    }

    ItemStack remove() {
        ItemStack removedFilter = installedFilter;
        installedFilter = ItemStack.EMPTY;
        return removedFilter;
    }

    boolean isClientLocked() {
        return clientLocked;
    }

    void setClientLocked(boolean clientLocked) {
        this.clientLocked = clientLocked;
    }

    void writeInstalledFilter(CompoundTag tag, Provider provider) {
        if (installedFilter.isEmpty()) {
            return;
        }

        tag.put(COMPOUND_KEY_INSTALLED_FILTER, installedFilter.saveOptional(provider));
    }

    void readInstalledFilter(CompoundTag tag, Provider provider) {
        installedFilter = tag.contains(COMPOUND_KEY_INSTALLED_FILTER) ? ItemStack.parseOptional(provider, tag.getCompound(COMPOUND_KEY_INSTALLED_FILTER)) : ItemStack.EMPTY;
        if (GasInjectionChamberUtils.isFilter(installedFilter)) {
            return;
        }

        installedFilter = ItemStack.EMPTY;
    }
}
