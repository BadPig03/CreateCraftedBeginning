package net.ty.createcraftedbeginning.content.crates.sturdycrate;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class SturdyCratePortableStorage {
    private SturdyCratePortableStorage() {
    }

    public static boolean hasStoredData(SturdyCrateBlockEntity crate) {
        return crate.getStoredCount() > 0 || !crate.getFilterItem().isEmpty();
    }

    public static void saveToItem(SturdyCrateBlockEntity crate, ItemStack crateItem) {
        SturdyCrateContents contents = new SturdyCrateContents(crate.getStoredItem(), crate.getStoredCount(), crate.getFilterItem());
        if (contents.hasData()) {
            crateItem.set(CCBDataComponents.STURDY_CRATE_CONTENTS, contents);
            return;
        }

        crateItem.remove(CCBDataComponents.STURDY_CRATE_CONTENTS);
    }

    public static void loadFromItem(SturdyCrateBlockEntity crate, ItemStack crateItem) {
        SturdyCrateContents contents = crateItem.get(CCBDataComponents.STURDY_CRATE_CONTENTS);
        if (contents == null) {
            contents = SturdyCrateContents.empty();
        }

        crate.setStoredItems(contents.content(), contents.count());
        crate.setFilterItem(contents.filterItem());
    }
}
