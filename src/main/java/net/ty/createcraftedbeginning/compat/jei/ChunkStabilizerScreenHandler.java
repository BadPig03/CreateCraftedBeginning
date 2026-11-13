package net.ty.createcraftedbeginning.compat.jei;

import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.gui.handlers.IScreenHandler;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.management.ChunkStabilizerScreen;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class ChunkStabilizerScreenHandler implements IScreenHandler<ChunkStabilizerScreen> {
    @Override
    public @Nullable IGuiProperties apply(ChunkStabilizerScreen screen) {
        if (screen.isModalOpen() || screen.width <= 0 || screen.height <= 0) {
            return null;
        }

        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        return new Properties(Math.max(0, left), Math.max(0, top), screen.getXSize() - Math.min(0, left), screen.getYSize() - Math.min(0, top), screen.width, screen.height);
    }

    private record Properties(int guiLeft, int guiTop, int guiXSize, int guiYSize, int screenWidth, int screenHeight) implements IGuiProperties {
        @Override
        public Class<ChunkStabilizerScreen> screenClass() {
            return ChunkStabilizerScreen.class;
        }
    }
}
