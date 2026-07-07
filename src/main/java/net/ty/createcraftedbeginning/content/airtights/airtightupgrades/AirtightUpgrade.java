package net.ty.createcraftedbeginning.content.airtights.airtightupgrades;

import net.createmod.catnip.data.Couple;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gascanisters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightArmorsUtils;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades.AirtightBootsUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.AirtightChestplateUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades.AirtightHelmetUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.upgrades.AirtightLeggingsUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.upgrades.AirtightHandheldDrillUpgradeRegistry;
import net.ty.createcraftedbeginning.data.CCBIcons;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
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

    List<Component> getComponents(Player player, ItemStack item);

    boolean canApply(Player player);

    boolean meetsConditions(Player player, ItemStack item);

    boolean isRightIndicator();

    CCBIcons getIcon();

    Component getDescription();

    Component getTitle();

    Couple<Integer> getOffset();

    int getGasConsumptionPerSecond(Player player, ItemStack item);

    int getIndex();

    Item getUpgradeItem();

    ResourceLocation getID();

    void applyEffect(Player player);

    default float getGasConsumptionMultiplier(Player player) {
        return 1;
    }

    default boolean testUpgradeItem(ItemStack item) {
        return item.is(getUpgradeItem());
    }

    default boolean startsEnabled() {
        return false;
    }

    default boolean startsInstalled() {
        return false;
    }

    default boolean isActive(Player player, ItemStack item) {
        if (!isEnabled(item) || !meetsConditions(player, item)) {
            return false;
        }

        int consumption = getGasConsumptionPerSecond(player, item);
        if (consumption < 0) {
            return true;
        }
        else if (consumption == 0) {
            return !CanisterContainerSuppliers.getFirstAvailableGasContent(player).getGasType().isEmpty();
        }
        return GlobalAirtightUpgradesConsumptionManager.isPowered(player, this);
    }

    default boolean isEnabled(ItemStack item) {
        AirtightUpgradeStatus status = getUpgradeStatus(item);
        return status.isInstalled() && status.isEnabled();
    }

    default boolean isInstalled(ItemStack item) {
        return getUpgradeStatus(item).isInstalled();
    }

    default boolean isRequesting(Player player, ItemStack item) {
        return isEnabled(item) && meetsConditions(player, item) && getGasConsumptionPerSecond(player, item) > 0;
    }

    default AirtightUpgradeStatus getUpgradeStatus(ItemStack item) {
        List<AirtightUpgradeStatus> upgradeStatusList = item.getOrDefault(CCBDataComponents.AIRTIGHT_UPGRADE_STATUS, AirtightArmorsUtils.getDefaultUpgradeList(item));
        if (upgradeStatusList.isEmpty()) {
            return new AirtightUpgradeStatus(getID(), false, false);
        }

        int index = getIndex();
        if (index < 0 || index >= upgradeStatusList.size()) {
            return new AirtightUpgradeStatus(getID(), false, false);
        }

        AirtightUpgradeStatus status = upgradeStatusList.get(index);
        if (!status.id().equals(getID())) {
            return new AirtightUpgradeStatus(getID(), false, false);
        }

        return status;
    }
}
