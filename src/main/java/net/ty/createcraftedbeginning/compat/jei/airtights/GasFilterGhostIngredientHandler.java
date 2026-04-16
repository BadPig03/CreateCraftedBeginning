package net.ty.createcraftedbeginning.compat.jei.airtights;

import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.compat.jei.JEIPlugin;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasFilterScreen;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasVirtualItem;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

public class GasFilterGhostIngredientHandler implements IGhostIngredientHandler<GasFilterScreen> {
    private static final int PLAYER_INVENTORY_SLOTS = Inventory.INVENTORY_SIZE;

    @Override
    public <I> @NotNull List<Target<I>> getTargetsTyped(@NotNull GasFilterScreen gui, @NotNull ITypedIngredient<I> ingredient, boolean doStart) {
        List<Target<I>> targets = new LinkedList<>();
        if (ingredient.getType() != JEIPlugin.GAS_STACK) {
            return targets;
        }

        for (int i = PLAYER_INVENTORY_SLOTS; i < gui.getMenu().slots.size(); i++) {
            targets.add(new GhostTarget<>(gui, i - PLAYER_INVENTORY_SLOTS));
        }
        return targets;
    }

    @Override
    public void onComplete() {
    }

    private static class GhostTarget<I> implements Target<I> {
        private final Rect2i area;
        private final GasFilterScreen gui;

        public GhostTarget(@NotNull GasFilterScreen gui, int slotIndex) {
            this.gui = gui;
            Slot slot = gui.getMenu().slots.get(slotIndex + PLAYER_INVENTORY_SLOTS);
            area = new Rect2i(gui.getGuiLeft() + slot.x, gui.getGuiTop() + slot.y, 16, 16);
        }

        @Override
        public @NotNull Rect2i getArea() {
            return area;
        }

        @Override
        public void accept(@NotNull I ingredient) {
            if (!(ingredient instanceof GasStack gasStack)) {
                return;
            }

            ItemStack virtualItem = GasVirtualItem.getVirtualItem(gasStack);
            gui.getMenu().insertDirectly(List.of(virtualItem));
            CatnipServices.NETWORK.sendToServer(new GasFilterGhostItemSubmitPacket(virtualItem));
        }
    }
}
