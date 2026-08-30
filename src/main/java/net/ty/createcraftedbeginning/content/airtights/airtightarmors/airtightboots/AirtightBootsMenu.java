package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightArmorMenu;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades.AirtightBootsUpgradeRegistry;
import net.ty.createcraftedbeginning.registry.CCBMenuTypes;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightBootsMenu extends AirtightArmorMenu {
    private static final UpgradeRegistryAccess UPGRADES = upgradeRegistry(AirtightBootsUpgradeRegistry::getById, AirtightBootsUpgradeRegistry::getByStack, AirtightBootsUpgradeRegistry::getDefaultUpgradeList, AirtightBootsUpgradeRegistry::getAll);

    public AirtightBootsMenu(int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        this(CCBMenuTypes.AIRTIGHT_BOOTS_MENU.get(), id, inv, extraData);
    }

    private AirtightBootsMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        super(type, id, inv, extraData, UPGRADES);
    }

    AirtightBootsMenu(MenuType<?> type, int id, Inventory inv, ItemStack contentHolder, InteractionHand sourceHand) {
        super(type, id, inv, contentHolder, sourceHand, UPGRADES);
    }
}
