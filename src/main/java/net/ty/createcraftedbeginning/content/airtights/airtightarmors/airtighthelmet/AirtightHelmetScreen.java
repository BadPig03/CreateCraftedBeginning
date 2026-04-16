package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet;

import com.simibubi.create.foundation.gui.widget.IconButton;
import net.createmod.catnip.data.Couple;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.VerticalIndicator;
import net.ty.createcraftedbeginning.api.VerticalIndicator.State;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades.AirtightHelmetUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradableMenu;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradableScreen;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradeStatus;
import net.ty.createcraftedbeginning.data.CCBGUITextures;

@OnlyIn(Dist.CLIENT)
public class AirtightHelmetScreen extends AirtightUpgradableScreen<AirtightHelmetMenu> {
    public AirtightHelmetScreen(AirtightHelmetMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, CCBGUITextures.ARMORS);
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
        AirtightHelmetUpgradeRegistry.forEach(upgrade -> {
            Couple<Integer> offset = upgrade.getOffset();
            IconButton button = new IconButton(leftPos + offset.getFirst(), topPos + offset.getSecond(), upgrade.getIcon()).withCallback(() -> onUpgradeButtonPressed(upgrade));
            upgradeButtons.put(upgrade, button);

            boolean isRight = upgrade.isRightIndicator();
            VerticalIndicator indicator = new VerticalIndicator(leftPos + (isRight ? offset.getFirst() + 18 : offset.getFirst() - 6), topPos + offset.getSecond(), isRight);
            upgradeIndicators.put(upgrade, indicator);

            buttonConfigsMap.put(upgrade, new ScreenButtonConfig(button, upgrade.getTitle(), upgrade.getDescription(), () -> button.green, () -> !menu.getStatus(upgrade).isInstalled() && button.active, () -> upgrade.getGasCostComponent(menu.player), upgrade.getUpgradeItem()));
            addRenderableWidgets(button, indicator);
        });
    }

    @Override
    protected void updateStates() {
        ItemStack stack = menu.getMenuInventory().getStackInSlot(AirtightUpgradableMenu.UPGRADE_SLOT_INDEX);
        AirtightHelmetUpgradeRegistry.forEach(upgrade -> {
            IconButton button = upgradeButtons.get(upgrade);
            VerticalIndicator indicator = (VerticalIndicator) upgradeIndicators.get(upgrade);
            AirtightUpgradeStatus upgradeStatus = menu.getStatus(upgrade);

            button.active = upgradeStatus.isInstalled() || stack.is(upgrade.getUpgradeItem());
            button.green = upgradeStatus.isInstalled() && upgradeStatus.isEnabled();
            indicator.state = upgradeStatus.isInstalled() ? upgradeStatus.isEnabled() ? State.GREEN : State.RED : button.active ? State.YELLOW : State.OFF;
        });
        disableUpgradeButton.visible = menu.getCurrentStatusList().stream().allMatch(AirtightUpgradeStatus::isInstalled);
    }
}
