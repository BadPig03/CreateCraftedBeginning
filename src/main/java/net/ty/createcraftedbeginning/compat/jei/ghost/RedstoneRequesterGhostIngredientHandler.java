package net.ty.createcraftedbeginning.compat.jei.ghost;

import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterScreen;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.client.stockkeeper.GasRequestSteps;
import net.ty.createcraftedbeginning.client.stockkeeper.RedstoneRequesterInteraction;
import net.ty.createcraftedbeginning.compat.jei.CCBJEIPlugin;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.VirtualGasItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RedstoneRequesterGhostIngredientHandler implements IGhostIngredientHandler<RedstoneRequesterScreen> {
    private static final int PLAYER_INVENTORY_SLOTS = Inventory.INVENTORY_SIZE;

    @Override
    public <I> List<Target<I>> getTargetsTyped(RedstoneRequesterScreen screen, ITypedIngredient<I> ingredient, boolean doStart) {
        List<Target<I>> targets = new ArrayList<>();
        if (ingredient.getType() != CCBJEIPlugin.GAS_STACK) {
            return targets;
        }

        for (int i = PLAYER_INVENTORY_SLOTS; i < screen.getMenu().slots.size(); i++) {
            targets.add(new GhostTarget<>(screen, i - PLAYER_INVENTORY_SLOTS));
        }
        return targets;
    }

    @Override
    public void onComplete() {
    }

    private static class GhostTarget<I> implements Target<I> {
        private final Rect2i area;
        private final int slotIndex;
        private final RedstoneRequesterScreen screen;

        public GhostTarget(RedstoneRequesterScreen screen, int slotIndex) {
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
            if (!(ingredient instanceof GasStack gasStack)) {
                return;
            }

            RedstoneRequesterInteraction.submitVirtualItem(screen, screen.getMenu(), VirtualGasItems.createVirtualItem(gasStack), slotIndex, GasRequestSteps.getScrollStep());
        }
    }
}
