package net.ty.createcraftedbeginning.content.fluids;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer.FogMode;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Vector3f;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Internal
@OnlyIn(Dist.CLIENT)
public final class CCBFluidClientExtensions {
    private CCBFluidClientExtensions() {
    }

    public static IClientFluidTypeExtensions tinted(TintedFluidType fluidType) {
        return new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return fluidType.clientStillTexture();
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return fluidType.clientFlowingTexture();
            }

            @Override
            public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector3f fogColor) {
                Vector3f customFogColor = fluidType.clientCustomFogColor();
                if (customFogColor == null) {
                    return fogColor;
                }

                return customFogColor;
            }

            @Override
            public void modifyFogRender(Camera camera, FogMode mode, float renderDistance, float partialTick, float nearDistance, float farDistance, FogShape shape) {
                float fogDistanceModifier = fluidType.clientFogDistanceModifier();
                if (fogDistanceModifier == 1) {
                    return;
                }

                RenderSystem.setShaderFogShape(FogShape.CYLINDER);
                RenderSystem.setShaderFogStart(-8);
                RenderSystem.setShaderFogEnd(96 * fogDistanceModifier);
            }

            @Override
            public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
                return fluidType.clientTintColor(state, getter, pos);
            }

            @Override
            public int getTintColor(FluidStack stack) {
                return fluidType.clientTintColor(stack);
            }
        };
    }
}
