package net.ty.createcraftedbeginning.content.opticalpower.laseremitter.beamrender;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.beamrender.LaserBeamCoverageRenderer.DrawState;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class LaserCloudOcclusion {
    private static @Nullable RenderTarget cloudBuffer;
    private static @Nullable RenderTarget foregroundBuffer;

    private LaserCloudOcclusion() {
    }

    public static void render(Runnable renderClouds) {
        if (!LaserBeamCoverageRenderer.hasCloudDraws() || !LaserRenderTypes.isAvailable()) {
            renderClouds.run();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget main = minecraft.getMainRenderTarget();
        RenderTarget fabulousClouds = minecraft.levelRenderer.getCloudsTarget();
        try (DrawState ignored = new DrawState()) {
            RenderSystem.disableScissor();
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.depthMask(true);
            RenderTarget temporary = prepareTarget(cloudBuffer, main, true);
            cloudBuffer = temporary;
            RenderTarget foreground = prepareTarget(foregroundBuffer, main, false);
            foregroundBuffer = foreground;
            temporary.clear(Minecraft.ON_OSX);
            foreground.clear(Minecraft.ON_OSX);
            RenderTarget clouds = fabulousClouds;
            if (clouds == null) {
                clouds = temporary;
                clouds.copyDepthFrom(main);
                clouds.bindWrite(true);
            }
            else {
                main.bindWrite(true);
            }

            renderClouds.run();
            LaserBeamCoverageRenderer.renderCloudForeground(clouds, foreground);
            RenderSystem.disableScissor();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.colorMask(true, true, true, true);
            ShaderInstance shader = LaserRenderTypes.prepareCloudShader(clouds, foreground);
            if (fabulousClouds != null) {
                temporary.bindWrite(true);
                RenderSystem.disableBlend();
                drawScreen(shader);
                GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, temporary.frameBufferId);
                GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, fabulousClouds.frameBufferId);
                GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, main.width, main.height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
                return;
            }

            main.bindWrite(true);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
            drawScreen(shader);
            main.copyDepthFrom(clouds);
        }
    }

    static void clear() {
        try (DrawState ignored = new DrawState()) {
            RenderTarget clouds = cloudBuffer;
            cloudBuffer = null;
            if (clouds != null) {
                clouds.destroyBuffers();
            }

            RenderTarget foreground = foregroundBuffer;
            foregroundBuffer = null;
            if (foreground != null) {
                foreground.destroyBuffers();
            }
        }
    }

    static void drawScreen(ShaderInstance shader) {
        shader.apply();
        BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(Mode.QUADS, DefaultVertexFormat.BLIT_SCREEN);
        builder.addVertex(0, 0, 0);
        builder.addVertex(1, 0, 0);
        builder.addVertex(1, 1, 0);
        builder.addVertex(0, 1, 0);
        BufferUploader.draw(builder.buildOrThrow());
        shader.clear();
    }

    private static RenderTarget prepareTarget(@Nullable RenderTarget target, RenderTarget main, boolean depth) {
        boolean stencil = depth && main.isStencilEnabled();
        if (target == null || target.isStencilEnabled() != stencil) {
            if (target != null) {
                target.destroyBuffers();
            }

            target = new TextureTarget(main.width, main.height, depth, Minecraft.ON_OSX);
            target.setClearColor(0, 0, 0, 0);
            if (stencil) {
                target.enableStencil();
            }
        }

        if (target.width != main.width || target.height != main.height) {
            target.resize(main.width, main.height, Minecraft.ON_OSX);
        }

        return target;
    }
}
