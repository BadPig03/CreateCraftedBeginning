package net.ty.createcraftedbeginning.client.stockkeeper;

import com.simibubi.create.content.logistics.BigItemStack;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.airtights.gasfactorygauge.GasFactoryGaugeBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.VirtualGasItems;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasRequestAmounts;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasRequestFormat;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasFactoryPanelInteraction {
    private GasFactoryPanelInteraction() {
    }

    public static boolean renderInput(GuiGraphics graphics, Font font, int guiLeft, int guiTop, int slot, BigItemStack entry, int mouseX, int mouseY) {
        ItemStack item = entry.stack;
        if (item.isEmpty() || !VirtualGasItems.isVirtualItem(item)) {
            return false;
        }

        int x = guiLeft + 68 + slot % 3 * 20;
        int y = guiTop + 28 + slot / 3 * 20;
        int count = entry.count;
        graphics.renderItem(item, x, y);
        graphics.renderItemDecorations(font, item, x, y, GasRequestFormat.format(count, false));
        if (mouseX >= x - 2 && mouseX < x + 18 && mouseY >= y - 2 && mouseY < y + 18) {
            List<Component> tooltips = GasRequestTooltips.getFactoryAmount(item, count);
            graphics.renderComponentTooltip(font, tooltips, mouseX, mouseY);
        }
        return true;
    }

    public static boolean scroll(List<BigItemStack> inputConfig, int guiLeft, int guiTop, double mouseX, double mouseY, double scrollY, int step, boolean control) {
        for (int i = 0; i < inputConfig.size(); i++) {
            int inputX = guiLeft + 68 + i % 3 * 20;
            int inputY = guiTop + 26 + i / 3 * 20;
            if (mouseX < inputX || mouseX >= inputX + 16 || mouseY < inputY || mouseY >= inputY + 16) {
                continue;
            }

            BigItemStack entry = inputConfig.get(i);
            if (entry.stack.isEmpty() || !VirtualGasItems.isVirtualItem(entry.stack)) {
                return false;
            }

            entry.count = GasRequestAmounts.scroll(entry.count, step, scrollY, control, GasFactoryGaugeBehaviour.MAX_TARGET_AMOUNT);
            return true;
        }

        return false;
    }
}
