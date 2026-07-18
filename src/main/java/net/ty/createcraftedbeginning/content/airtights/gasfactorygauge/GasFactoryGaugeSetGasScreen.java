package net.ty.createcraftedbeginning.content.airtights.gasfactorygauge;

import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.IconButton;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.ty.createcraftedbeginning.data.CCBGUITextures;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasFactoryGaugeSetGasScreen extends AbstractSimiContainerScreen<GasFactoryGaugeSetGasMenu> {
    private static final CCBGUITextures BACKGROUND = CCBGUITextures.GAS_FACTORY_GAUGE_SET_GAS;

    private List<Rect2i> extraAreas = Collections.emptyList();

    public GasFactoryGaugeSetGasScreen(GasFactoryGaugeSetGasMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        setWindowSize(BACKGROUND.getWidth(), BACKGROUND.getHeight() + 108);
        super.init();
        clearWidgets();

        int x = getGuiLeft();
        int y = getGuiTop();
        IconButton confirmButton = new IconButton(x + BACKGROUND.getWidth() - 28, y + BACKGROUND.getHeight() - 25, AllIcons.I_CONFIRM);
        confirmButton.withCallback(() -> menu.player.closeContainer());
        addRenderableWidget(confirmButton);

        extraAreas = List.of(new Rect2i(x + BACKGROUND.getWidth() + 12, y + BACKGROUND.getHeight() - 30, 40, 20));
    }

    @Override
    public List<Rect2i> getExtraAreas() {
        return extraAreas;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = getGuiLeft();
        int y = getGuiTop();
        BACKGROUND.render(graphics, x + 7, y);
        renderPlayerInventory(graphics, x + 5, y + 94);

        Component title = CCBLang.translateDirect("gui.gas_factory_gauge.place_gas_to_monitor");
        graphics.drawString(font, title, x + imageWidth / 2 - font.width(title) / 2 + 7, y + 4, 0x3D3C48, false);

        GuiGameElement.of(CCBBlocks.GAS_FACTORY_GAUGE_BLOCK.asStack()).scale(3).render(graphics, x + 192, y + 48);
    }
}
