package net.ty.createcraftedbeginning.compat.jei.ghost;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.AirtightHandheldDrillGhostItemSubmitPacket;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.AirtightHandheldDrillMenu;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.AirtightHandheldDrillScreen;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightHandheldDrillGhostIngredientHandler implements IGhostIngredientHandler<AirtightHandheldDrillScreen> {
    private static final int PLAYER_INVENTORY_SLOTS = Inventory.INVENTORY_SIZE;

    @Override
    public <I> @NotNull List<Target<I>> getTargetsTyped(AirtightHandheldDrillScreen screen, ITypedIngredient<I> ingredient, boolean doStart) {
        List<Target<I>> targets = new ArrayList<>();
        if (ingredient.getType() != VanillaTypes.ITEM_STACK) {
            return targets;
        }

        for (int i = PLAYER_INVENTORY_SLOTS; i < screen.getMenu().slots.size(); i++) {
            Slot slot = screen.getMenu().slots.get(i);
            if (!slot.isActive() || slot.getSlotIndex() == AirtightHandheldDrillMenu.UPGRADE_SLOT_INDEX) {
                continue;
            }

            targets.add(new GhostTarget<>(screen, i - PLAYER_INVENTORY_SLOTS));
        }
        return targets;
    }

    @Override
    public void onComplete() {
    }

    private static class GhostTarget<I> implements Target<I> {
        private final Rect2i area;
        private final AirtightHandheldDrillScreen screen;
        private final int slotIndex;

        public GhostTarget(AirtightHandheldDrillScreen screen, int slotIndex) {
            this.screen = screen;
            this.slotIndex = slotIndex;
            Slot menuSlot = screen.getMenu().slots.get(slotIndex + PLAYER_INVENTORY_SLOTS);
            area = new Rect2i(screen.getGuiLeft() + menuSlot.x, screen.getGuiTop() + menuSlot.y, 16, 16);
        }

        @Override
        public Rect2i getArea() {
            return area;
        }

        @Override
        public void accept(I ingredient) {
            if (slotIndex == AirtightHandheldDrillMenu.UPGRADE_SLOT_INDEX || !(ingredient instanceof ItemStack itemStack)) {
                return;
            }

            ItemStack submittedStack = itemStack.copyWithCount(1);
            screen.getMenu().getMenuInventory().setStackInSlot(slotIndex, submittedStack);
            CatnipServices.NETWORK.sendToServer(new AirtightHandheldDrillGhostItemSubmitPacket(submittedStack));
        }
    }
}
