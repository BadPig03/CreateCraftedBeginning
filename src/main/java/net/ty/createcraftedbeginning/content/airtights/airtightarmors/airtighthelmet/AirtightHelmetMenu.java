package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades.AirtightHelmetUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradableMenu;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgrade;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightHelmetMenu extends AirtightUpgradableMenu {
    public AirtightHelmetMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        super(type, id, inv, extraData);
    }

    public AirtightHelmetMenu(MenuType<?> type, int id, Inventory inv, ItemStack contentHolder, InteractionHand sourceHand) {
        super(type, id, inv, contentHolder, sourceHand);
    }

    @Override
    protected @Nullable AirtightUpgrade getUpgradeById(ResourceLocation id) {
        return AirtightHelmetUpgradeRegistry.getByID(id);
    }

    @Override
    protected boolean isValidUpgrade(ItemStack stack) {
        AirtightUpgrade upgrade = AirtightHelmetUpgradeRegistry.getByStack(stack);
        return upgrade != null && !getStatus(upgrade).isInstalled();
    }

    @Override
    public void updateStatus(ItemStack stack) {
        currentStatusList = normalizeStatusList(stack.getOrDefault(CCBDataComponents.AIRTIGHT_UPGRADE_STATUS, AirtightHelmetUpgradeRegistry.getDefaultUpgradeList()), AirtightHelmetUpgradeRegistry.getAll());
    }
}
