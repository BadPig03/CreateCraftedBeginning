package net.ty.createcraftedbeginning.content.opticalpower.laseremitter.beamrender;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent.Pre;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.LaserEmitterBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.laseremitter.beamrender.LaserBeamVertices.Layer;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID, value = Dist.CLIENT)
final class LaserBeamCoverageRenderer {
    private static final Map<LaserEmitterBlockEntity, Submission> PENDING = new LinkedHashMap<>();
    private static @Nullable RenderTarget coverage;
    private static @Nullable ClientLevel frameLevel;

    private LaserBeamCoverageRenderer() {
    }

    static void submit(LaserEmitterBlockEntity emitter, Matrix4f pose, List<Layer> layers) {
        PENDING.put(emitter, new Submission(new Matrix4f(pose), new Matrix4f(RenderSystem.getModelViewMatrix()), new Matrix4f(RenderSystem.getProjectionMatrix()), layers));
    }

    static void invalidate(LaserEmitterBlockEntity owner) {
        PENDING.remove(owner);
    }

    static void clear() {
        RenderSystem.assertOnRenderThread();
        PENDING.clear();
        frameLevel = null;
        RenderTarget target = coverage;
        coverage = null;
        if (target == null) {
            return;
        }

        try (DrawState ignored = new DrawState()) {
            target.destroyBuffers();
        }
    }

    @SubscribeEvent
    private static void onFrame(Pre event) {
        PENDING.clear();
        ClientLevel level = Minecraft.getInstance().level;
        ClientLevel previousLevel = frameLevel;
        if (previousLevel != level) {
            if (previousLevel != null) {
                LaserEmitterRenderer.clearWorld(previousLevel);
            }

            clear();
            frameLevel = level;
        }

        LaserBeamMeshCache.beginFrame(level);
    }

    @SubscribeEvent
    private static void onReload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> LaserEmitterRenderer.clearCaches());
    }

    @SubscribeEvent
    private static void onShutdown(GameShuttingDownEvent event) {
        LaserEmitterRenderer.clearCaches();
    }

    @SubscribeEvent
    private static void render(RenderLevelStageEvent event) {
        if (event.getStage() != Stage.AFTER_BLOCK_ENTITIES || PENDING.isEmpty()) {
            return;
        }

        if (!LaserRenderTypes.isAvailable()) {
            PENDING.clear();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget main = minecraft.getMainRenderTarget();
        try (DrawState ignored = new DrawState()) {
            ShaderInstance shader = LaserRenderTypes.prepareLaserShader(false);
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
            RenderSystem.disableScissor();
            RenderSystem.colorMask(true, true, true, true);
            RenderTarget target = coverage;
            boolean stencil = main.isStencilEnabled();
            if (target == null || target.isStencilEnabled() != stencil) {
                if (target != null) {
                    target.destroyBuffers();
                }

                target = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
                if (stencil) {
                    target.enableStencil();
                }

                coverage = target;
            }

            if (target.width != main.width || target.height != main.height) {
                target.resize(main.width, main.height, Minecraft.ON_OSX);
            }

            boolean depthReady = false;
            for (Submission submission : PENDING.values()) {
                Matrix4f pose = submission.pose();
                Matrix4f modelView = submission.modelView();
                Matrix4f projection = submission.projection();
                for (Layer vertices : submission.layers()) {
                    if (vertices.isEmpty()) {
                        continue;
                    }

                    LaserBeamScissor scissor = LaserBeamScissor.project(vertices.bounds(), pose, modelView, projection, main.width, main.height);
                    int scissorWidth = scissor.width();
                    int scissorHeight = scissor.height();
                    if (scissorWidth == 0 || scissorHeight == 0) {
                        continue;
                    }

                    BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
                    vertices.render(builder, pose);
                    MeshData mesh = builder.build();
                    if (mesh == null) {
                        continue;
                    }

                    target.bindWrite(true);
                    if (!depthReady) {
                        RenderSystem.disableScissor();
                        target.copyDepthFrom(main);
                        depthReady = true;
                    }

                    target.bindWrite(true);
                    RenderSystem.enableScissor(scissor.x(), scissor.y(), scissorWidth, scissorHeight);
                    RenderSystem.clearColor(0, 0, 0, 0);
                    RenderSystem.colorMask(true, true, true, true);
                    RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT, Minecraft.ON_OSX);
                    RenderSystem.enableDepthTest();
                    RenderSystem.depthFunc(GL11.GL_LEQUAL);
                    RenderSystem.depthMask(false);
                    RenderSystem.disableCull();
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    shader.setDefaultUniforms(Mode.QUADS, modelView, projection, minecraft.getWindow());
                    shader.apply();
                    RenderSystem.blendEquation(GL14.GL_MAX);
                    BufferUploader.draw(mesh);
                    shader.clear();
                    RenderSystem.blendEquation(GL14.GL_FUNC_ADD);

                    main.bindWrite(true);
                    RenderSystem.defaultBlendFunc();
                    target.blitToScreen(main.width, main.height, false);
                }
            }
        }
        finally {
            PENDING.clear();
        }
    }

    private record Submission(Matrix4f pose, Matrix4f modelView, Matrix4f projection, List<Layer> layers) {}

    private static final class DrawState implements AutoCloseable {
        private final boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        private final boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        private final boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        private final boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        private final boolean depthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        private final int depthFunction = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        private final int equationRgb = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB);
        private final int equationAlpha = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
        private final int sourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        private final int destinationRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        private final int sourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        private final int destinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        private final int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        private final int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        private final int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        private final int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        private final int[] viewport = new int[4];
        private final int[] scissorBox = new int[4];
        private final float[] clearColor = new float[4];
        private final boolean[] colorWrite = new boolean[4];
        private final int texture;

        private DrawState() {
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissorBox);
            GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, clearColor);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                ByteBuffer mask = stack.malloc(4);
                GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, mask);
                for (int channel = 0; channel < 4; channel++) {
                    colorWrite[channel] = mask.get(channel) != 0;
                }
            }

            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
            texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            RenderSystem.activeTexture(activeTexture);
        }

        @Override
        public void close() {
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            RenderSystem.enableScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3]);
            GL20.glBlendEquationSeparate(equationRgb, equationAlpha);
            RenderSystem.blendFuncSeparate(sourceRgb, destinationRgb, sourceAlpha, destinationAlpha);
            RenderSystem.depthFunc(depthFunction);
            RenderSystem.depthMask(depthWrite);
            RenderSystem.colorMask(colorWrite[0], colorWrite[1], colorWrite[2], colorWrite[3]);
            RenderSystem.clearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
            GlStateManager._bindTexture(texture);
            RenderSystem.activeTexture(activeTexture);
            GlStateManager._glUseProgram(program);
            if (depth) {
                RenderSystem.enableDepthTest();
            }
            else {
                RenderSystem.disableDepthTest();
            }

            if (cull) {
                RenderSystem.enableCull();
            }
            else {
                RenderSystem.disableCull();
            }

            if (blend) {
                RenderSystem.enableBlend();
            }
            else {
                RenderSystem.disableBlend();
            }

            if (!scissor) {
                RenderSystem.disableScissor();
                return;
            }

            GlStateManager._enableScissorTest();
        }
    }
}
