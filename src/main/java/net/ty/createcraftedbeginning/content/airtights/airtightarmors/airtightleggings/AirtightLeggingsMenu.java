package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightArmorMenu;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.upgrades.AirtightLeggingsUpgradeRegistry;
import net.ty.createcraftedbeginning.registry.CCBMenuTypes;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightLeggingsMenu extends AirtightArmorMenu {
    private static final UpgradeRegistryAccess UPGRADES = upgradeRegistry(AirtightLeggingsUpgradeRegistry::getByID, AirtightLeggingsUpgradeRegistry::getByStack, AirtightLeggingsUpgradeRegistry::getDefaultUpgradeList, AirtightLeggingsUpgradeRegistry::getAll);

    public AirtightLeggingsMenu(int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        this(CCBMenuTypes.AIRTIGHT_LEGGINGS_MENU.get(), id, inv, extraData);
    }

    private AirtightLeggingsMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        super(type, id, inv, extraData, UPGRADES);
    }

    AirtightLeggingsMenu(MenuType<?> type, int id, Inventory inv, ItemStack contentHolder, InteractionHand sourceHand) {
        super(type, id, inv, contentHolder, sourceHand, UPGRADES);
    }
}
