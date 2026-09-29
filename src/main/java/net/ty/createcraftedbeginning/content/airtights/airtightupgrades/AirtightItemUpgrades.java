package net.ty.createcraftedbeginning.content.airtights.airtightupgrades;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades.AirtightBootsUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.AirtightChestplateUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades.AirtightHelmetUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.upgrades.AirtightLeggingsUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.upgrades.AirtightHandheldDrillUpgradeRegistry;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightItemUpgrades {
    private AirtightItemUpgrades() {
    }

    public static List<AirtightUpgradeStatus> getDefaultUpgradeList(ItemStack item) {
        if (item.is(CCBItems.AIRTIGHT_HELMET)) {
            return AirtightHelmetUpgradeRegistry.getDefaultUpgradeList();
        }

        if (item.is(CCBItems.AIRTIGHT_CHESTPLATE)) {
            return AirtightChestplateUpgradeRegistry.getDefaultUpgradeList();
        }

        if (item.is(CCBItems.AIRTIGHT_LEGGINGS)) {
            return AirtightLeggingsUpgradeRegistry.getDefaultUpgradeList();
        }

        if (item.is(CCBItems.AIRTIGHT_BOOTS)) {
            return AirtightBootsUpgradeRegistry.getDefaultUpgradeList();
        }

        if (item.is(CCBItems.AIRTIGHT_HANDHELD_DRILL)) {
            return AirtightHandheldDrillUpgradeRegistry.getDefaultUpgradeList();
        }

        return List.of();
    }

    public static List<AirtightUpgrade> getAllUpgrades(ItemStack item) {
        if (item.is(CCBItems.AIRTIGHT_HELMET)) {
            return AirtightHelmetUpgradeRegistry.getAll();
        }

        if (item.is(CCBItems.AIRTIGHT_CHESTPLATE)) {
            return AirtightChestplateUpgradeRegistry.getAll();
        }

        if (item.is(CCBItems.AIRTIGHT_LEGGINGS)) {
            return AirtightLeggingsUpgradeRegistry.getAll();
        }

        if (item.is(CCBItems.AIRTIGHT_BOOTS)) {
            return AirtightBootsUpgradeRegistry.getAll();
        }

        if (item.is(CCBItems.AIRTIGHT_HANDHELD_DRILL)) {
            return AirtightHandheldDrillUpgradeRegistry.getAll();
        }

        return List.of();
    }
}
