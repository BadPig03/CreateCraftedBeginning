package net.ty.createcraftedbeginning.compat.jei.utils;

import com.simibubi.create.content.logistics.redstoneRequester.RedstoneRequesterScreen;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.compat.jei.CCBJEIPlugin;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasVirtualUtils;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasRequestClientUtils;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasRequestUtils;
import net.ty.createcraftedbeginning.mixin.client.accessor.RedstoneRequesterScreenAccessor;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RedstoneRequesterGhostIngredientHandler implements IGhostIngredientHandler<RedstoneRequesterScreen> {
    private static final int PLAYER_INVENTORY_SLOTS = Inventory.INVENTORY_SIZE;

    @Override
    public <I> List<Target<I>> getTargetsTyped(RedstoneRequesterScreen gui, ITypedIngredient<I> ingredient, boolean doStart) {
        List<Target<I>> targets = new LinkedList<>();
        if (ingredient.getType() != CCBJEIPlugin.GAS_STACK) {
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
        private final int slotIndex;
        private final RedstoneRequesterScreen gui;

        public GhostTarget(RedstoneRequesterScreen gui, int slotIndex) {
            this.gui = gui;
            this.slotIndex = slotIndex;
            Slot slot = gui.getMenu().slots.get(slotIndex + PLAYER_INVENTORY_SLOTS);
            area = new Rect2i(gui.getGuiLeft() + slot.x, gui.getGuiTop() + slot.y, 16, 16);
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

            GasRequestClientUtils.submitVirtualItem((RedstoneRequesterScreenAccessor) gui, gui.getMenu(), GasVirtualUtils.createVirtualItem(gasStack), slotIndex, GasRequestUtils.getScrollStep());
        }
    }
}
