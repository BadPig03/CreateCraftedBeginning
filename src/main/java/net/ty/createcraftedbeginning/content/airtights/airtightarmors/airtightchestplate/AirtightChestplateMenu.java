package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightArmorMenu;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.AirtightChestplateUpgradeRegistry;
import net.ty.createcraftedbeginning.registry.CCBMenuTypes;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightChestplateMenu extends AirtightArmorMenu {
    private static final UpgradeRegistryAccess UPGRADES = upgradeRegistry(AirtightChestplateUpgradeRegistry::getByID, AirtightChestplateUpgradeRegistry::getByStack, AirtightChestplateUpgradeRegistry::getDefaultUpgradeList, AirtightChestplateUpgradeRegistry::getAll);

    public AirtightChestplateMenu(int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        this(CCBMenuTypes.AIRTIGHT_CHESTPLATE_MENU.get(), id, inv, extraData);
    }

    private AirtightChestplateMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
        super(type, id, inv, extraData, UPGRADES);
    }

    AirtightChestplateMenu(MenuType<?> type, int id, Inventory inv, ItemStack contentHolder, InteractionHand sourceHand) {
        super(type, id, inv, contentHolder, sourceHand, UPGRADES);
    }
}
