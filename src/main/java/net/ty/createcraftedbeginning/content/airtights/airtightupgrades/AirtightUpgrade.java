package net.ty.createcraftedbeginning.content.airtights.airtightupgrades;

import net.createmod.catnip.data.Couple;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightArmorsUtils;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades.AirtightBootsUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.AirtightChestplateUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades.AirtightHelmetUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.upgrades.AirtightLeggingsUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.upgrades.AirtightHandheldDrillUpgradeRegistry;
import net.ty.createcraftedbeginning.data.CCBIcons;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface AirtightUpgrade {
    @Nullable
    static AirtightUpgrade getByID(ResourceLocation id) {
        AirtightUpgrade helmet = AirtightHelmetUpgradeRegistry.getByID(id);
        if (helmet != null) {
            return helmet;
        }

        AirtightUpgrade chestplate = AirtightChestplateUpgradeRegistry.getByID(id);
        if (chestplate != null) {
            return chestplate;
        }

        AirtightUpgrade leggings = AirtightLeggingsUpgradeRegistry.getByID(id);
        if (leggings != null) {
            return leggings;
        }

        AirtightUpgrade boots = AirtightBootsUpgradeRegistry.getByID(id);
        if (boots != null) {
            return boots;
        }

        return AirtightHandheldDrillUpgradeRegistry.getByID(id);
    }

    int getIndex();

    ResourceLocation getID();

    Item getUpgradeItem();

    Couple<Integer> getOffset();

    CCBIcons getIcon();

    boolean isRightIndicator();

    Component getTitle();

    Component getDescription();

    @Nullable Component getGasCostComponent(Player player);

    int getGasCost(Player player);

    boolean canApply(Player player);

    void applyEffect(Player player);

    default boolean startsEnabled() {
        return false;
    }

    default boolean startsInstalled() {
        return false;
    }

    default boolean isEnabled(@NotNull ItemStack item) {
        List<AirtightUpgradeStatus> upgradeStatusList = item.getOrDefault(CCBDataComponents.AIRTIGHT_UPGRADE_STATUS, AirtightArmorsUtils.getDefaultUpgradeList(item));
        return upgradeStatusList.get(getIndex()).isEnabled();
    }

    default boolean isInstalled(@NotNull ItemStack item) {
        List<AirtightUpgradeStatus> upgradeStatusList = item.getOrDefault(CCBDataComponents.AIRTIGHT_UPGRADE_STATUS, AirtightArmorsUtils.getDefaultUpgradeList(item));
        return upgradeStatusList.get(getIndex()).isInstalled();
    }
}
