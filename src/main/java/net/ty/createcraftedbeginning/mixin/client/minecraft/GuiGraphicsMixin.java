package net.ty.createcraftedbeginning.mixin.client.minecraft;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import net.createmod.catnip.data.Couple;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.client.stockkeeper.GasRequestTooltips;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.VirtualGasItems;
import net.ty.createcraftedbeginning.platform.access.client.StockKeeperRequestScreenAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V", at = @At("HEAD"), cancellable = true)
    private void ccb$renderTooltip(Font font, ItemStack stack, int mouseX, int mouseY, CallbackInfo callback) {
        if (!(Minecraft.getInstance().screen instanceof StockKeeperRequestScreen requestScreen) || !(requestScreen instanceof StockKeeperRequestScreenAccess accessor)) {
            return;
        }

        Couple<Integer> hoveredSlot = accessor.ccb$getHoveredSlot(mouseX, mouseY);
        int row = hoveredSlot.getFirst();
        int column = hoveredSlot.getSecond();
        if (row == -1 && column == -1 || row == -2) {
            return;
        }

        boolean orderHovered = row == -1;
        BigItemStack entry = null;
        if (orderHovered) {
            if (column >= 0 && column < requestScreen.itemsToOrder.size()) {
                entry = requestScreen.itemsToOrder.get(column);
            }
        }
        else {
            if (row < 0 || row >= requestScreen.displayedItems.size()) {
                return;
            }

            List<BigItemStack> rowItems = requestScreen.displayedItems.get(row);
            if (column < 0 || column >= rowItems.size()) {
                return;
            }

            entry = rowItems.get(column);
        }
        if (entry == null) {
            return;
        }

        if (!VirtualGasItems.isVirtualItem(entry.stack) || !ItemStack.isSameItemSameComponents(stack, entry.stack)) {
            return;
        }

        List<Component> tooltip = GasRequestTooltips.getTooltipLines(requestScreen, entry, orderHovered);
        GuiGraphics guiGraphics = (GuiGraphics) (Object) this;
        guiGraphics.renderComponentTooltip(font, tooltip, mouseX, mouseY, stack);
        callback.cancel();
    }
}
