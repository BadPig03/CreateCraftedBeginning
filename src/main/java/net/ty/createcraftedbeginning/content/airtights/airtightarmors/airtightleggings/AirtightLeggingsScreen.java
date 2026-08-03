package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings;

import com.simibubi.create.foundation.gui.widget.IconButton;
import net.createmod.catnip.data.Couple;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.client.VerticalIndicator;
import net.ty.createcraftedbeginning.client.VerticalIndicator.State;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.upgrades.AirtightLeggingsUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradableMenu;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradableScreen;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradeStatus;
import net.ty.createcraftedbeginning.client.CCBGUITextures;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class AirtightLeggingsScreen extends AirtightUpgradableScreen<AirtightLeggingsMenu> {
    public AirtightLeggingsScreen(AirtightLeggingsMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, CCBGUITextures.ARMORS);
    }

    private static State getIndicatorState(AirtightUpgradeStatus status, boolean canInstall) {
        if (!status.isInstalled()) {
            return canInstall ? State.YELLOW : State.OFF;
        }
        return status.isEnabled() ? State.GREEN : State.RED;
    }

    @Override
    protected void init() {
        setWindowSize(Math.max(background.getWidth(), PLAYER_INVENTORY.getWidth()), background.getHeight() + 4 + PLAYER_INVENTORY.getHeight());
        setWindowOffset(0, -4);
        super.init();
    }

    @Override
    protected void initButtons() {
        super.initButtons();
        AirtightLeggingsUpgradeRegistry.forEach(upgrade -> {
            Couple<Integer> offset = upgrade.getOffset();
            IconButton button = new IconButton(leftPos + offset.getFirst(), topPos + offset.getSecond(), upgrade.getIcon()).withCallback(() -> onUpgradeButtonPressed(upgrade));
            upgradeButtons.put(upgrade, button);

            boolean rightAligned = upgrade.isRightIndicator();
            int indicatorX = leftPos + offset.getFirst() + (rightAligned ? 18 : -6);
            VerticalIndicator indicator = new VerticalIndicator(indicatorX, topPos + offset.getSecond(), rightAligned);
            upgradeIndicators.put(upgrade, indicator);

            AirtightUpgradableScreen.ScreenButtonConfig config = new AirtightUpgradableScreen.ScreenButtonConfig(button, upgrade.getTitle(), upgrade.getDescription(), () -> button.green, () -> !menu.getStatus(upgrade).isInstalled() && button.active, () -> upgrade.getComponents(menu.player, menu.contentHolder.copy()), upgrade.getUpgradeItem());
            buttonConfigsMap.put(upgrade, config);
            addRenderableWidgets(button, indicator);
        });
    }

    @Override
    protected void updateStates() {
        ItemStack stack = menu.getMenuInventory().getStackInSlot(AirtightUpgradableMenu.UPGRADE_SLOT_INDEX);
        AirtightLeggingsUpgradeRegistry.forEach(upgrade -> {
            IconButton button = upgradeButtons.get(upgrade);
            VerticalIndicator indicator = (VerticalIndicator) upgradeIndicators.get(upgrade);
            AirtightUpgradeStatus status = menu.getStatus(upgrade);

            button.active = status.isInstalled() || upgrade.testUpgradeItem(stack);
            button.green = status.isInstalled() && status.isEnabled();
            indicator.state = getIndicatorState(status, button.active);
        });
        disableUpgradeButton.visible = menu.getCurrentStatusList().stream().allMatch(AirtightUpgradeStatus::isInstalled);
    }
}
