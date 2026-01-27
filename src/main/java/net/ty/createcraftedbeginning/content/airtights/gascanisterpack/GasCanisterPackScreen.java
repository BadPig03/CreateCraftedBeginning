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
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackMenu.PackItemHandler;
import net.ty.createcraftedbeginning.data.CCBGUITextures;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class GasCanisterPackScreen extends AbstractSimiContainerScreen<GasCanisterPackMenu> {
    private ItemStack pack;

    private static final CCBGUITextures BACKGROUND = CCBGUITextures.GAS_CANISTER_PACK;
    private static final CCBGUITextures CANISTER = CCBGUITextures.GAS_CANISTER_PACK_CANISTER;
    private static final AllGuiTextures PLAYER_INVENTORY = AllGuiTextures.PLAYER_INVENTORY;

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
        return ImmutableList.of(new Rect2i(leftPos + BACKGROUND.getWidth(), topPos + BACKGROUND.getHeight() - 32, 64, 48));
    }

    @Override
	public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        drawGasCanisters(graphics);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        renderPlayerInventory(graphics, getLeftOfCentered(PLAYER_INVENTORY.getWidth()), topPos + BACKGROUND.getHeight() + 4);
        BACKGROUND.render(graphics, leftPos, topPos);
        Component drillHoverName = pack.getHoverName();
        graphics.drawString(font, drillHoverName, leftPos + (BACKGROUND.getWidth() - 8) / 2 - font.width(drillHoverName) / 2, topPos + 4, 0xFFFFFF, false);
        GuiGameElement.of(pack).scale(4).at(leftPos + BACKGROUND.getWidth() + 11, topPos + BACKGROUND.getHeight() - 48, -200).render(graphics);
    }

    private void initButtons() {
        IconButton confirmButton = new IconButton(leftPos + BACKGROUND.getWidth() - 33, topPos + BACKGROUND.getHeight() - 24, AllIcons.I_CONFIRM).withCallback(() -> menu.player.closeContainer());
        addRenderableWidget(confirmButton);
    }

    private void drawGasCanisters(GuiGraphics graphics) {
        PackItemHandler packInventory = menu.packInventory;
        if (CanisterContainerSuppliers.isValidGasCanister(packInventory.getStackInSlot(GasCanisterPackMenu.I_SLOT_INDEX))) {
            CANISTER.render(graphics, leftPos + 23, topPos + 27);
        }
        if (CanisterContainerSuppliers.isValidGasCanister(packInventory.getStackInSlot(GasCanisterPackMenu.II_SLOT_INDEX))) {
            CANISTER.render(graphics, leftPos + 65, topPos + 27);
        }
        if (CanisterContainerSuppliers.isValidGasCanister(packInventory.getStackInSlot(GasCanisterPackMenu.III_SLOT_INDEX))) {
            CANISTER.render(graphics, leftPos + 107, topPos + 27);
        }
        if (CanisterContainerSuppliers.isValidGasCanister(packInventory.getStackInSlot(GasCanisterPackMenu.IV_SLOT_INDEX))) {
            CANISTER.render(graphics, leftPos + 149, topPos + 27);
        }
    }
}
