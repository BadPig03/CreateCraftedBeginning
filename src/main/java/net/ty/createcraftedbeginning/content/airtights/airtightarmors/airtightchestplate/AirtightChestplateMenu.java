package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.AirtightChestplateUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradableMenu;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgrade;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightChestplateMenu extends AirtightUpgradableMenu {
    public AirtightChestplateMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    public AirtightChestplateMenu(MenuType<?> type, int id, Inventory inv, ItemStack contentHolder) {
        super(type, id, inv, contentHolder);
    }

    @Override
    protected boolean isValidUpgrade(ItemStack stack) {
        AirtightUpgrade upgrade = AirtightChestplateUpgradeRegistry.getByItem(stack.getItem());
        return upgrade != null && !currentStatusList.get(upgrade.getIndex()).isInstalled();
    }

    @Override
    protected @Nullable AirtightUpgrade getUpgradeById(ResourceLocation id) {
        return AirtightChestplateUpgradeRegistry.getByID(id);
    }

    @Override
    public void updateStatus(ItemStack stack) {
        currentStatusList = normalizeStatusList(stack.getOrDefault(CCBDataComponents.AIRTIGHT_UPGRADE_STATUS, AirtightChestplateUpgradeRegistry.getDefaultUpgradeList()), AirtightChestplateUpgradeRegistry.getAll());
    }
}
