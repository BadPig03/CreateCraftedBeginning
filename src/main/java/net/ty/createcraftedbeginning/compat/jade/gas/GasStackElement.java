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
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import snownee.jade.api.config.IWailaConfig.IConfigOverlay;
import snownee.jade.api.ui.Element;
import snownee.jade.overlay.OverlayRenderer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.BiConsumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasStackElement extends Element {
    private static final ResourceLocation BACKGROUND = CreateCraftedBeginning.asResource("gas/full");
    private final GasObject gas;

    public GasStackElement(GasObject gas) {
        this.gas = gas;
    }

    private static void getGasSpriteAndColor(GasObject gas, BiConsumer<@Nullable TextureAtlasSprite, Integer> consumer) {
        int tint = gas.gasType().getTint();
        if (OverlayRenderer.alpha != 1) {
            tint = IConfigOverlay.applyAlpha(tint, OverlayRenderer.alpha);
        }
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

        color = IConfigOverlay.applyAlpha(color, OverlayRenderer.alpha);
        VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.gui());
        buffer.addVertex(matrix, minX, maxY, 0).setColor(color);
        buffer.addVertex(matrix, maxX, maxY, 0).setColor(color);
        buffer.addVertex(matrix, maxX, minY, 0).setColor(color);
        buffer.addVertex(matrix, minX, minY, 0).setColor(color);
        graphics.flush();
    }

    private static void drawGas(GuiGraphics graphics, float x, float y, GasObject gas, float width, float height, long capacity) {
        long amount = GasObject.bucketVolume();
        float scaledAmount = (float) amount * height / capacity;
        if (amount > 0 && scaledAmount < 1) {
            scaledAmount = 1;
        }
        if (scaledAmount > height) {
            scaledAmount = height;
        }

        float gasHeight = scaledAmount;
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

        drawGas(guiGraphics, x, y, gas, size.x, size.y, GasObject.bucketVolume());
    }

    @Override
    public @Nullable String getMessage() {
        return null;
    }
}
