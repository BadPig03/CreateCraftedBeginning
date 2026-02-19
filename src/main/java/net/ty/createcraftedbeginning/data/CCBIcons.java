package net.ty.createcraftedbeginning.data;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.gui.AllIcons;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

@SuppressWarnings("unused")
public class CCBIcons extends AllIcons {
    public static final ResourceLocation CCB_ICON_ATLAS = CreateCraftedBeginning.asResource("textures/gui/icons.png");
    public static final int CCB_ICON_ATLAS_SIZE = 256;

    private static int ccbX;
    private static int ccbY = -1;
    private final int iconX;
    private final int iconY;

    public static final CCBIcons I_NO_TRANSFER = newRow();
    public static final CCBIcons I_INPUT_ONLY = next();
    public static final CCBIcons I_OUTPUT_ONLY = next();
    public static final CCBIcons I_STAY_HALF = next();
    public static final CCBIcons I_EMPTY = next();
    public static final CCBIcons I_FILTER = newRow();
    public static final CCBIcons I_FINISHED = next();
    public static final CCBIcons I_OUTLINE = next();
    public static final CCBIcons I_CONTAINER = next();
    public static final CCBIcons I_DRILL_ATTACK = next();
    public static final CCBIcons I_SILK_TOUCH_UPGRADE = next();
    public static final CCBIcons I_MAGNET_UPGRADE = next();
    public static final CCBIcons I_CONVERSION_UPGRADE = next();
    public static final CCBIcons I_LIQUID_REPLACEMENT_UPGRADE = next();
    public static final CCBIcons I_SMOKING = newRow();
    public static final CCBIcons I_BLASTING = next();
    public static final CCBIcons I_IGNITION = next();

    public CCBIcons(int x, int y) {
        super(x, y);
        iconX = x * 16;
        iconY = y * 16;
    }

    @Contract(" -> new")
    private static @NotNull CCBIcons newRow() {
        return new CCBIcons(ccbX = 0, ++ccbY);
    }

    @Contract(" -> new")
    private static @NotNull CCBIcons next() {
        return new CCBIcons(++ccbX, ccbY);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void bind() {
        RenderSystem.setShaderTexture(0, CCB_ICON_ATLAS);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(@NotNull GuiGraphics graphics, int x, int y) {
        graphics.blit(CCB_ICON_ATLAS, x, y, 0, iconX, iconY, 16, 16, 256, 256);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(@NotNull PoseStack ms, @NotNull MultiBufferSource buffer, int color) {
        VertexConsumer builder = buffer.getBuffer(RenderType.text(CCB_ICON_ATLAS));
        Matrix4f matrix = ms.last().pose();
        Color rgb = new Color(color);
        int light = LightTexture.FULL_BRIGHT;

        Vec3 vec1 = new Vec3(0, 0, 0);
        Vec3 vec2 = new Vec3(0, 1, 0);
        Vec3 vec3 = new Vec3(1, 1, 0);
        Vec3 vec4 = new Vec3(1, 0, 0);

        float u1 = iconX * 1.0f / CCB_ICON_ATLAS_SIZE;
        float u2 = (iconX + 16) * 1.0f / CCB_ICON_ATLAS_SIZE;
        float v1 = iconY * 1.0f / CCB_ICON_ATLAS_SIZE;
        float v2 = (iconY + 16) * 1.0f / CCB_ICON_ATLAS_SIZE;

        vertex(builder, matrix, vec1, rgb, u1, v1, light);
        vertex(builder, matrix, vec2, rgb, u1, v2, light);
        vertex(builder, matrix, vec3, rgb, u2, v2, light);
        vertex(builder, matrix, vec4, rgb, u2, v1, light);
    }

    @OnlyIn(Dist.CLIENT)
    private void vertex(@NotNull VertexConsumer builder, Matrix4f matrix, @NotNull Vec3 vec, @NotNull Color rgb, float u, float v, int light) {
        builder.addVertex(matrix, (float) vec.x, (float) vec.y, (float) vec.z).setColor(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), 255).setUv(u, v).setLight(light);
    }
}
