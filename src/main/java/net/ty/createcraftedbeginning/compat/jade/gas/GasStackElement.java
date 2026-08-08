package net.ty.createcraftedbeginning.compat.jade.gas;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec2;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.compat.jade.internal.JadeClientInternalBridge;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import snownee.jade.api.ui.Element;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.BiConsumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasStackElement extends Element {
    private static final ResourceLocation BACKGROUND = CCBAPI.asResource("gas/full");

    private final GasObject gas;

    public GasStackElement(GasObject gas) {
        this.gas = gas;
    }

    private static void getGasSpriteAndColor(GasObject gas, BiConsumer<@Nullable TextureAtlasSprite, Integer> consumer) {
        int tint = JadeClientInternalBridge.applyOverlayAlpha(gas.gasType().getTint());
        consumer.accept(Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(BACKGROUND), tint);
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

    private static void drawGas(GuiGraphics graphics, float x, float y, GasObject gas, float width, float height) {
        float gasHeight = Math.max(1, height);
        getGasSpriteAndColor(gas, (sprite, color) -> {
            float maxY = y + height;
            fill(graphics, x, maxY - gasHeight, x + width, maxY, color);
        });
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
