package net.ty.createcraftedbeginning.content.airtights.gascanisterpack;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.gas.cansiters.GasCanisterQueryUtils;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackMenu.PackItemHandler;
import net.ty.createcraftedbeginning.data.CCBGUITextures;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class GasCanisterPackScreen extends AbstractSimiContainerScreen<GasCanisterPackMenu> {
    private ItemStack pack;

    private static final CCBGUITextures BACKGROUND = CCBGUITextures.GAS_CANISTER_PACK;
    private static final CCBGUITextures CANISTER = CCBGUITextures.GAS_CANISTER_PACK_CANISTER;
    private static final AllGuiTextures PLAYER_INVENTORY = AllGuiTextures.PLAYER_INVENTORY;

    private List<Rect2i> extraAreas = Collections.emptyList();

    public GasCanisterPackScreen(GasCanisterPackMenu container, Inventory inv, Component title) {
        super(container, inv, title);
        pack = menu.player.getMainHandItem();
    }

    @Override
    protected void init() {
        setWindowSize(Math.max(BACKGROUND.getWidth(), PLAYER_INVENTORY.getWidth()), BACKGROUND.getHeight() + 4 + PLAYER_INVENTORY.getHeight());
        setWindowOffset(-2, -4);
        super.init();
        clearWidgets();
        initButtons();
    }

    @Override
    protected void containerTick() {
        Player player = menu.player;
        ItemStack newPack = player.getMainHandItem();
        if (!ItemStack.isSameItem(newPack, menu.contentHolder)) {
            player.closeContainer();
        }
        if (!ItemStack.isSameItemSameComponents(newPack, pack)) {
            pack = newPack;
        }
        super.containerTick();
    }

    @Override
    public List<Rect2i> getExtraAreas() {
        return extraAreas;
    }

    @Override
	public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        drawGasCanisters(graphics);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int invX = getLeftOfCentered(PLAYER_INVENTORY.getWidth());
        int invY = topPos + BACKGROUND.getHeight() + 4;
        renderPlayerInventory(guiGraphics, invX, invY);

        int x = leftPos;
        int y = topPos;
        int width = BACKGROUND.getWidth();
        BACKGROUND.render(guiGraphics, x, y);
        Component drillHoverName = pack.getHoverName();
        guiGraphics.drawString(font, drillHoverName, x + (width - 8) / 2 - font.width(drillHoverName) / 2, y + 4, 0xFFFFFF, false);
        GuiGameElement.of(pack).scale(4).at(x + width + 11, y + BACKGROUND.getHeight() - 48, -200).render(guiGraphics);
    }

    private void initButtons() {
        int x = leftPos;
        int y = topPos;
        int width = BACKGROUND.getWidth();
        int height = BACKGROUND.getHeight();

        IconButton confirmButton = new IconButton(x + width - 33, y + height - 24, AllIcons.I_CONFIRM).withCallback(() -> menu.player.closeContainer());
        addRenderableWidget(confirmButton);
        extraAreas = ImmutableList.of(new Rect2i(x + width, y + height - 32, 64, 48));
    }

    private void drawGasCanisters(GuiGraphics graphics) {
        int x = leftPos;
        int y = topPos + 27;
        PackItemHandler packInventory = menu.packInventory;
        if (GasCanisterQueryUtils.isValidCanister(packInventory.getStackInSlot(GasCanisterPackMenu.I_SLOT_INDEX))) {
            CANISTER.render(graphics, x + 23, y);
        }
        if (GasCanisterQueryUtils.isValidCanister(packInventory.getStackInSlot(GasCanisterPackMenu.II_SLOT_INDEX))) {
            CANISTER.render(graphics, x + 65, y);
        }
        if (GasCanisterQueryUtils.isValidCanister(packInventory.getStackInSlot(GasCanisterPackMenu.III_SLOT_INDEX))) {
            CANISTER.render(graphics, x + 107, y);
        }
        if (GasCanisterQueryUtils.isValidCanister(packInventory.getStackInSlot(GasCanisterPackMenu.IV_SLOT_INDEX))) {
            CANISTER.render(graphics, x + 149, y);
        }
    }
}
