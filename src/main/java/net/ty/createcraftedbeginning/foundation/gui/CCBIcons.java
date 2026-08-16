package net.ty.createcraftedbeginning.foundation.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.foundation.gui.AllIcons;
import net.createmod.catnip.theme.Color;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.CCBAPI;
import org.jetbrains.annotations.Contract;
import org.joml.Matrix4f;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBIcons extends AllIcons {
    private static final ResourceLocation CCB_ICON_ATLAS = CCBAPI.asResource("textures/gui/icons.png");
    private static final int CCB_ICON_ATLAS_SIZE = 256;

    private static int ccbX;
    private static int ccbY = -1;

    public static final CCBIcons I_NO_TRANSFER = newRow();
    public static final CCBIcons I_INPUT_ONLY = next();
    public static final CCBIcons I_OUTPUT_ONLY = next();
    public static final CCBIcons I_STAY_HALF = next();
    private static final CCBIcons I_FILTER = newRow();
    public static final CCBIcons I_FINISHED = next();
    private static final CCBIcons I_OUTLINE_DISPLAY = next();
    private static final CCBIcons I_CONTAINER_PROTECTION = next();
    private static final CCBIcons I_ATTACK_MODE = next();
    private static final CCBIcons I_SILK_TOUCH = next();
    private static final CCBIcons I_MAGNET = next();
    private static final CCBIcons I_EXPERIENCE_CONVERSION = next();
    private static final CCBIcons I_LIQUID_REPLACEMENT = next();
    public static final CCBIcons I_SMOKING = newRow();
    public static final CCBIcons I_BLASTING = next();
    public static final CCBIcons I_IGNITION = next();
    public static final CCBIcons I_1X1 = newRow();
    public static final CCBIcons I_3X3 = next();
    public static final CCBIcons I_5X5 = next();
    private static final CCBIcons I_EFFECTS_PROTECTION = newRow();
    private static final CCBIcons I_WATER_BREATHING = next();
    private static final CCBIcons I_GOGGLES = next();
    private static final CCBIcons I_VISION = next();
    private static final CCBIcons I_SPECTRAL = next();
    private static final CCBIcons I_RESISTANCE = next();
    private static final CCBIcons I_ELYTRA = next();
    private static final CCBIcons I_CREATIVE_FLIGHT = next();
    private static final CCBIcons I_INVISIBILITY = next();
    private static final CCBIcons I_REGENERATION = next();
    private static final CCBIcons I_HASTE = next();
    private static final CCBIcons I_PROJECTILE_DEFLECTION = next();
    private static final CCBIcons I_QUICK_SWIMMING = next();
    private static final CCBIcons I_SWIFT_SNEAK = next();
    private static final CCBIcons I_CRAMMING_PROTECTION = next();
    private static final CCBIcons I_BLAST_RESISTANCE = next();
    private static final CCBIcons I_MOVEMENT_EFFICIENCY = newRow();
    private static final CCBIcons I_JUMP_STRENGTH = next();
    private static final CCBIcons I_STEP_HEIGHT = next();
    private static final CCBIcons I_ENVIRONMENTAL_DAMAGE_PROTECTION = next();
    private static final CCBIcons I_FALL_PROTECTION = next();

    private final int iconX;
    private final int iconY;

    public CCBIcons(int x, int y) {
        super(x, y);
        iconX = x * 16;
        iconY = y * 16;
    }

    public static CCBIcons get(AirtightUpgradeIcon icon) {
        return switch (icon) {
            case MAGNET -> I_MAGNET;
            case EXPERIENCE_CONVERSION -> I_EXPERIENCE_CONVERSION;
            case ATTACK_MODE -> I_ATTACK_MODE;
            case FILTER -> I_FILTER;
            case OUTLINE_DISPLAY -> I_OUTLINE_DISPLAY;
            case SILK_TOUCH -> I_SILK_TOUCH;
            case LIQUID_REPLACEMENT -> I_LIQUID_REPLACEMENT;
            case CONTAINER_PROTECTION -> I_CONTAINER_PROTECTION;
            case EFFECTS_PROTECTION -> I_EFFECTS_PROTECTION;
            case RESISTANCE -> I_RESISTANCE;
            case GOGGLES -> I_GOGGLES;
            case VISION -> I_VISION;
            case WATER_BREATHING -> I_WATER_BREATHING;
            case SPECTRAL -> I_SPECTRAL;
            case ELYTRA -> I_ELYTRA;
            case HASTE -> I_HASTE;
            case INVISIBILITY -> I_INVISIBILITY;
            case REGENERATION -> I_REGENERATION;
            case CREATIVE_FLIGHT -> I_CREATIVE_FLIGHT;
            case ENVIRONMENTAL_DAMAGE_PROTECTION -> I_ENVIRONMENTAL_DAMAGE_PROTECTION;
            case STEP_HEIGHT -> I_STEP_HEIGHT;
            case JUMP_STRENGTH -> I_JUMP_STRENGTH;
            case FALL_PROTECTION -> I_FALL_PROTECTION;
            case MOVEMENT_EFFICIENCY -> I_MOVEMENT_EFFICIENCY;
            case CRAMMING_PROTECTION -> I_CRAMMING_PROTECTION;
            case QUICK_SWIMMING -> I_QUICK_SWIMMING;
            case BLAST_RESISTANCE -> I_BLAST_RESISTANCE;
            case SWIFT_SNEAK -> I_SWIFT_SNEAK;
            case PROJECTILE_DEFLECTION -> I_PROJECTILE_DEFLECTION;
        };
    }

    @Contract(" -> new")
    private static CCBIcons newRow() {
        return new CCBIcons(ccbX = 0, ++ccbY);
    }

    @Contract(" -> new")
    private static CCBIcons next() {
        return new CCBIcons(++ccbX, ccbY);
    }

    @OnlyIn(Dist.CLIENT)
    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Vec3 position, Color color, float u, float v, int light) {
        consumer.addVertex(matrix, (float) position.x, (float) position.y, (float) position.z).setColor(color.getRed(), color.getGreen(), color.getBlue(), 255).setUv(u, v).setLight(light);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void bind() {
        RenderSystem.setShaderTexture(0, CCB_ICON_ATLAS);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void render(GuiGraphics graphics, int x, int y) {
        graphics.blit(CCB_ICON_ATLAS, x, y, 0, iconX, iconY, 16, 16, CCB_ICON_ATLAS_SIZE, CCB_ICON_ATLAS_SIZE);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void render(PoseStack ms, MultiBufferSource buffer, int color) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.text(CCB_ICON_ATLAS));
        Matrix4f matrix = ms.last().pose();
        Color tint = new Color(color);

        Vec3 bottomLeft = new Vec3(0, 0, 0);
        Vec3 topLeft = new Vec3(0, 1, 0);
        Vec3 topRight = new Vec3(1, 1, 0);
        Vec3 bottomRight = new Vec3(1, 0, 0);

        float u1 = iconX * 1.0f / CCB_ICON_ATLAS_SIZE;
        float u2 = (iconX + 16) * 1.0f / CCB_ICON_ATLAS_SIZE;
        float v1 = iconY * 1.0f / CCB_ICON_ATLAS_SIZE;
        float v2 = (iconY + 16) * 1.0f / CCB_ICON_ATLAS_SIZE;

        vertex(consumer, matrix, bottomLeft, tint, u1, v1, LightTexture.FULL_BRIGHT);
        vertex(consumer, matrix, topLeft, tint, u1, v2, LightTexture.FULL_BRIGHT);
        vertex(consumer, matrix, topRight, tint, u2, v2, LightTexture.FULL_BRIGHT);
        vertex(consumer, matrix, bottomRight, tint, u2, v1, LightTexture.FULL_BRIGHT);
    }
}
