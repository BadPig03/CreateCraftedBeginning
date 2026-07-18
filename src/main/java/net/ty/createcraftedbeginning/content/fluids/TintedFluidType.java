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
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class TintedFluidType extends FluidType {
    private final ResourceLocation stillTexture;
    private final ResourceLocation flowingTexture;

    public TintedFluidType(Properties properties, ResourceLocation stillTexture, ResourceLocation flowingTexture) {
        super(properties);
        this.stillTexture = stillTexture;
        this.flowingTexture = flowingTexture;
        CCBFluidClientExtensions.track(this, this::createClientExtensions);
    }

    private IClientFluidTypeExtensions createClientExtensions() {
        return new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return stillTexture;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return flowingTexture;
            }

            @Override
            public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector3f fogColor) {
                Vector3f customColor = getCustomFogColor();
                return customColor == null ? fogColor : customColor;
            }

            @Override
            public void modifyFogRender(Camera camera, FogMode mode, float renderDistance, float partialTick, float nearDistance, float farDistance, FogShape shape) {
                float modifier = getFogDistanceModifier();
                if (modifier == 1.0f) {
                    return;
                }

                RenderSystem.setShaderFogShape(FogShape.CYLINDER);
                RenderSystem.setShaderFogStart(-8);
                RenderSystem.setShaderFogEnd(96.0f * modifier);
            }

            @Override
            public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
                return TintedFluidType.this.getTintColor(state, getter, pos);
            }

            @Override
            public int getTintColor(FluidStack stack) {
                return TintedFluidType.this.getTintColor(stack);
            }
        };
    }

    protected abstract int getTintColor(FluidStack stack);

    protected abstract int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos);

    protected @Nullable Vector3f getCustomFogColor() {
        return null;
    }

    protected float getFogDistanceModifier() {
        return 1.0f;
    }
}