package net.ty.createcraftedbeginning.mixin.client.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.stockTicker.CraftableBigItemStack;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestMenu;
import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.client.stockkeeper.GasRequestSteps;
import net.ty.createcraftedbeginning.client.stockkeeper.StockKeeperCrafting;
import net.ty.createcraftedbeginning.client.stockkeeper.StockKeeperRequestRendering;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.VirtualGasItems;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasCraftableBigItemStack;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasOrderChanges;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasOrderChanges.Change;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
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

    @Shadow
    @Nullable
    protected abstract BigItemStack getOrderForItem(ItemStack stack);

    @Shadow
    protected abstract Couple<Integer> getHoveredSlot(int mouseX, int mouseY);

    @Shadow
    protected abstract int getMaxScroll();

    @Unique
    private void ccb$changeDirectGasOrder(BigItemStack entry, boolean orderClicked, boolean remove, int transfer) {
        if (transfer <= 0) {
            return;
        }

        int available = blockEntity.getLastClientsideStockSnapshotAsSummary().getCountOf(entry.stack);
        BigItemStack order = orderClicked ? entry : getOrderForItem(entry.stack);
        Change change = GasOrderChanges.apply(itemsToOrder, order, entry.stack, available, remove, transfer, hasControlDown());
        switch (change) {
            case ADDED -> {
                playUiSound(SoundEvents.WOOL_STEP, 0.75F, 1.2F);
                playUiSound(SoundEvents.BAMBOO_WOOD_STEP, 0.75F, 0.8F);
            }
            case REMOVED -> {
                playUiSound(SoundEvents.WOOL_STEP, 0.75F, 1.8F);
                playUiSound(SoundEvents.BAMBOO_WOOD_STEP, 0.75F, 1.8F);
            }
            case UPDATED -> playUiSound(AllSoundEvents.SCROLL_VALUE.getMainEvent(), 0.25F, 1.2F);
        }
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void ccb$init(StockKeeperRequestMenu container, Inventory inv, Component title, CallbackInfo callback) {
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
    private void ccb$renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY, CallbackInfo callback) {
        if (minecraft == null || minecraft.level == null) {
            return;
        }

        BreezeCoolerBlockEntity breeze = ccb$breeze.get();
        if (breeze == null || breeze.isRemoved()) {
            return;
        }

        BlazeBurnerBlockEntity burner = blaze.get();
        StockKeeperRequestRendering.renderBreeze(graphics, minecraft.level, breeze, burner != null && !burner.isRemoved(), getGuiLeft(), getGuiTop(), windowHeight);
    }

    @WrapOperation(method = "containerTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;closeContainer()V"))
    private void ccb$containerTick(Player player, Operation<Void> original) {
        BreezeCoolerBlockEntity breeze = ccb$breeze.get();
        if (breeze != null && !breeze.isRemoved()) {
            return;
        }

        original.call(player);
    }

    @Inject(method = "renderItemEntry", at = @At("HEAD"))
    private void ccb$renderItemEntryHead(GuiGraphics graphics, float scale, BigItemStack entry, boolean isStackHovered, boolean isRenderingOrders, CallbackInfo callback) {
        ccb$renderingGasVirtualItem = VirtualGasItems.isVirtualItem(entry.stack);
    }

    @Inject(method = "drawItemCount", at = @At("HEAD"), cancellable = true)
    private void ccb$drawItemCount(GuiGraphics graphics, int count, int customCount, CallbackInfo callback) {
        if (!ccb$renderingGasVirtualItem) {
            return;
        }

        StockKeeperRequestRendering.drawGasCount(graphics, customCount);
        callback.cancel();
    }

    @Inject(method = "renderItemEntry", at = @At("RETURN"))
    private void ccb$renderItemEntryReturn(GuiGraphics graphics, float scale, BigItemStack entry, boolean isStackHovered, boolean isRenderingOrders, CallbackInfo callback) {
        ccb$renderingGasVirtualItem = false;
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void ccb$mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> callback) {
        Couple<Integer> hoveredSlot = getHoveredSlot((int) mouseX, (int) mouseY);
        int row = hoveredSlot.getFirst();
        int column = hoveredSlot.getSecond();
        if (row == -1 && column == -1 || row >= 0 && !hasShiftDown() && getMaxScroll() != 0) {
            return;
        }

        boolean recipeClicked = row == -2;
        if (recipeClicked) {
            return;
        }

        boolean orderClicked = row == -1;
        BigItemStack entry = orderClicked ? itemsToOrder.get(column) : displayedItems.get(row).get(column);
        if (!VirtualGasItems.isVirtualItem(entry.stack)) {
            return;
        }

        int step = GasRequestSteps.getStep(hasAltDown(), hasControlDown(), hasShiftDown()) * (orderClicked ? 1 : 10);
        int transfer = Mth.ceil(Math.abs(scrollY) * step);
        if (transfer <= 0) {
            callback.setReturnValue(true);
            return;
        }

        ccb$changeDirectGasOrder(entry, orderClicked, scrollY < 0, transfer);
        callback.setReturnValue(true);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void ccb$mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> callback) {
        if (button != 0 && button != 1) {
            return;
        }

        Couple<Integer> hoveredSlot = getHoveredSlot((int) mouseX, (int) mouseY);
        int row = hoveredSlot.getFirst();
        int column = hoveredSlot.getSecond();
        if (row == -1 && column == -1 || row == -2) {
            return;
        }

        boolean orderClicked = row == -1;
        BigItemStack entry = orderClicked ? itemsToOrder.get(column) : displayedItems.get(row).get(column);
        if (!VirtualGasItems.isVirtualItem(entry.stack)) {
            return;
        }

        boolean remove = orderClicked || button == 1;
        int step = GasRequestSteps.getStep(hasAltDown(), hasControlDown(), hasShiftDown()) * (orderClicked ? 1 : 10);
        ccb$changeDirectGasOrder(entry, orderClicked, remove, step);
        callback.setReturnValue(true);
    }

    @Inject(method = "requestCraftable", at = @At("HEAD"), cancellable = true)
    private void ccb$requestCraftable(CraftableBigItemStack craftable, int requestedDifference, CallbackInfo callback) {
        if (!(craftable instanceof GasCraftableBigItemStack gasCraftable)) {
            return;
        }

        StockKeeperCrafting.requestCraftable(this, gasCraftable, requestedDifference);
        callback.cancel();
    }

    @Inject(method = "updateCraftableAmounts", at = @At("HEAD"), cancellable = true)
    private void ccb$updateCraftableAmounts(CallbackInfo callback) {
        if (!StockKeeperCrafting.hasGasCraftable(this)) {
            return;
        }

        StockKeeperCrafting.updateCraftableAmounts(this);
        canRequestCraftingPackage = true;
        callback.cancel();
    }
}
