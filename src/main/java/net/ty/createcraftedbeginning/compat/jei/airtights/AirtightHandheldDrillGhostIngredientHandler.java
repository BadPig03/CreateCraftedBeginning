package net.ty.createcraftedbeginning.compat.jei.airtights;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.AirtightHandheldDrillMenu;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.AirtightHandheldDrillScreen;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

public class AirtightHandheldDrillGhostIngredientHandler implements IGhostIngredientHandler<AirtightHandheldDrillScreen> {
    private static final int PLAYER_INVENTORY_SLOTS = Inventory.INVENTORY_SIZE;

    @Override
    public <I> @NotNull List<Target<I>> getTargetsTyped(@NotNull AirtightHandheldDrillScreen gui, @NotNull ITypedIngredient<I> ingredient, boolean doStart) {
        List<Target<I>> targets = new LinkedList<>();
        if (ingredient.getType() != VanillaTypes.ITEM_STACK) {
            return targets;
        }

        for (int i = PLAYER_INVENTORY_SLOTS; i < gui.getMenu().slots.size(); i++) {
            Slot slot = gui.getMenu().slots.get(i);
            if (!slot.isActive() || slot.getSlotIndex() == AirtightHandheldDrillMenu.UPGRADE_SLOT_INDEX) {
                continue;
            }

            targets.add(new GhostTarget<>(gui, i - PLAYER_INVENTORY_SLOTS));
        }
        return targets;
    }

    @Override
    public void onComplete() {
    }

    private static class GhostTarget<I> implements Target<I> {
        private final Rect2i area;
        private final AirtightHandheldDrillScreen gui;
        private final int slotIndex;

        public GhostTarget(@NotNull AirtightHandheldDrillScreen gui, int slotIndex) {
            this.gui = gui;
            this.slotIndex = slotIndex;
            Slot slot = gui.getMenu().slots.get(slotIndex + PLAYER_INVENTORY_SLOTS);
            area = new Rect2i(gui.getGuiLeft() + slot.x, gui.getGuiTop() + slot.y, 16, 16);
        }

        @Override
        public @NotNull Rect2i getArea() {
            return area;
        }

        @Override
        public void accept(@NotNull I ingredient) {
            if (slotIndex == AirtightHandheldDrillMenu.UPGRADE_SLOT_INDEX || !(ingredient instanceof ItemStack itemStack)) {
                return;
            }

            ItemStack stack = itemStack.copyWithCount(1);
            gui.getMenu().getMenuInventory().setStackInSlot(slotIndex, stack);
            CatnipServices.NETWORK.sendToServer(new AirtightHandheldDrillGhostItemSubmitPacket(stack));
        }
    }
}
