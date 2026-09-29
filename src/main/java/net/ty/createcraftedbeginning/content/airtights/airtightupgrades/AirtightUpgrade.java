package net.ty.createcraftedbeginning.content.airtights.airtightupgrades;

import net.createmod.catnip.data.Couple;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface AirtightUpgrade {

    List<Component> getComponents(Player player, ItemStack item);

    boolean canApply(Player player);

    boolean meetsConditions(Player player, ItemStack item);

    boolean isRightIndicator();

    AirtightUpgradeIcon getIcon();

    Component getDescription();

    Component getTitle();

    Couple<Integer> getOffset();

    default AirtightUpgradePowerMode getPowerMode() {
        return AirtightUpgradePowerMode.PASSIVE;
    }

    default int getGasConsumptionPerSecond(Player player, ItemStack item) {
        return 0;
    }

    Item getDefaultUpgradeItem();

    default Item getUpgradeItem(Level level) {
        return AirtightUpgradeMaterials.resolveMaterial(this, level.isClientSide);
    }

    ResourceLocation getID();

    void applyEffect(Player player);

    default float getGasConsumptionMultiplier(Player player) {
        return 1;
    }

    default boolean testUpgradeItem(ItemStack item, Level level) {
        return !item.isEmpty() && !startsInstalled() && item.is(getUpgradeItem(level));
    }

    default boolean startsEnabled() {
        return false;
    }

    default boolean startsInstalled() {
        return false;
    }

    default boolean isActive(Player player, ItemStack item) {
        return isEnabled(item) && meetsConditions(player, item) && switch (getPowerMode()) {
            case PASSIVE, ON_DEMAND -> true;
            case SUPPLY_REQUIRED -> !CanisterContainerSuppliers.getFirstAvailableGasContent(player).isEmpty();
            case CONTINUOUS -> GlobalAirtightUpgradesConsumptionManager.isPowered(player, this);
        };
    }

    default boolean isEnabled(ItemStack item) {
        AirtightUpgradeStatus status = getUpgradeStatus(item);
        return status.isInstalled() && status.isEnabled();
    }

    @SuppressWarnings("unused")
    default boolean isInstalled(ItemStack item) {
        return getUpgradeStatus(item).isInstalled();
    }

    default boolean isRequesting(Player player, ItemStack item) {
        return getPowerMode() == AirtightUpgradePowerMode.CONTINUOUS && isEnabled(item) && meetsConditions(player, item) && getGasConsumptionPerSecond(player, item) >= 0;
    }

    default AirtightUpgradeStatus getUpgradeStatus(ItemStack item) {
        List<AirtightUpgradeStatus> upgradeStatuses = item.get(CCBDataComponents.AIRTIGHT_UPGRADE_STATUS);
        if (upgradeStatuses == null) {
            upgradeStatuses = AirtightItemUpgrades.getDefaultUpgradeList(item);
        }

        ResourceLocation upgradeId = getID();
        for (int statusIndex = upgradeStatuses.size() - 1; statusIndex >= 0; statusIndex--) {
            AirtightUpgradeStatus status = upgradeStatuses.get(statusIndex);
            if (!status.id().equals(upgradeId)) {
                continue;
            }

            return status;
        }
        return new AirtightUpgradeStatus(upgradeId, startsEnabled(), startsInstalled());
    }
}
