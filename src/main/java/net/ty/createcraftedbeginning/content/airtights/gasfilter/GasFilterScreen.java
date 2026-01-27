package net.ty.createcraftedbeginning.content.airtights.gasfilter;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.item.TooltipHelper;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.data.CCBGUITextures;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GasFilterScreen extends AbstractSimiContainerScreen<GasFilterMenu> {
    private static final CCBGUITextures BACKGROUND = CCBGUITextures.GAS_FILTER;
    private static final AllGuiTextures PLAYER_INVENTORY = AllGuiTextures.PLAYER_INVENTORY;
    private static final ItemStack FILTER = new ItemStack(CCBItems.GAS_FILTER.asItem());
    private static final Component BLACKLIST_TITLE = CCBLang.translateDirect("gui.gas_filter.blacklist");
    private static final Component BLACKLIST_DESCRIPTION = CCBLang.translateDirect("gui.gas_filter.blacklist.description");
    private static final Component WHITELIST_TITLE = CCBLang.translateDirect("gui.gas_filter.whitelist");
    private static final Component WHITELIST_DESCRIPTION = CCBLang.translateDirect("gui.gas_filter.whitelist.description");

    private final ItemStack filter;
    private IconButton whitelistButton;
    private IconButton blacklistButton;

    public GasFilterScreen(GasFilterMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        filter = menu.player.getMainHandItem();
    }

    @Override
    protected void init() {
        setWindowSize(Math.max(BACKGROUND.getWidth(), PLAYER_INVENTORY.getWidth()), BACKGROUND.getHeight() + 4 + PLAYER_INVENTORY.getHeight());
        setWindowOffset(-11, 8);
        super.init();
        clearWidgets();
        initButtons();
    }

    @Override
    protected void containerTick() {
        Player player = menu.player;
        if (!ItemStack.isSameItem(player.getMainHandItem(), menu.contentHolder)) {
            player.closeContainer();
        }
        super.containerTick();
        updateStates();
        renderTooltips();
    }

    @Override
    public List<Rect2i> getExtraAreas() {
        return ImmutableList.of(new Rect2i(leftPos + BACKGROUND.getWidth(), topPos + BACKGROUND.getHeight() - 40, 64, 48));
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        renderPlayerInventory(graphics, getLeftOfCentered(PLAYER_INVENTORY.getWidth()), topPos + BACKGROUND.getHeight() - 1);
        BACKGROUND.render(graphics, leftPos, topPos - 5);
        Component filterHoverName = filter.getHoverName();
        graphics.drawString(font, filterHoverName, leftPos + (BACKGROUND.getWidth() - 8) / 2 - font.width(filterHoverName) / 2, topPos - 1, 0xFFFFFF, false);
        GuiGameElement.of(FILTER).scale(4).at(leftPos + BACKGROUND.getWidth() + 8, topPos + BACKGROUND.getHeight() - 53, -200).render(graphics);
    }

    private void initButtons() {
        IconButton confirmButton = new IconButton(leftPos + BACKGROUND.getWidth() - 33, topPos + BACKGROUND.getHeight() - 29, AllIcons.I_CONFIRM).withCallback(() -> menu.player.closeContainer());
        addRenderableWidget(confirmButton);

        blacklistButton = new IconButton(leftPos + 18, topPos + 75, AllIcons.I_BLACKLIST).withCallback(() -> {
            menu.blacklist = true;
            CatnipServices.NETWORK.sendToServer(new GasFilterScreenPacket(true));
        });
        whitelistButton = new IconButton(leftPos + 36, topPos + 75, AllIcons.I_WHITELIST).withCallback(() -> {
            menu.blacklist = false;
            CatnipServices.NETWORK.sendToServer(new GasFilterScreenPacket(false));
        });
        addRenderableWidgets(blacklistButton, whitelistButton);

        IconButton resetButton = new IconButton(leftPos + BACKGROUND.getWidth() - 62, topPos + BACKGROUND.getHeight() - 29, AllIcons.I_TRASH).withCallback(() -> {
			menu.clearContents();
			menu.sendClearPacket();
		});
        addRenderableWidgets(resetButton);
    }

    private void updateStates() {
        blacklistButton.green = menu.blacklist;
        whitelistButton.green = !menu.blacklist;
    }

    private void renderTooltips() {
        boolean hasShiftDown = hasShiftDown();
        if (blacklistButton.isHoveredOrFocused()) {
            blacklistButton.setToolTip(BLACKLIST_TITLE);
            List<Component> blacklistButtonToolTip = blacklistButton.getToolTip();
            blacklistButtonToolTip.add(CCBLang.translateDirect("gui.hold_for_description", CCBLang.translateDirect("gui.key.shift").withStyle(hasShiftDown ? ChatFormatting.WHITE : ChatFormatting.GRAY)).withStyle(ChatFormatting.DARK_GRAY));
            if (hasShiftDown) {
                blacklistButtonToolTip.addAll(TooltipHelper.cutTextComponent(BLACKLIST_DESCRIPTION, Palette.ALL_GRAY));
            }
        }

        if (whitelistButton.isHoveredOrFocused()) {
            whitelistButton.setToolTip(WHITELIST_TITLE);
            List<Component> whitelistButtonToolTip = whitelistButton.getToolTip();
            whitelistButtonToolTip.add(CCBLang.translateDirect("gui.hold_for_description", CCBLang.translateDirect("gui.key.shift").withStyle(hasShiftDown ? ChatFormatting.WHITE : ChatFormatting.GRAY)).withStyle(ChatFormatting.DARK_GRAY));
            if (hasShiftDown) {
                whitelistButtonToolTip.addAll(TooltipHelper.cutTextComponent(WHITELIST_DESCRIPTION, Palette.ALL_GRAY));
            }
        }
    }
}
