package net.ty.createcraftedbeginning.content.airtights.airtightupgrades;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.item.TooltipHelper;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.data.CCBGUITextures;
import net.ty.createcraftedbeginning.data.CCBIcons;
import net.ty.createcraftedbeginning.data.CCBLang;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public abstract class AirtightUpgradableScreen<T extends AirtightUpgradableMenu> extends AbstractSimiContainerScreen<T> {
    protected static final AllGuiTextures PLAYER_INVENTORY = AllGuiTextures.PLAYER_INVENTORY;

    protected static final int PLAYER_INVENTORY_SLOTS = Inventory.INVENTORY_SIZE;
    protected static final Component UPGRADE_SLOT_TITLE = CCBLang.translateDirect("gui.upgrade_slot");
    protected static final Component OPTION_ENABLED = CCBLang.translateDirect("gui.option_enabled");
    protected static final Component OPTION_DISABLED = CCBLang.translateDirect("gui.option_disabled");
    protected static final Component UPGRADE_NOT_INSTALLED = CCBLang.translateDirect("gui.upgrade_not_installed");
    protected static final Component UPGRADE_CAN_BE_INSTALLED = CCBLang.translateDirect("gui.upgrade_can_be_installed");
    protected static final Component UPGRADE_FULL = CCBLang.translateDirect("gui.upgrade_full");

    protected final CCBGUITextures background;
    protected final Map<AirtightUpgrade, IconButton> upgradeButtons = new HashMap<>();
    protected final Map<AirtightUpgrade, AbstractSimiWidget> upgradeIndicators = new HashMap<>();
    protected final Map<AirtightUpgrade, ScreenButtonConfig> buttonConfigsMap = new HashMap<>();

    protected IconButton disableUpgradeButton;

    public AirtightUpgradableScreen(T container, Inventory inv, Component title, CCBGUITextures background) {
        super(container, inv, title);
        this.background = background;
    }

    protected static boolean isMouseOverSlot(int mouseX, int mouseY, int x, int y) {
        return mouseX >= x && mouseY >= y && mouseX < x + 18 && mouseY < y + 18;
    }

    @Override
    protected void init() {
        super.init();
        clearWidgets();
        initWidgets();
        initButtons();
        updateStates();
    }

    @Override
    protected void containerTick() {
        Player player = menu.player;
        if (!ItemStack.isSameItem(player.getMainHandItem(), menu.contentHolder)) {
            player.closeContainer();
        }
        super.containerTick();
        updateStates();
    }

    @Override
    protected void renderForeground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.renderForeground(guiGraphics, mouseX, mouseY, partialTicks);
        for (ScreenButtonConfig buttonConfig : buttonConfigsMap.values()) {
            IconButton button = buttonConfig.getIconButton();
            if (!button.isHovered()) {
                continue;
            }

            List<Component> tooltips = new ArrayList<>(List.of(buttonConfig.getTitle()));
            Item upgradeItem = buttonConfig.getUpgradeItem();
            if (!button.isActive() && upgradeItem != null) {
                tooltips.add(UPGRADE_NOT_INSTALLED.plainCopy().append(upgradeItem.getDescription()).withStyle(ChatFormatting.RED));
            }

            if (buttonConfig.canBeInstalled()) {
                tooltips.add(UPGRADE_CAN_BE_INSTALLED.plainCopy().withStyle(ChatFormatting.GOLD));
            }
            else if (button.isActive()) {
                boolean isEnabled = buttonConfig.isEnabled();
                tooltips.add((isEnabled ? OPTION_ENABLED : OPTION_DISABLED).plainCopy().withStyle(isEnabled ? ChatFormatting.DARK_GREEN : ChatFormatting.RED));
            }

            boolean hasShiftDown = hasShiftDown();
            tooltips.add(CCBLang.translateDirect("gui.hold_for_description", CCBLang.translateDirect("gui.key.shift").withStyle(hasShiftDown ? ChatFormatting.WHITE : ChatFormatting.GRAY)).withStyle(ChatFormatting.DARK_GRAY));
            if (hasShiftDown) {
                tooltips.addAll(TooltipHelper.cutTextComponent(buttonConfig.getDescription(), Palette.ALL_GRAY));

                Component gasCost = buttonConfig.getGasCostComponent();
                if (gasCost != null) {
                    tooltips.add(CommonComponents.EMPTY);
                    tooltips.add(CCBLang.translateDirect("gui.gas_cost").withStyle(ChatFormatting.GRAY));
                    tooltips.add(gasCost.plainCopy().withStyle(ChatFormatting.GRAY));
                }
            }
            guiGraphics.renderTooltip(font, tooltips, Optional.empty(), mouseX, mouseY);
        }
        renderForeground(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        renderPlayerInventory(guiGraphics, getLeftOfCentered(PLAYER_INVENTORY.getWidth()) + 1, topPos + background.getHeight() + 4);
        background.render(guiGraphics, leftPos + 2, topPos);
        Component hoverName = menu.contentHolder.getHoverName();
        guiGraphics.drawString(font, hoverName, leftPos + (background.getWidth() - 8) / 2 - font.width(hoverName) / 2 + 2, topPos + 4, 0xFFFFFF, false);
        GuiGameElement.of(menu.contentHolder).scale(4).at(leftPos + background.getWidth() + 2, topPos + background.getHeight() - 48, -200).render(guiGraphics);
    }

    @Override
    public List<Rect2i> getExtraAreas() {
        return ImmutableList.of(new Rect2i(leftPos + 2 + background.getWidth(), topPos + background.getHeight() - 48, 48, 48));
    }

    protected void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (hoveredSlot == null || hoveredSlot.hasItem() || hoveredSlot.getMaxStackSize() != 1) {
            return;
        }

        int slot = hoveredSlot.getSlotIndex();
        if (slot != AirtightUpgradableMenu.UPGRADE_SLOT_INDEX) {
            return;
        }

        guiGraphics.renderTooltip(font, disableUpgradeButton.visible ? UPGRADE_FULL.plainCopy().withStyle(ChatFormatting.GRAY) : UPGRADE_SLOT_TITLE.plainCopy().withStyle(ChatFormatting.GRAY), mouseX, mouseY);
    }

    protected void initWidgets() {
    }

    protected void initButtons() {
        upgradeButtons.clear();
        upgradeIndicators.clear();
        buttonConfigsMap.clear();

        IconButton confirmButton = new IconButton(leftPos + background.getWidth() - 31, topPos + background.getHeight() - 24, AllIcons.I_CONFIRM).withCallback(() -> menu.player.closeContainer());
        addRenderableWidget(confirmButton);

        disableUpgradeButton = new IconButton(leftPos + 84, topPos + 76, CCBIcons.I_FINISHED).setActive(false);
        disableUpgradeButton.visible = false;
        addRenderableWidget(disableUpgradeButton);
    }

    protected void onUpgradeButtonPressed(AirtightUpgrade upgrade) {
        AirtightUpgradeStatus upgradeStatus = menu.getStatus(upgrade);
        if (upgradeStatus.isInstalled()) {
            menu.toggleUpgrade(upgrade);
            CatnipServices.NETWORK.sendToServer(new AirtightUpgradePacket(upgrade.getID(), false, ItemStack.EMPTY));
        } else if (menu.getMenuInventory().getStackInSlot(AirtightUpgradableMenu.UPGRADE_SLOT_INDEX).is(upgrade.getUpgradeItem())) {
            menu.installUpgrade(upgrade);
            ItemStack upgradeItemInSlot = menu.getMenuInventory().getStackInSlot(AirtightUpgradableMenu.UPGRADE_SLOT_INDEX).copy();
            CatnipServices.NETWORK.sendToServer(new AirtightUpgradePacket(upgrade.getID(), true, upgradeItemInSlot));
        }
    }

    protected abstract void updateStates();

    protected static class ScreenButtonConfig {
        private final IconButton iconButton;
        private final Component title;
        private final Component description;
        private final Supplier<Boolean> isEnabled;
        private final Supplier<Boolean> canBeInstalled;
        private final Supplier<Component> gasCostComponent;
        @Nullable
        private final Item upgradeItem;

        public ScreenButtonConfig(IconButton iconButton, Component title, Component description, Supplier<Boolean> isEnabled, Supplier<Boolean> canBeInstalled, Supplier<Component> gasCostComponent, @Nullable Item upgradeItem) {
            this.iconButton = iconButton;
            this.title = title;
            this.description = description;
            this.isEnabled = isEnabled;
            this.canBeInstalled = canBeInstalled;
            this.gasCostComponent = gasCostComponent;
            this.upgradeItem = upgradeItem;
        }

        public IconButton getIconButton() {
            return iconButton;
        }

        public Component getTitle() {
            return title;
        }

        public Component getDescription() {
            return description;
        }

        public boolean isEnabled() {
            return isEnabled.get();
        }

        public boolean canBeInstalled() {
            return canBeInstalled.get();
        }

        @Nullable
        public Component getGasCostComponent() {
            return gasCostComponent.get();
        }

        @Nullable
        public Item getUpgradeItem() {
            return upgradeItem;
        }
    }
}
