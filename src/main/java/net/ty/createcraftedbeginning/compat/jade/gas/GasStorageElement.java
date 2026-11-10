package net.ty.createcraftedbeginning.compat.jade.gas;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec2;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.compat.jade.internal.JadeClientInternalBridge;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import snownee.jade.api.ui.Element;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasStorageElement extends Element {
    private final GasStack gas;

    public GasStorageElement(GasStack gas) {
        this.gas = gas.copy();
    }

    private static void fill(GuiGraphics graphics, float minX, float minY, float maxX, float maxY, int color) {
        Matrix4f matrix = graphics.pose().last().pose();
        if (minX < maxX) {
            float swapX = minX;
            minX = maxX;
            maxX = swapX;
        }
        if (minY < maxY) {
            float swapY = minY;
            minY = maxY;
            maxY = swapY;
        }

        color = JadeClientInternalBridge.applyOverlayAlpha(color);
        VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.gui());
        buffer.addVertex(matrix, minX, maxY, 0).setColor(color);
        buffer.addVertex(matrix, maxX, maxY, 0).setColor(color);
        buffer.addVertex(matrix, maxX, minY, 0).setColor(color);
        buffer.addVertex(matrix, minX, minY, 0).setColor(color);
        graphics.flush();
    }

    private static void drawGas(GuiGraphics graphics, float x, float y, GasStack gas, float width, float height) {
        float gasHeight = Math.max(1, height);
        int color = JadeClientInternalBridge.applyOverlayAlpha(gas.getHint());
        float maxY = y + height;
        fill(graphics, x, maxY - gasHeight, x + width, maxY, color);
    }

    @Override
    public Vec2 getSize() {
        return new Vec2(16, 16);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(GuiGraphics guiGraphics, float x, float y, float maxX, float maxY) {
        if (gas.isEmpty()) {
            return;
        }

        drawGas(guiGraphics, x, y, gas, size.x, size.y);
    }

    @Override
    public @Nullable String getMessage() {
        return null;
    }
}
