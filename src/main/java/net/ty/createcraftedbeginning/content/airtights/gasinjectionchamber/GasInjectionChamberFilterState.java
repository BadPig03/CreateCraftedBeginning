package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasInjectionChamberFilterState {
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
        return GasInjectionChamberFilterItem.getFanProcessingTypeId(installedFilter);
    }

    boolean install(ItemStack stack) {
        if (hasInstalledFilter() || !GasInjectionChamberFilterItem.isFilter(stack)) {
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

    void writeInstalledFilter(CompoundTag compoundTag, Provider provider) {
        if (installedFilter.isEmpty()) {
            return;
        }

        compoundTag.put(COMPOUND_KEY_INSTALLED_FILTER, installedFilter.saveOptional(provider));
    }

    void readInstalledFilter(CompoundTag compoundTag, Provider provider) {
        installedFilter = compoundTag.contains(COMPOUND_KEY_INSTALLED_FILTER) ? ItemStack.parseOptional(provider, compoundTag.getCompound(COMPOUND_KEY_INSTALLED_FILTER)) : ItemStack.EMPTY;
        if (GasInjectionChamberFilterItem.isFilter(installedFilter)) {
            return;
        }

        installedFilter = ItemStack.EMPTY;
    }
}
