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
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.foundation.client.CCBGUITextures;
import net.ty.createcraftedbeginning.foundation.gui.CCBIcons;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public abstract class AirtightUpgradableScreen<T extends AirtightUpgradableMenu> extends AbstractSimiContainerScreen<T> {
    protected static final AllGuiTextures PLAYER_INVENTORY = AllGuiTextures.PLAYER_INVENTORY;
    protected static final Component UPGRADE_SLOT_TITLE = CCBLang.translateDirect("gui.upgrade_slot");
    protected static final Component UPGRADE_FULL = CCBLang.translateDirect("gui.upgrade_full");
    private static final Component OPTION_ENABLED = CCBLang.translateDirect("gui.option_enabled");
    private static final Component OPTION_DISABLED = CCBLang.translateDirect("gui.option_disabled");
    private static final Component UPGRADE_NOT_INSTALLED = CCBLang.translateDirect("gui.upgrade_not_installed");
    private static final Component UPGRADE_CAN_BE_INSTALLED = CCBLang.translateDirect("gui.upgrade_can_be_installed");

    protected final CCBGUITextures background;
    protected final Map<AirtightUpgrade, IconButton> upgradeButtons = new HashMap<>();
    protected final Map<AirtightUpgrade, AbstractSimiWidget> upgradeIndicators = new HashMap<>();
    protected final Map<AirtightUpgrade, ScreenButtonConfig> buttonConfigsMap = new HashMap<>();
    private final Set<ResourceLocation> pendingUpgradeRequests = new HashSet<>();

    protected IconButton disableUpgradeButton;
    private int lastServerStateRevision;

    protected AirtightUpgradableScreen(T container, Inventory inv, Component title, CCBGUITextures background) {
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
        if (!menu.stillValid(player)) {
            player.closeContainer();
        }
        super.containerTick();
        int serverStateRevision = menu.getServerStateRevision();
        if (serverStateRevision != lastServerStateRevision) {
            pendingUpgradeRequests.clear();
            lastServerStateRevision = serverStateRevision;
        }
        updateStates();
    }

    @Override
    protected void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.renderForeground(guiGraphics, mouseX, mouseY, partialTicks);
        for (ScreenButtonConfig buttonConfig : buttonConfigsMap.values()) {
            if (!buttonConfig.getIconButton().isHovered()) {
                continue;
            }

            renderButtonTooltip(guiGraphics, buttonConfig, mouseX, mouseY);
        }
        renderForeground(guiGraphics, mouseX, mouseY);
    }

    @Override
    public List<Rect2i> getExtraAreas() {
        return ImmutableList.of(new Rect2i(leftPos + 2 + background.getWidth(), topPos + background.getHeight() - 48, 48, 48));
    }

    private void renderButtonTooltip(GuiGraphics guiGraphics, ScreenButtonConfig buttonConfig, int mouseX, int mouseY) {
        IconButton upgradeButton = buttonConfig.getIconButton();
        List<Component> tooltipLines = new ArrayList<>(List.of(buttonConfig.getTitle()));
        Item upgradeItem = buttonConfig.getUpgradeItem();
        if (!upgradeButton.isActive() && upgradeItem != null) {
            tooltipLines.add(UPGRADE_NOT_INSTALLED.plainCopy().append(upgradeItem.getDescription()).withStyle(ChatFormatting.RED));
        }

        if (buttonConfig.canBeInstalled()) {
            tooltipLines.add(UPGRADE_CAN_BE_INSTALLED.plainCopy().withStyle(ChatFormatting.GOLD));
        }
        else if (upgradeButton.isActive()) {
            boolean isEnabled = buttonConfig.isEnabled();
            Component statusLabel = isEnabled ? OPTION_ENABLED : OPTION_DISABLED;
            ChatFormatting statusColor = isEnabled ? ChatFormatting.DARK_GREEN : ChatFormatting.RED;
            tooltipLines.add(statusLabel.plainCopy().withStyle(statusColor));
        }

        boolean isShiftDown = hasShiftDown();
        Component shiftKey = CCBLang.translateDirect("gui.key.shift").withStyle(isShiftDown ? ChatFormatting.WHITE : ChatFormatting.GRAY);
        tooltipLines.add(CCBLang.translateDirect("gui.hold_for_description", shiftKey).withStyle(ChatFormatting.DARK_GRAY));
        if (isShiftDown) {
            tooltipLines.addAll(TooltipHelper.cutTextComponent(buttonConfig.getDescription(), Palette.ALL_GRAY));
            List<Component> gasConsumptionLines = buttonConfig.getComponents();
            if (!gasConsumptionLines.isEmpty()) {
                tooltipLines.add(CommonComponents.EMPTY);
                tooltipLines.add(CCBLang.translateDirect("gui.gas_consumption").withStyle(ChatFormatting.GRAY));
                for (Component gasConsumptionLine : gasConsumptionLines) {
                    tooltipLines.add(gasConsumptionLine.plainCopy().withStyle(ChatFormatting.GRAY));
                }
            }
        }

        guiGraphics.renderTooltip(font, tooltipLines, Optional.empty(), mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int inventoryX = getLeftOfCentered(PLAYER_INVENTORY.getWidth()) + 1;
        int inventoryY = topPos + background.getHeight() + 4;
        renderPlayerInventory(guiGraphics, inventoryX, inventoryY);
        background.render(guiGraphics, leftPos + 2, topPos);

        Component itemName = menu.contentHolder.getHoverName();
        int titleX = leftPos + (background.getWidth() - 8) / 2 - font.width(itemName) / 2 + 2;
        guiGraphics.drawString(font, itemName, titleX, topPos + 4, 0xFFFFFF, false);

        GuiGameElement.of(menu.contentHolder).scale(4).at(leftPos + background.getWidth() + 2, topPos + background.getHeight() - 48, -200).render(guiGraphics);
    }

    protected void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (hoveredSlot == null || hoveredSlot.hasItem() || hoveredSlot.getMaxStackSize() != 1 || hoveredSlot.getSlotIndex() != AirtightUpgradableMenu.UPGRADE_SLOT_INDEX) {
            return;
        }

        Component slotTitle = disableUpgradeButton.visible ? UPGRADE_FULL : UPGRADE_SLOT_TITLE;
        guiGraphics.renderTooltip(font, slotTitle.plainCopy().withStyle(ChatFormatting.GRAY), mouseX, mouseY);
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
        ResourceLocation upgradeId = upgrade.getID();
        if (!pendingUpgradeRequests.add(upgradeId)) {
            return;
        }

        if (menu.getStatus(upgrade).isInstalled()) {
            CatnipServices.NETWORK.sendToServer(new AirtightUpgradePacket(upgradeId, false));
            return;
        }

        ItemStack upgradeStack = menu.getMenuInventory().getStackInSlot(AirtightUpgradableMenu.UPGRADE_SLOT_INDEX);
        if (upgrade.testUpgradeItem(upgradeStack)) {
            CatnipServices.NETWORK.sendToServer(new AirtightUpgradePacket(upgradeId, true));
            return;
        }

        pendingUpgradeRequests.remove(upgradeId);
    }

    protected abstract void updateStates();

    protected static class ScreenButtonConfig {
        private final IconButton iconButton;
        private final Component title;
        private final Component description;
        private final Supplier<Boolean> enabledSupplier;
        private final Supplier<Boolean> installableSupplier;
        private final Supplier<List<Component>> componentsSupplier;
        @Nullable
        private final Item upgradeItem;

        public ScreenButtonConfig(IconButton iconButton, Component title, Component description, Supplier<Boolean> enabledSupplier, Supplier<Boolean> installableSupplier, Supplier<List<Component>> componentsSupplier, @Nullable Item upgradeItem) {
            this.iconButton = iconButton;
            this.title = title;
            this.description = description;
            this.enabledSupplier = enabledSupplier;
            this.installableSupplier = installableSupplier;
            this.componentsSupplier = componentsSupplier;
            this.upgradeItem = upgradeItem;
        }

        private IconButton getIconButton() {
            return iconButton;
        }

        private Component getTitle() {
            return title;
        }

        private Component getDescription() {
            return description;
        }

        private boolean isEnabled() {
            return enabledSupplier.get();
        }

        private boolean canBeInstalled() {
            return installableSupplier.get();
        }

        private List<Component> getComponents() {
            return componentsSupplier.get();
        }

        @Nullable
        private Item getUpgradeItem() {
            return upgradeItem;
        }
    }
}
