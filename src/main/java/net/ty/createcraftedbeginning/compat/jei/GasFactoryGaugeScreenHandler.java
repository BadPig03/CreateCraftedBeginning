package net.ty.createcraftedbeginning.compat.jei;

import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGlobalGuiHandler;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.gui.handlers.IScreenHandler;
import mezz.jei.api.runtime.IClickableIngredient;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.ty.createcraftedbeginning.content.airtights.gasfactorygauge.GasFactoryGaugeScreen;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasFactoryGaugeScreenHandler implements IScreenHandler<GasFactoryGaugeScreen>, IGlobalGuiHandler {
    @Override
    public @Nullable IGuiProperties apply(GasFactoryGaugeScreen screen) {
        if (screen.width <= 0 || screen.height <= 0) {
            return null;
        }

        Rect2i bounds = screen.getGuiBounds();
        int left = bounds.getX();
        int top = bounds.getY();
        return new Properties(Math.max(0, left), Math.max(0, top), bounds.getWidth() - Math.min(0, left), bounds.getHeight() - Math.min(0, top), screen.width, screen.height);
    }

    @Override
    public List<Rect2i> getGuiExtraAreas() {
        if (!(Minecraft.getInstance().screen instanceof GasFactoryGaugeScreen screen)) {
            return List.of();
        }

        return screen.getExtraAreas();
    }

    @Override
    public Optional<IClickableIngredient<?>> getClickableIngredientUnderMouse(IClickableIngredientFactory factory, double mouseX, double mouseY) {
        if (!(Minecraft.getInstance().screen instanceof GasFactoryGaugeScreen screen)) {
            return Optional.empty();
        }

        return screen.getHoveredIngredient((int) mouseX, (int) mouseY).flatMap(hovered -> factory.createBuilder(hovered.getFirst().copyWithCount(1)).buildWithArea(hovered.getSecond()));
    }

    private record Properties(int guiLeft, int guiTop, int guiXSize, int guiYSize, int screenWidth, int screenHeight) implements IGuiProperties {
        @Override
        public Class<GasFactoryGaugeScreen> screenClass() {
            return GasFactoryGaugeScreen.class;
        }
    }
}
