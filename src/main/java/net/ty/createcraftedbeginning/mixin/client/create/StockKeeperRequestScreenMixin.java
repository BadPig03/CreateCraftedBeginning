package net.ty.createcraftedbeginning.mixin.client.create;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.stockTicker.CraftableBigItemStack;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestMenu;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.compat.jei.category.stockkeeper.GasCraftableBigItemStack;
import net.ty.createcraftedbeginning.compat.jei.utils.StockKeeperTransferUtils;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasVirtualUtils;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasRequestUtils;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerRenderer;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(value = StockKeeperRequestScreen.class, remap = false)
public abstract class StockKeeperRequestScreenMixin extends AbstractSimiContainerScreen<StockKeeperRequestMenu> {
    @Unique
    private WeakReference<BreezeCoolerBlockEntity> ccb$breeze = new WeakReference<>(null);
    @Unique
    private boolean ccb$renderingGasVirtualItem;
    @Shadow
    private StockTickerBlockEntity blockEntity;
    @Shadow
    private WeakReference<BlazeBurnerBlockEntity> blaze;
    @Shadow
    private int windowHeight;
    @Shadow
    private List<BigItemStack> itemsToOrder;
    @Shadow
    private List<List<BigItemStack>> displayedItems;
    @Shadow
    private boolean canRequestCraftingPackage;

    private StockKeeperRequestScreenMixin(StockKeeperRequestMenu container, Inventory inv, Component title) {
        super(container, inv, title);
    }

    @Unique
    private void ccb$changeDirectGasOrder(BigItemStack entry, boolean orderClicked, boolean remove, int transfer) {
        if (transfer <= 0) {
            return;
        }

        int available = blockEntity.getLastClientsideStockSnapshotAsSummary().getCountOf(entry.stack);
        BigItemStack existingOrder = orderClicked ? entry : getOrderForItem(entry.stack);
        if (existingOrder == null) {
            if (remove || itemsToOrder.size() >= 9 || available <= 0) {
                return;
            }

            existingOrder = new BigItemStack(entry.stack.copyWithCount(1), 0);
            itemsToOrder.add(existingOrder);
        }

        int current = existingOrder.count;
        int next;
        if (remove) {
            next = current - transfer;
        }
        else {
            int addable = Math.max(0, available - current);
            if (addable == 0) {
                return;
            }

            next = current + Math.min(transfer, addable);
        }

        if (next <= 0) {
            itemsToOrder.remove(existingOrder);
            playUiSound(SoundEvents.WOOL_STEP, 0.75f, 1.8f);
            playUiSound(SoundEvents.BAMBOO_WOOD_STEP, 0.75f, 1.8f);
        }
        else {
            existingOrder.count = next;
            if (current == 0) {
                playUiSound(SoundEvents.WOOL_STEP, 0.75f, 1.2f);
                playUiSound(SoundEvents.BAMBOO_WOOD_STEP, 0.75f, 0.8f);
            }
            else {
                playUiSound(AllSoundEvents.SCROLL_VALUE.getMainEvent(), 0.25f, 1.2f);
            }
        }
    }

    @Shadow
    @Nullable
    protected abstract BigItemStack getOrderForItem(ItemStack stack);

    @Shadow
    protected abstract Couple<Integer> getHoveredSlot(int mouseX, int mouseY);

    @Shadow
    protected abstract int getMaxScroll();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ccb$init(StockKeeperRequestMenu container, Inventory inv, Component title, CallbackInfo ci) {
        if (blockEntity == null) {
            return;
        }

        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        for (Direction side : Iterate.horizontalDirections) {
            if (!(level.getBlockEntity(blockEntity.getBlockPos().relative(side)) instanceof BreezeCoolerBlockEntity breeze)) {
                continue;
            }

            ccb$breeze = new WeakReference<>(breeze);
            return;
        }
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void ccb$renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY, CallbackInfo ci) {
        if (minecraft == null || minecraft.level == null) {
            return;
        }

        BreezeCoolerBlockEntity breezeBE = ccb$breeze.get();
        if (breezeBE == null || breezeBE.isRemoved()) {
            return;
        }

        PoseStack ms = graphics.pose();
        ms.pushPose();

        BlazeBurnerBlockEntity blazeBE = blaze.get();
        if (blazeBE != null && !blazeBE.isRemoved()) {
            ms.translate(0, -64, 0);
        }

        ms.translate(getGuiLeft() - 35, getGuiTop() + windowHeight - 43, 0);
        ms.mulPose(Axis.XP.rotationDegrees(-22.5f));
        ms.mulPose(Axis.YP.rotationDegrees(-45));
        ms.scale(48, -48, 48);

        Lighting.setupForEntityInInventory();

        BlockState state = breezeBE.getBlockState();
        float horizontalAngle = AngleHelper.rad(270);
        boolean isChilled = breezeBE.getFrostLevel().isAtLeast(FrostLevel.CHILLED);
        CachedBuffers.partial(CCBPartialModels.BREEZE_COOLER_BLOCK, state).rotateCentered(horizontalAngle + Mth.PI, Direction.UP).light(LightTexture.FULL_BRIGHT).renderInto(ms, graphics.bufferSource().getBuffer(RenderType.cutoutMipped()));
        BreezeCoolerRenderer.renderShared(ms, null, graphics.bufferSource(), minecraft.level, state, breezeBE.getFrostLevelForRender(), breezeBE.getHeadAnimation().getValue(AnimationTickHolder.getPartialTicks()) * 0.175f, horizontalAngle, breezeBE.hasGoggles(), breezeBE.hasTrainHat() ? CCBPartialModels.BREEZE_TRAIN_HAT : breezeBE.isStockKeeper() ? CCBPartialModels.BREEZE_LOGISTICS_HAT : null, isChilled, isChilled ? 24 : 0, breezeBE.hashCode(), LightTexture.FULL_BRIGHT, null);

        Lighting.setupFor3DItems();

        ms.popPose();
    }

    @Redirect(method = "containerTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;closeContainer()V"))
    private void ccb$containerTick(Player player) {
        BreezeCoolerBlockEntity breeze = ccb$breeze.get();
        if (breeze != null && !breeze.isRemoved()) {
            return;
        }

        player.closeContainer();
    }

    @Inject(method = "renderItemEntry", at = @At("HEAD"))
    private void ccb$renderItemEntryHead(GuiGraphics graphics, float scale, BigItemStack entry, boolean isStackHovered, boolean isRenderingOrders, CallbackInfo ci) {
        ccb$renderingGasVirtualItem = GasVirtualUtils.isVirtualItem(entry.stack);
    }

    @Inject(method = "drawItemCount", at = @At("HEAD"), cancellable = true)
    private void ccb$drawItemCount(GuiGraphics graphics, int count, int customCount, CallbackInfo ci) {
        if (!ccb$renderingGasVirtualItem) {
            return;
        }

        String text = GasRequestUtils.format(customCount, true);
        if (text.isBlank()) {
            ci.cancel();
            return;
        }

        int textWidth = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = Character.toLowerCase(text.charAt(i));
            if (c == ',') {
                continue;
            }

            int spriteWidth;
            switch (c) {
                case ' ' -> spriteWidth = 4;
                case '.' -> spriteWidth = 3;
                case 'm' -> spriteWidth = 7;
                case '+' -> spriteWidth = 9;
                default -> spriteWidth = AllGuiTextures.NUMBERS.getWidth();
            }

            textWidth += spriteWidth;
            if (i >= text.length() - 1) {
                continue;
            }

            textWidth -= 1;
        }

        RenderSystem.enableBlend();
        int x = AllGuiTextures.NUMBERS.getWidth() - textWidth;
        for (char c : text.toCharArray()) {
            c = Character.toLowerCase(c);
            int index = c - '0';
            int xOffset = index * 6;
            int spriteWidth = AllGuiTextures.NUMBERS.getWidth();
            switch (c) {
                case ' ':
                    x += 4;
                    continue;
                case ',':
                    continue;
                case '.':
                    spriteWidth = 3;
                    xOffset = 60;
                    break;
                case 'k':
                    xOffset = 64;
                    break;
                case 'm':
                    spriteWidth = 7;
                    xOffset = 70;
                    break;
                case 'b':
                    xOffset = 78;
                    break;
                case '+':
                    spriteWidth = 9;
                    xOffset = 84;
                    break;
            }

            graphics.blit(AllGuiTextures.NUMBERS.location, 14 + x, 10, 0, AllGuiTextures.NUMBERS.getStartX() + xOffset, AllGuiTextures.NUMBERS.getStartY(), spriteWidth, AllGuiTextures.NUMBERS.getHeight(), 256, 256);
            x += spriteWidth - 1;
        }

        ci.cancel();
    }

    @Inject(method = "renderItemEntry", at = @At("RETURN"))
    private void ccb$renderItemEntryReturn(GuiGraphics graphics, float scale, BigItemStack entry, boolean isStackHovered, boolean isRenderingOrders, CallbackInfo ci) {
        ccb$renderingGasVirtualItem = false;
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void ccb$mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
        Couple<Integer> hoveredSlot = getHoveredSlot((int) mouseX, (int) mouseY);
        if (hoveredSlot.getFirst() == -1 && hoveredSlot.getSecond() == -1 || hoveredSlot.getFirst() >= 0 && !hasShiftDown() && getMaxScroll() != 0) {
            return;
        }

        boolean recipeClicked = hoveredSlot.getFirst() == -2;
        if (recipeClicked) {
            return;
        }

        boolean orderClicked = hoveredSlot.getFirst() == -1;
        BigItemStack entry = orderClicked ? itemsToOrder.get(hoveredSlot.getSecond()) : displayedItems.get(hoveredSlot.getFirst()).get(hoveredSlot.getSecond());
        if (!GasVirtualUtils.isVirtualItem(entry.stack)) {
            return;
        }

        int step = GasRequestUtils.getStep(hasAltDown(), hasControlDown(), hasShiftDown()) * (orderClicked ? 1 : 10);
        int transfer = Mth.ceil(Math.abs(scrollY) * step);
        if (transfer <= 0) {
            cir.setReturnValue(true);
            return;
        }

        boolean remove = scrollY < 0;
        ccb$changeDirectGasOrder(entry, orderClicked, remove, transfer);
        cir.setReturnValue(true);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void ccb$mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != 0 && button != 1) {
            return;
        }

        Couple<Integer> hoveredSlot = getHoveredSlot((int) mouseX, (int) mouseY);
        if (hoveredSlot.getFirst() == -1 && hoveredSlot.getSecond() == -1 || hoveredSlot.getFirst() == -2) {
            return;
        }

        boolean orderClicked = hoveredSlot.getFirst() == -1;
        BigItemStack entry = orderClicked ? itemsToOrder.get(hoveredSlot.getSecond()) : displayedItems.get(hoveredSlot.getFirst()).get(hoveredSlot.getSecond());
        if (!GasVirtualUtils.isVirtualItem(entry.stack)) {
            return;
        }

        boolean remove = button == 1;
        int transfer = GasRequestUtils.getStep(hasAltDown(), hasControlDown(), hasShiftDown()) * (orderClicked ? 1 : 10);
        ccb$changeDirectGasOrder(entry, orderClicked, remove, transfer);
        cir.setReturnValue(true);
    }

    @Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true)
    private void ccb$renderTooltip(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
        Couple<Integer> hoveredSlot = getHoveredSlot(mouseX, mouseY);
        if (hoveredSlot.getFirst() == -1 && hoveredSlot.getSecond() == -1 || hoveredSlot.getFirst() == -2) {
            return;
        }

        boolean orderClicked = hoveredSlot.getFirst() == -1;
        BigItemStack entry;
        if (orderClicked) {
            int index = hoveredSlot.getSecond();
            if (index < 0 || index >= itemsToOrder.size()) {
                return;
            }

            entry = itemsToOrder.get(index);
        }
        else {
            int row = hoveredSlot.getFirst();
            int column = hoveredSlot.getSecond();
            if (row < 0 || row >= displayedItems.size()) {
                return;
            }

            List<BigItemStack> rowItems = displayedItems.get(row);
            if (column < 0 || column >= rowItems.size()) {
                return;
            }

            entry = rowItems.get(column);
        }

        if (!GasVirtualUtils.isVirtualItem(entry.stack)) {
            return;
        }

        List<Component> tooltip = new ArrayList<>();
        int available = blockEntity.getLastClientsideStockSnapshotAsSummary().getCountOf(entry.stack);
        tooltip.add(CCBLang.translate("gui.tooltips.gas_virtual_item.send_item", CCBLang.itemName(entry.stack).add(CCBLang.text(" x" + GasRequestUtils.formatPrecise(available)))).color(ScrollInput.HEADER_RGB).component());
        tooltip.add(CCBLang.translate("gui.tooltips.gas_virtual_item.scroll", GasRequestUtils.getScrollStep()).style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component());
        tooltip.add(CCBLang.translate("gui.tooltips.gas_virtual_item.shift_to_scroll", GasRequestUtils.getShiftStep()).style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component());
        tooltip.add(CCBLang.translate("gui.tooltips.gas_virtual_item.alt_to_scroll", GasRequestUtils.getAltStep()).style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component());
        tooltip.add(CCBLang.translate("gui.tooltips.gas_virtual_item.ctrl_to_scroll", GasRequestUtils.getCtrlStep()).style(ChatFormatting.DARK_GRAY).style(ChatFormatting.ITALIC).component());
        graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        ci.cancel();
    }

    @Inject(method = "requestCraftable", at = @At("HEAD"), cancellable = true)
    private void ccb$requestCraftable(CraftableBigItemStack craftable, int requestedDifference, CallbackInfo ci) {
        if (!(craftable instanceof GasCraftableBigItemStack gasCraftable)) {
            return;
        }

        StockKeeperTransferUtils.requestCraftable(this, gasCraftable, requestedDifference);
        ci.cancel();
    }

    @Inject(method = "updateCraftableAmounts", at = @At("HEAD"), cancellable = true)
    private void ccb$updateCraftableAmounts(CallbackInfo ci) {
        if (!StockKeeperTransferUtils.hasGasCraftable(this)) {
            return;
        }

        StockKeeperTransferUtils.updateCraftableAmounts(this);
        canRequestCraftingPackage = true;
        ci.cancel();
    }
}
