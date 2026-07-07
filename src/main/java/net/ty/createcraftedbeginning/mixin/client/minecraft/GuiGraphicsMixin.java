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
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasVirtualUtils;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasRequestClientUtils;
import net.ty.createcraftedbeginning.mixin.client.accessor.StockKeeperRequestScreenAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @Unique
    private static boolean ccb$renderingGasTooltip;

    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V", at = @At("HEAD"), cancellable = true)
    private void ccb$renderTooltip(Font font, ItemStack stack, int mouseX, int mouseY, CallbackInfo ci) {
        if (ccb$renderingGasTooltip) {
            return;
        }

        if (!(Minecraft.getInstance().screen instanceof StockKeeperRequestScreen requestScreen) || !(requestScreen instanceof StockKeeperRequestScreenAccessor accessor)) {
            return;
        }

        Couple<Integer> hoveredSlot = accessor.ccb$getHoveredSlot(mouseX, mouseY);
        if (hoveredSlot.getFirst() == -1 && hoveredSlot.getSecond() == -1 || hoveredSlot.getFirst() == -2) {
            return;
        }

        boolean orderHovered = hoveredSlot.getFirst() == -1;
        BigItemStack entry = null;
        if (orderHovered) {
            int index = hoveredSlot.getSecond();
            if (index >= 0 && index < requestScreen.itemsToOrder.size()) {
                entry = requestScreen.itemsToOrder.get(index);
            }
        }
        else {
            int row = hoveredSlot.getFirst();
            int column = hoveredSlot.getSecond();
            if (row >= 0 && row < requestScreen.displayedItems.size()) {
                List<BigItemStack> rowItems = requestScreen.displayedItems.get(row);
                if (column >= 0 && column < rowItems.size()) {
                    entry = rowItems.get(column);
                }
            }
        }
        if (entry == null) {
            return;
        }

        if (!GasVirtualUtils.isVirtualItem(entry.stack) || !ItemStack.isSameItemSameComponents(stack, entry.stack)) {
            return;
        }

        List<Component> tooltip = GasRequestClientUtils.getTooltipLines(accessor, entry, orderHovered);
        ccb$renderingGasTooltip = true;
        try {
            ((GuiGraphics) (Object) this).renderComponentTooltip(font, tooltip, mouseX, mouseY);
        } finally {
            ccb$renderingGasTooltip = false;
        }

        ci.cancel();
    }
}
