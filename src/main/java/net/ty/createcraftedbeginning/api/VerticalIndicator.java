package net.ty.createcraftedbeginning.api;

import com.google.common.collect.ImmutableList;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.ty.createcraftedbeginning.data.CCBGUITextures;
import org.jetbrains.annotations.NotNull;

public class VerticalIndicator extends AbstractSimiWidget {
    public State state;
    public boolean right;

    public VerticalIndicator(int x, int y, boolean right) {
        super(x, y, CCBGUITextures.INDICATOR_RIGHT.getWidth(), CCBGUITextures.INDICATOR_RIGHT.getHeight());
        toolTip = ImmutableList.of();
        state = State.OFF;
        this.right = right;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (!visible) {
            return;
        }

        if (!right) {
            CCBGUITextures toDraw = switch (state) {
                case ON -> CCBGUITextures.INDICATOR_WHITE_LEFT;
                case OFF -> CCBGUITextures.INDICATOR_LEFT;
                case RED -> CCBGUITextures.INDICATOR_RED_LEFT;
                case YELLOW -> CCBGUITextures.INDICATOR_YELLOW_LEFT;
                case GREEN -> CCBGUITextures.INDICATOR_GREEN_LEFT;
            };
            toDraw.render(graphics, getX(), getY());
            return;
        }

        CCBGUITextures toDraw = switch (state) {
            case ON -> CCBGUITextures.INDICATOR_WHITE_RIGHT;
            case OFF -> CCBGUITextures.INDICATOR_RIGHT;
            case RED -> CCBGUITextures.INDICATOR_RED_RIGHT;
            case YELLOW -> CCBGUITextures.INDICATOR_YELLOW_RIGHT;
            case GREEN -> CCBGUITextures.INDICATOR_GREEN_RIGHT;
        };
        toDraw.render(graphics, getX(), getY());
    }

    public enum State {
        OFF,
        ON,
        RED,
        YELLOW,
        GREEN
    }
}
