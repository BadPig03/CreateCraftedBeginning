package net.ty.createcraftedbeginning.compat.jei.ghost;

import com.simibubi.create.compat.jei.GhostIngredientHandler;
import com.simibubi.create.foundation.gui.menu.GhostItemSubmitPacket;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.compat.jei.CCBJEIPlugin;
import net.ty.createcraftedbeginning.content.airtights.gasfactorygauge.GasFactoryGaugeSetGasMenu;
import net.ty.createcraftedbeginning.content.airtights.gasfactorygauge.GasFactoryGaugeSetGasScreen;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.VirtualGasItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasFactoryGaugeGhostIngredientHandler implements IGhostIngredientHandler<GasFactoryGaugeSetGasScreen> {
    private final GhostIngredientHandler<GasFactoryGaugeSetGasMenu> itemHandler = new GhostIngredientHandler<>();

    @Override
    public <I> List<Target<I>> getTargetsTyped(GasFactoryGaugeSetGasScreen screen, ITypedIngredient<I> ingredient, boolean doStart) {
        if (ingredient.getType() != CCBJEIPlugin.GAS_STACK) {
            return itemHandler.getTargetsTyped(screen, ingredient, doStart);
        }

        if (!(ingredient.getIngredient() instanceof GasStack gas) || gas.isEmpty()) {
            return List.of();
        }

        Slot slot = screen.getMenu().slots.get(Inventory.INVENTORY_SIZE);
        Rect2i area = new Rect2i(screen.getGuiLeft() + slot.x, screen.getGuiTop() + slot.y, 16, 16);
        return List.of(new GasTarget<>(screen, area));
    }

    @Override
    public void onComplete() {
    }

    private record GasTarget<I>(GasFactoryGaugeSetGasScreen screen, Rect2i area) implements Target<I> {
        @Override
        public Rect2i getArea() {
            return area;
        }

        @Override
        public void accept(I ingredient) {
            if (!(ingredient instanceof GasStack gas) || gas.isEmpty()) {
                return;
            }

            ItemStack sample = VirtualGasItems.createVirtualItem(gas);
            screen.getMenu().ghostInventory.setStackInSlot(0, sample);
            CatnipServices.NETWORK.sendToServer(new GhostItemSubmitPacket(sample, 0));
        }
    }
}
