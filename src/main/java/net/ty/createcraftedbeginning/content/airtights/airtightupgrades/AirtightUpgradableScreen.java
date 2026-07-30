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
import net.ty.createcraftedbeginning.client.CCBGUITextures;
import net.ty.createcraftedbeginning.data.CCBIcons;
import net.ty.createcraftedbeginning.data.CCBLang;
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
    protected static final Component OPTION_ENABLED = CCBLang.translateDirect("gui.option_enabled");
    protected static final Component OPTION_DISABLED = CCBLang.translateDirect("gui.option_disabled");
    protected static final Component UPGRADE_NOT_INSTALLED = CCBLang.translateDirect("gui.upgrade_not_installed");
    protected static final Component UPGRADE_CAN_BE_INSTALLED = CCBLang.translateDirect("gui.upgrade_can_be_installed");
    protected static final Component UPGRADE_FULL = CCBLang.translateDirect("gui.upgrade_full");

    protected final CCBGUITextures background;
    protected final Map<AirtightUpgrade, IconButton> upgradeButtons = new HashMap<>();
    protected final Map<AirtightUpgrade, AbstractSimiWidget> upgradeIndicators = new HashMap<>();
    protected final Map<AirtightUpgrade, ScreenButtonConfig> buttonConfigsMap = new HashMap<>();
    private final Set<ResourceLocation> pendingUpgradeRequests = new HashSet<>();

    protected IconButton disableUpgradeButton;
    private int lastServerStateRevision;

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
        if (!menu.stillValid(player)) {
            player.closeContainer();
        }
        super.containerTick();
        int revision = menu.getServerStateRevision();
        if (revision != lastServerStateRevision) {
            pendingUpgradeRequests.clear();
            lastServerStateRevision = revision;
        }
        updateStates();
    }

    @Override
    protected void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.renderForeground(guiGraphics, mouseX, mouseY, partialTicks);
        for (ScreenButtonConfig config : buttonConfigsMap.values()) {
            if (!config.getIconButton().isHovered()) {
                continue;
            }

            renderButtonTooltip(guiGraphics, config, mouseX, mouseY);
        }
        renderForeground(guiGraphics, mouseX, mouseY);
    }

    @Override
    public List<Rect2i> getExtraAreas() {
        return ImmutableList.of(new Rect2i(leftPos + 2 + background.getWidth(), topPos + background.getHeight() - 48, 48, 48));
    }

    private void renderButtonTooltip(GuiGraphics guiGraphics, ScreenButtonConfig config, int mouseX, int mouseY) {
        IconButton button = config.getIconButton();
        List<Component> tooltips = new ArrayList<>(List.of(config.getTitle()));
        Item upgradeItem = config.getUpgradeItem();
        if (!button.isActive() && upgradeItem != null) {
            Component notInstalled = UPGRADE_NOT_INSTALLED.plainCopy().append(upgradeItem.getDescription()).withStyle(ChatFormatting.RED);
            tooltips.add(notInstalled);
        }

        if (config.canBeInstalled()) {
            tooltips.add(UPGRADE_CAN_BE_INSTALLED.plainCopy().withStyle(ChatFormatting.GOLD));
        }
        else if (button.isActive()) {
            boolean isEnabled = config.isEnabled();
            Component option = isEnabled ? OPTION_ENABLED : OPTION_DISABLED;
            ChatFormatting color = isEnabled ? ChatFormatting.DARK_GREEN : ChatFormatting.RED;
            tooltips.add(option.plainCopy().withStyle(color));
        }

        boolean isShiftDown = hasShiftDown();
        Component shiftKey = CCBLang.translateDirect("gui.key.shift").withStyle(isShiftDown ? ChatFormatting.WHITE : ChatFormatting.GRAY);
        tooltips.add(CCBLang.translateDirect("gui.hold_for_description", shiftKey).withStyle(ChatFormatting.DARK_GRAY));
        if (isShiftDown) {
            tooltips.addAll(TooltipHelper.cutTextComponent(config.getDescription(), Palette.ALL_GRAY));
            List<Component> components = config.getComponents();
            if (!components.isEmpty()) {
                tooltips.add(CommonComponents.EMPTY);
                tooltips.add(CCBLang.translateDirect("gui.gas_consumption").withStyle(ChatFormatting.GRAY));
                for (Component component : components) {
                    tooltips.add(component.plainCopy().withStyle(ChatFormatting.GRAY));
                }
            }
        }

        guiGraphics.renderTooltip(font, tooltips, Optional.empty(), mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int inventoryX = getLeftOfCentered(PLAYER_INVENTORY.getWidth()) + 1;
        int inventoryY = topPos + background.getHeight() + 4;
        renderPlayerInventory(guiGraphics, inventoryX, inventoryY);
        background.render(guiGraphics, leftPos + 2, topPos);

        Component hoverName = menu.contentHolder.getHoverName();
        int titleX = leftPos + (background.getWidth() - 8) / 2 - font.width(hoverName) / 2 + 2;
        guiGraphics.drawString(font, hoverName, titleX, topPos + 4, 0xFFFFFF, false);

        GuiGameElement.of(menu.contentHolder).scale(4).at(leftPos + background.getWidth() + 2, topPos + background.getHeight() - 48, -200).render(guiGraphics);
    }

    protected void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (hoveredSlot == null || hoveredSlot.hasItem() || hoveredSlot.getMaxStackSize() != 1) {
            return;
        }

        int slot = hoveredSlot.getSlotIndex();
        if (slot != AirtightUpgradableMenu.UPGRADE_SLOT_INDEX) {
            return;
        }

        Component title = disableUpgradeButton.visible ? UPGRADE_FULL : UPGRADE_SLOT_TITLE;
        guiGraphics.renderTooltip(font, title.plainCopy().withStyle(ChatFormatting.GRAY), mouseX, mouseY);
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
        ResourceLocation id = upgrade.getID();
        if (!pendingUpgradeRequests.add(id)) {
            return;
        }

        AirtightUpgradeStatus status = menu.getStatus(upgrade);
        if (status.isInstalled()) {
            CatnipServices.NETWORK.sendToServer(new AirtightUpgradePacket(id, false));
            return;
        }

        ItemStack stack = menu.getMenuInventory().getStackInSlot(AirtightUpgradableMenu.UPGRADE_SLOT_INDEX);
        if (upgrade.testUpgradeItem(stack)) {
            CatnipServices.NETWORK.sendToServer(new AirtightUpgradePacket(id, true));
            return;
        }

        pendingUpgradeRequests.remove(id);
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
            return enabledSupplier.get();
        }

        public boolean canBeInstalled() {
            return installableSupplier.get();
        }

        public List<Component> getComponents() {
            return componentsSupplier.get();
        }

        @Nullable
        public Item getUpgradeItem() {
            return upgradeItem;
        }
    }
}
