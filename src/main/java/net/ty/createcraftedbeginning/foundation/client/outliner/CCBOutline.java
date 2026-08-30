package net.ty.createcraftedbeginning.foundation.client.outliner;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.createmod.catnip.render.BindableTexture;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.createmod.catnip.theme.Color;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public abstract class CCBOutline {
    protected final CCBOutlineParams params;
    protected final Vector4f colorTemp = new Vector4f();
    protected final Vector3f minPosTemp = new Vector3f();
    protected final Vector3f maxPosTemp = new Vector3f();
    protected final Vector4f posTransformTemp = new Vector4f();
    protected final Vector3f normalTransformTemp = new Vector3f();

    protected CCBOutline() {
        params = new CCBOutlineParams();
    }

    private static void transformNormal(Vector3f normal, Matrix3f matrix, boolean disableNormals, float normalX, float normalY, float normalZ) {
        normal.set(disableNormals ? 0 : normalX, disableNormals ? 1 : normalY, disableNormals ? 0 : normalZ).mul(matrix);
    }

    private static void bufferVertex(VertexConsumer consumer, float vertexX, float vertexY, float vertexZ, Vector4f color, float u, float v, int lightmap, Vector3f normal) {
        consumer.addVertex(vertexX, vertexY, vertexZ).setColor(color.x(), color.y(), color.z(), color.w()).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightmap).setNormal(normal.x(), normal.y(), normal.z());
    }

    public CCBOutlineParams getParams() {
        return params;
    }

    public abstract void render(PoseStack poseStack, SuperRenderTypeBuffer buffer, Vec3 camera, float partialTicks);

    public void tick() {
    }

    public void bufferCuboidLine(Pose pose, VertexConsumer consumer, Vector3f origin, Direction direction, float length, float width, Vector4f color, int lightmap, boolean disableNormals) {
        Vector3f minPos = minPosTemp;
        Vector3f maxPos = maxPosTemp;

        float halfWidth = width / 2;
        minPos.set(origin.x() - halfWidth, origin.y() - halfWidth, origin.z() - halfWidth);
        maxPos.set(origin.x() + halfWidth, origin.y() + halfWidth, origin.z() + halfWidth);
        switch (direction) {
            case DOWN -> minPos.add(0, -length, 0);
            case UP -> maxPos.add(0, length, 0);
            case NORTH -> minPos.add(0, 0, -length);
            case SOUTH -> maxPos.add(0, 0, length);
            case WEST -> minPos.add(-length, 0, 0);
            case EAST -> maxPos.add(length, 0, 0);
        }

        bufferCuboid(pose, consumer, minPos, maxPos, color, lightmap, disableNormals);
    }

    protected void bufferCuboid(Pose pose, VertexConsumer consumer, Vector3f minPos, Vector3f maxPos, Vector4f color, int lightmap, boolean disableNormals) {
        Vector4f position = posTransformTemp;
        Vector3f normal = normalTransformTemp;
        float minX = minPos.x();
        float minY = minPos.y();
        float minZ = minPos.z();
        float maxX = maxPos.x();
        float maxY = maxPos.y();
        float maxZ = maxPos.z();

        Matrix4f poseMatrix = pose.pose();
        position.set(minX, minY, maxZ, 1).mul(poseMatrix);
        float x0 = position.x();
        float y0 = position.y();
        float z0 = position.z();

        position.set(minX, minY, minZ, 1).mul(poseMatrix);
        float x1 = position.x();
        float y1 = position.y();
        float z1 = position.z();

        position.set(maxX, minY, minZ, 1).mul(poseMatrix);
        float x2 = position.x();
        float y2 = position.y();
        float z2 = position.z();

        position.set(maxX, minY, maxZ, 1).mul(poseMatrix);
        float x3 = position.x();
        float y3 = position.y();
        float z3 = position.z();

        position.set(minX, maxY, minZ, 1).mul(poseMatrix);
        float x4 = position.x();
        float y4 = position.y();
        float z4 = position.z();

        position.set(minX, maxY, maxZ, 1).mul(poseMatrix);
        float x5 = position.x();
        float y5 = position.y();
        float z5 = position.z();

        position.set(maxX, maxY, maxZ, 1).mul(poseMatrix);
        float x6 = position.x();
        float y6 = position.y();
        float z6 = position.z();

        position.set(maxX, maxY, minZ, 1).mul(poseMatrix);
        float x7 = position.x();
        float y7 = position.y();
        float z7 = position.z();

        Matrix3f normalMatrix = pose.normal();
        transformNormal(normal, normalMatrix, disableNormals, 0, -1, 0);
        bufferVertex(consumer, x0, y0, z0, color, 0, 0, lightmap, normal);
        bufferVertex(consumer, x1, y1, z1, color, 0, 1, lightmap, normal);
        bufferVertex(consumer, x2, y2, z2, color, 1, 1, lightmap, normal);
        bufferVertex(consumer, x3, y3, z3, color, 1, 0, lightmap, normal);

        transformNormal(normal, normalMatrix, disableNormals, 0, 1, 0);
        bufferVertex(consumer, x4, y4, z4, color, 0, 0, lightmap, normal);
        bufferVertex(consumer, x5, y5, z5, color, 0, 1, lightmap, normal);
        bufferVertex(consumer, x6, y6, z6, color, 1, 1, lightmap, normal);
        bufferVertex(consumer, x7, y7, z7, color, 1, 0, lightmap, normal);

        transformNormal(normal, normalMatrix, disableNormals, 0, 0, -1);
        bufferVertex(consumer, x7, y7, z7, color, 0, 0, lightmap, normal);
        bufferVertex(consumer, x2, y2, z2, color, 0, 1, lightmap, normal);
        bufferVertex(consumer, x1, y1, z1, color, 1, 1, lightmap, normal);
        bufferVertex(consumer, x4, y4, z4, color, 1, 0, lightmap, normal);

        transformNormal(normal, normalMatrix, disableNormals, 0, 0, 1);
        bufferVertex(consumer, x5, y5, z5, color, 0, 0, lightmap, normal);
        bufferVertex(consumer, x0, y0, z0, color, 0, 1, lightmap, normal);
        bufferVertex(consumer, x3, y3, z3, color, 1, 1, lightmap, normal);
        bufferVertex(consumer, x6, y6, z6, color, 1, 0, lightmap, normal);

        transformNormal(normal, normalMatrix, disableNormals, -1, 0, 0);
        bufferVertex(consumer, x4, y4, z4, color, 0, 0, lightmap, normal);
        bufferVertex(consumer, x1, y1, z1, color, 0, 1, lightmap, normal);
        bufferVertex(consumer, x0, y0, z0, color, 1, 1, lightmap, normal);
        bufferVertex(consumer, x5, y5, z5, color, 1, 0, lightmap, normal);

        transformNormal(normal, normalMatrix, disableNormals, 1, 0, 0);
        bufferVertex(consumer, x6, y6, z6, color, 0, 0, lightmap, normal);
        bufferVertex(consumer, x3, y3, z3, color, 0, 1, lightmap, normal);
        bufferVertex(consumer, x2, y2, z2, color, 1, 1, lightmap, normal);
        bufferVertex(consumer, x7, y7, z7, color, 1, 0, lightmap, normal);
    }

    public void bufferQuad(Pose pose, VertexConsumer consumer, Vector3f pos0, Vector3f pos1, Vector3f pos2, Vector3f pos3, Vector4f color, int lightmap, Vector3f normal) {
        bufferQuad(pose, consumer, pos0, pos1, pos2, pos3, color, 0, 0, 1, 1, lightmap, normal);
    }

    public void bufferQuad(Pose pose, VertexConsumer consumer, Vector3f pos0, Vector3f pos1, Vector3f pos2, Vector3f pos3, Vector4f color, float minU, float minV, float maxU, float maxV, int lightmap, Vector3f normal) {
        Vector4f position = posTransformTemp;
        Vector3f transformedNormal = normalTransformTemp;
        Matrix4f poseMatrix = pose.pose();

        position.set(pos0.x(), pos0.y(), pos0.z(), 1).mul(poseMatrix);
        float x0 = position.x();
        float y0 = position.y();
        float z0 = position.z();

        position.set(pos1.x(), pos1.y(), pos1.z(), 1).mul(poseMatrix);
        float x1 = position.x();
        float y1 = position.y();
        float z1 = position.z();

        position.set(pos2.x(), pos2.y(), pos2.z(), 1).mul(poseMatrix);
        float x2 = position.x();
        float y2 = position.y();
        float z2 = position.z();

        position.set(pos3.x(), pos3.y(), pos3.z(), 1).mul(poseMatrix);
        float x3 = position.x();
        float y3 = position.y();
        float z3 = position.z();

        transformedNormal.set(normal).mul(pose.normal());
        bufferVertex(consumer, x0, y0, z0, color, minU, minV, lightmap, transformedNormal);
        bufferVertex(consumer, x1, y1, z1, color, minU, maxV, lightmap, transformedNormal);
        bufferVertex(consumer, x2, y2, z2, color, maxU, maxV, lightmap, transformedNormal);
        bufferVertex(consumer, x3, y3, z3, color, maxU, minV, lightmap, transformedNormal);
    }

    public static class CCBOutlineParams {
        @Nullable
        protected BindableTexture faceTexture;
        @Nullable
        protected BindableTexture highlightedFaceTexture;
        protected boolean fadeLineWidth;
        protected boolean disableCull;
        protected boolean disableLineNormals;
        protected float alpha;
        protected int lightmap;
        protected Color rgb;
        protected float lineWidth;

        protected CCBOutlineParams() {
            faceTexture = highlightedFaceTexture = null;
            alpha = 1;
            lineWidth = 0.03125f;
            fadeLineWidth = true;
            rgb = Color.WHITE;
            lightmap = LightTexture.FULL_BRIGHT;
        }

        public CCBOutlineParams colored(int color) {
            rgb = new Color(color, false);
            return this;
        }

        public CCBOutlineParams colored(Color color) {
            rgb = color.copy();
            return this;
        }

        public CCBOutlineParams lineWidth(float width) {
            lineWidth = width;
            return this;
        }

        public CCBOutlineParams withFaceTexture(@Nullable BindableTexture texture) {
            faceTexture = texture;
            return this;
        }

        public CCBOutlineParams disableLineNormals() {
            disableLineNormals = true;
            return this;
        }

        public CCBOutlineParams disableCull() {
            disableCull = true;
            return this;
        }

        public float getLineWidth() {
            if (fadeLineWidth) {
                return alpha * lineWidth;
            }
            return lineWidth;
        }

        public void loadColor(Vector4f targetColor) {
            targetColor.set(rgb.getRedAsFloat(), rgb.getGreenAsFloat(), rgb.getBlueAsFloat(), rgb.getAlphaAsFloat() * alpha);
        }
    }
}
