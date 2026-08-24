package net.ty.createcraftedbeginning.compat.jei.utils;

import com.simibubi.create.content.logistics.stockTicker.StockKeeperRequestScreen;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.runtime.IClickableIngredient;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class StockKeeperRequestGasGuiHandler implements IGuiContainerHandler<StockKeeperRequestScreen> {
    @Override
    public List<Rect2i> getGuiExtraAreas(StockKeeperRequestScreen screen) {
        return List.of();
    }

    @Override
    public Optional<IClickableIngredient<?>> getClickableIngredientUnderMouse(IClickableIngredientFactory factory, StockKeeperRequestScreen screen, double mouseX, double mouseY) {
        return screen.getHoveredIngredient((int) mouseX, (int) mouseY).flatMap(hovered -> {
            ItemStack hoveredStack = hovered.getFirst();
            Rect2i hoveredArea = hovered.getSecond();
            if (hoveredStack.isEmpty()) {
                return Optional.empty();
            }
            return factory.createBuilder(hoveredStack.copyWithCount(1)).buildWithArea(hoveredArea);
        });
    }
}