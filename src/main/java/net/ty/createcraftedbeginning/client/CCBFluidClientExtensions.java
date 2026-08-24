package net.ty.createcraftedbeginning.client;

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
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.ty.createcraftedbeginning.content.fluids.CCBFluidClientExtensionRegistry;
import net.ty.createcraftedbeginning.content.fluids.CCBFluidClientExtensionRegistry.FluidRegistration;
import net.ty.createcraftedbeginning.content.fluids.CCBFluidClientExtensionRegistry.SimpleRegistration;
import net.ty.createcraftedbeginning.content.fluids.CCBFluidClientExtensionRegistry.TintedRegistration;
import net.ty.createcraftedbeginning.content.fluids.TintedFluidType;
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

    public static void register(RegisterClientExtensionsEvent event) {
        for (FluidRegistration registration : CCBFluidClientExtensionRegistry.registrations()) {
            switch (registration) {
                case SimpleRegistration(FluidType fluidType, ResourceLocation stillTexture, ResourceLocation flowingTexture) -> event.registerFluidType(createSimple(stillTexture, flowingTexture), fluidType);
                case TintedRegistration(TintedFluidType fluidType) -> event.registerFluidType(createTinted(fluidType), fluidType);
                default -> {
                }
            }
        }
    }

    private static IClientFluidTypeExtensions createSimple(ResourceLocation stillTexture, ResourceLocation flowingTexture) {
        return new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return stillTexture;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return flowingTexture;
            }
        };
    }

    private static IClientFluidTypeExtensions createTinted(TintedFluidType fluidType) {
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
                return customFogColor == null ? fogColor : customFogColor;
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
