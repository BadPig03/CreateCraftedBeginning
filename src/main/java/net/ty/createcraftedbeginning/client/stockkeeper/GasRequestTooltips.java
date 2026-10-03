package net.ty.createcraftedbeginning.client.stockkeeper;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.VirtualGasItems;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasRequestFormat;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.gas.visual.GasUnitFormat;
import net.ty.createcraftedbeginning.platform.client.RequestScreenBridge;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class GasRequestTooltips {
    private GasRequestTooltips() {
    }

    public static List<Component> getTooltipLines(StockKeeperRequestScreen screen, BigItemStack entry, boolean orderHovered) {
        List<Component> tooltipLines = new ArrayList<>();
        ItemStack virtualItem = entry.stack;
        tooltipLines.add(CCBLang.itemName(virtualItem).component());

        int availableAmount = RequestScreenBridge.getAvailableAmount(screen, virtualItem);
        if (orderHovered) {
            BigItemStack requestedOrderItem = RequestScreenBridge.getRequestedOrder(screen, virtualItem);
            if (requestedOrderItem != null && requestedOrderItem.count > 0) {
                tooltipLines.add(CCBLang.translate("gui.gas_virtual_item.requested", GasRequestFormat.formatPrecise(requestedOrderItem.count)).style(ChatFormatting.DARK_GRAY).component());
            }
        }
        else {
            tooltipLines.add(CCBLang.translate("gui.gas_virtual_item.available", GasRequestFormat.formatPrecise(availableAmount)).style(ChatFormatting.DARK_GRAY).component());
        }

        long scrollMultiplier = orderHovered ? 1 : 10;
        addScrollTooltip(tooltipLines, "gui.gas_virtual_item.scroll", GasRequestSteps.getScrollStep() * scrollMultiplier);
        addScrollTooltip(tooltipLines, "gui.gas_virtual_item.shift_to_scroll", GasRequestSteps.getShiftStep() * scrollMultiplier);
        addScrollTooltip(tooltipLines, "gui.gas_virtual_item.alt_to_scroll", GasRequestSteps.getAltStep() * scrollMultiplier);
        addScrollTooltip(tooltipLines, "gui.gas_virtual_item.ctrl_to_scroll", GasRequestSteps.getCtrlStep() * scrollMultiplier);
        tooltipLines.addAll(getExtraTooltips(virtualItem));
        return tooltipLines;
    }

    public static List<Component> getExtraTooltips(ItemStack virtualItem) {
        if (!VirtualGasItems.isVirtualItem(virtualItem)) {
            return List.of();
        }

        GasStack gas = VirtualGasItems.readGasSample(virtualItem);
        List<Component> tooltipLines = new ArrayList<>(gas.getGasType().getTooltip(gas));
        if (Minecraft.getInstance().options.advancedItemTooltips) {
            String gasId = gas.getGasType().getResourceLocation().toString();
            tooltipLines.add(CCBLang.text(gasId).style(ChatFormatting.DARK_GRAY).component());
        }
        tooltipLines.add(CCBLang.text(CCBAPI.NAME).style(ChatFormatting.BLUE).style(ChatFormatting.ITALIC).component());
        return tooltipLines;
    }

    public static List<Component> getRequesterAmount(ItemStack item, int amount) {
        return getConfiguredAmount(item, amount, "gui.gas_virtual_item.send_item");
    }

    public static List<Component> getFactoryAmount(ItemStack item, int amount) {
        List<Component> lines = getConfiguredAmount(item, amount, "gui.gas_factory_gauge.sending_item");
        lines.add(CCBLang.translate("gui.gas_factory_gauge.left_click_disconnect").style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component());
        return lines;
    }

    private static List<Component> getConfiguredAmount(ItemStack item, int amount, String key) {
        List<Component> lines = new ArrayList<>();
        lines.add(CCBLang.translate(key, CCBLang.itemName(item).add(CCBLang.text(" x" + GasRequestFormat.formatPrecise(amount)))).color(ScrollInput.HEADER_RGB).component());
        addScrollTooltip(lines, "gui.gas_virtual_item.scroll", GasRequestSteps.getScrollStep());
        addScrollTooltip(lines, "gui.gas_virtual_item.shift_to_scroll", GasRequestSteps.getShiftStep());
        addScrollTooltip(lines, "gui.gas_virtual_item.alt_to_scroll", GasRequestSteps.getAltStep());
        addScrollTooltip(lines, "gui.gas_virtual_item.ctrl_to_scroll", GasRequestSteps.getCtrlStep());
        return lines;
    }

    private static void addScrollTooltip(List<Component> tooltipLines, String key, long amount) {
        tooltipLines.add(CCBLang.translate(key, GasUnitFormat.format(amount)).style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component());
    }
}
