package net.ty.createcraftedbeginning.api.outliner;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.BindableTexture;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;

@SuppressWarnings("unused")
public abstract class CCBOutline {
    protected final CCBOutlineParams params;
    protected final Vector4f colorTemp = new Vector4f();
    protected final Vector3f diffPosTemp = new Vector3f();
    protected final Vector3f minPosTemp = new Vector3f();
    protected final Vector3f maxPosTemp = new Vector3f();
    protected final Vector4f posTransformTemp = new Vector4f();
    protected final Vector3f normalTransformTemp = new Vector3f();

    public CCBOutline() {
        params = new CCBOutlineParams();
    }

    public CCBOutlineParams getParams() {
        return params;
    }

    public abstract void render(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, float pt);

    public void tick() {
    }

    public void bufferCuboidLine(@NotNull PoseStack poseStack, VertexConsumer consumer, @NotNull Vec3 camera, @NotNull Vector3d start, @NotNull Vector3d end, float width, Vector4f color, int lightmap, boolean disableNormals) {
        Vector3f diff = diffPosTemp;
        diff.set((float) (end.x - start.x), (float) (end.y - start.y), (float) (end.z - start.z));
        float length = Mth.sqrt(diff.x() * diff.x() + diff.y() * diff.y() + diff.z() * diff.z());
        float hAngle = AngleHelper.deg(Mth.atan2(diff.x(), diff.z()));
        float hDistance = Mth.sqrt(diff.x() * diff.x() + diff.z() * diff.z());
        float vAngle = AngleHelper.deg(Mth.atan2(hDistance, diff.y())) - 90;

        poseStack.pushPose();

        TransformStack.of(poseStack).translate(start.x - camera.x, start.y - camera.y, start.z - camera.z).rotateYDegrees(hAngle).rotateXDegrees(vAngle);
        bufferCuboidLine(poseStack.last(), consumer, new Vector3f(), Direction.SOUTH, length, width, color, lightmap, disableNormals);

        poseStack.popPose();
    }

    public void bufferCuboidLine(Pose pose, VertexConsumer consumer, @NotNull Vector3f origin, @NotNull Direction direction, float length, float width, Vector4f color, int lightmap, boolean disableNormals) {
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

    public void bufferCuboid(@NotNull Pose pose, @NotNull VertexConsumer consumer, @NotNull Vector3f minPos, @NotNull Vector3f maxPos, @NotNull Vector4f color, int lightmap, boolean disableNormals) {
        Vector4f posTemp = posTransformTemp;
        Vector3f normalTemp = normalTransformTemp;
        float minX = minPos.x();
        float minY = minPos.y();
        float minZ = minPos.z();
        float maxX = maxPos.x();
        float maxY = maxPos.y();
        float maxZ = maxPos.z();

        Matrix4f posMatrix = pose.pose();
        posTemp.set(minX, minY, maxZ, 1);
        posTemp.mul(posMatrix);
        float x0 = posTemp.x();
        float y0 = posTemp.y();
        float z0 = posTemp.z();

        posTemp.set(minX, minY, minZ, 1);
        posTemp.mul(posMatrix);
        float x1 = posTemp.x();
        float y1 = posTemp.y();
        float z1 = posTemp.z();

        posTemp.set(maxX, minY, minZ, 1);
        posTemp.mul(posMatrix);
        float x2 = posTemp.x();
        float y2 = posTemp.y();
        float z2 = posTemp.z();

        posTemp.set(maxX, minY, maxZ, 1);
        posTemp.mul(posMatrix);
        float x3 = posTemp.x();
        float y3 = posTemp.y();
        float z3 = posTemp.z();

        posTemp.set(minX, maxY, minZ, 1);
        posTemp.mul(posMatrix);
        float x4 = posTemp.x();
        float y4 = posTemp.y();
        float z4 = posTemp.z();

        posTemp.set(minX, maxY, maxZ, 1);
        posTemp.mul(posMatrix);
        float x5 = posTemp.x();
        float y5 = posTemp.y();
        float z5 = posTemp.z();

        posTemp.set(maxX, maxY, maxZ, 1);
        posTemp.mul(posMatrix);
        float x6 = posTemp.x();
        float y6 = posTemp.y();
        float z6 = posTemp.z();

        posTemp.set(maxX, maxY, minZ, 1);
        posTemp.mul(posMatrix);
        float x7 = posTemp.x();
        float y7 = posTemp.y();
        float z7 = posTemp.z();
        float r = color.x();
        float g = color.y();
        float b = color.z();
        float a = color.w();

        Matrix3f normalMatrix = pose.normal();
        normalTemp.set(0, disableNormals ? 1 : -1, 0);

        normalTemp.mul(normalMatrix);
        float nx0 = normalTemp.x();
        float ny0 = normalTemp.y();
        float nz0 = normalTemp.z();

        consumer.addVertex(x0, y0, z0).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightmap).setNormal(nx0, ny0, nz0);
        consumer.addVertex(x1, y1, z1).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightmap).setNormal(nx0, ny0, nz0);
        consumer.addVertex(x2, y2, z2).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightmap).setNormal(nx0, ny0, nz0);
        consumer.addVertex(x3, y3, z3).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightmap).setNormal(nx0, ny0, nz0);

        normalTemp.set(0, 1, 0);
        normalTemp.mul(normalMatrix);
        float nx1 = normalTemp.x();
        float ny1 = normalTemp.y();
        float nz1 = normalTemp.z();

        consumer.addVertex(x4, y4, z4).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightmap).setNormal(nx1, ny1, nz1);
        consumer.addVertex(x5, y5, z5).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightmap).setNormal(nx1, ny1, nz1);
        consumer.addVertex(x6, y6, z6).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightmap).setNormal(nx1, ny1, nz1);
        consumer.addVertex(x7, y7, z7).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightmap).setNormal(nx1, ny1, nz1);
        if (disableNormals) {
            normalTemp.set(0, 1, 0);
        }
        else {
            normalTemp.set(0, 0, -1);
        }

        normalTemp.mul(normalMatrix);
        float nx2 = normalTemp.x();
        float ny2 = normalTemp.y();
        float nz2 = normalTemp.z();

        consumer.addVertex(x7, y7, z7).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightmap).setNormal(nx2, ny2, nz2);
        consumer.addVertex(x2, y2, z2).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightmap).setNormal(nx2, ny2, nz2);
        consumer.addVertex(x1, y1, z1).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightmap).setNormal(nx2, ny2, nz2);
        consumer.addVertex(x4, y4, z4).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightmap).setNormal(nx2, ny2, nz2);
        if (disableNormals) {
            normalTemp.set(0, 1, 0);
        }
        else {
            normalTemp.set(0, 0, 1);
        }

        normalTemp.mul(normalMatrix);
        float nx3 = normalTemp.x();
        float ny3 = normalTemp.y();
        float nz3 = normalTemp.z();

        consumer.addVertex(x5, y5, z5).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightmap).setNormal(nx3, ny3, nz3);
        consumer.addVertex(x0, y0, z0).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightmap).setNormal(nx3, ny3, nz3);
        consumer.addVertex(x3, y3, z3).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightmap).setNormal(nx3, ny3, nz3);
        consumer.addVertex(x6, y6, z6).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightmap).setNormal(nx3, ny3, nz3);
        if (disableNormals) {
            normalTemp.set(0, 1, 0);
        }
        else {
            normalTemp.set(-1, 0, 0);
        }

        normalTemp.mul(normalMatrix);
        float nx4 = normalTemp.x();
        float ny4 = normalTemp.y();
        float nz4 = normalTemp.z();

        consumer.addVertex(x4, y4, z4).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightmap).setNormal(nx4, ny4, nz4);
        consumer.addVertex(x1, y1, z1).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightmap).setNormal(nx4, ny4, nz4);
        consumer.addVertex(x0, y0, z0).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightmap).setNormal(nx4, ny4, nz4);
        consumer.addVertex(x5, y5, z5).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightmap).setNormal(nx4, ny4, nz4);
        if (disableNormals) {
            normalTemp.set(0, 1, 0);
        }
        else {
            normalTemp.set(1, 0, 0);
        }

        normalTemp.mul(normalMatrix);
        float nx5 = normalTemp.x();
        float ny5 = normalTemp.y();
        float nz5 = normalTemp.z();

        consumer.addVertex(x6, y6, z6).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightmap).setNormal(nx5, ny5, nz5);
        consumer.addVertex(x3, y3, z3).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightmap).setNormal(nx5, ny5, nz5);
        consumer.addVertex(x2, y2, z2).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightmap).setNormal(nx5, ny5, nz5);
        consumer.addVertex(x7, y7, z7).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightmap).setNormal(nx5, ny5, nz5);
    }

    public void bufferQuad(Pose pose, VertexConsumer consumer, Vector3f pos0, Vector3f pos1, Vector3f pos2, Vector3f pos3, Vector4f color, int lightmap, Vector3f normal) {
        bufferQuad(pose, consumer, pos0, pos1, pos2, pos3, color, 0, 0, 1, 1, lightmap, normal);
    }

    public void bufferQuad(@NotNull Pose pose, @NotNull VertexConsumer consumer, @NotNull Vector3f pos0, @NotNull Vector3f pos1, @NotNull Vector3f pos2, @NotNull Vector3f pos3, @NotNull Vector4f color, float minU, float minV, float maxU, float maxV, int lightmap, Vector3f normal) {
        Vector4f posTransformTemp = this.posTransformTemp;
        Vector3f normalTransformTemp = this.normalTransformTemp;
        Matrix4f posMatrix = pose.pose();

        posTransformTemp.set(pos0.x(), pos0.y(), pos0.z(), 1);
        posTransformTemp.mul(posMatrix);
        float x0 = posTransformTemp.x();
        float y0 = posTransformTemp.y();
        float z0 = posTransformTemp.z();

        posTransformTemp.set(pos1.x(), pos1.y(), pos1.z(), 1);
        posTransformTemp.mul(posMatrix);
        float x1 = posTransformTemp.x();
        float y1 = posTransformTemp.y();
        float z1 = posTransformTemp.z();

        posTransformTemp.set(pos2.x(), pos2.y(), pos2.z(), 1);
        posTransformTemp.mul(posMatrix);
        float x2 = posTransformTemp.x();
        float y2 = posTransformTemp.y();
        float z2 = posTransformTemp.z();

        posTransformTemp.set(pos3.x(), pos3.y(), pos3.z(), 1);
        posTransformTemp.mul(posMatrix);
        float x3 = posTransformTemp.x();
        float y3 = posTransformTemp.y();
        float z3 = posTransformTemp.z();
        float r = color.x();
        float g = color.y();
        float b = color.z();
        float a = color.w();

        normalTransformTemp.set(normal);
        normalTransformTemp.mul(pose.normal());
        float nx = normalTransformTemp.x();
        float ny = normalTransformTemp.y();
        float nz = normalTransformTemp.z();

        consumer.addVertex(x0, y0, z0).setColor(r, g, b, a).setUv(minU, minV).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightmap).setNormal(nx, ny, nz);
        consumer.addVertex(x1, y1, z1).setColor(r, g, b, a).setUv(minU, maxV).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightmap).setNormal(nx, ny, nz);
        consumer.addVertex(x2, y2, z2).setColor(r, g, b, a).setUv(maxU, maxV).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightmap).setNormal(nx, ny, nz);
        consumer.addVertex(x3, y3, z3).setColor(r, g, b, a).setUv(maxU, minV).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightmap).setNormal(nx, ny, nz);
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
        @Nullable Direction highlightedFace;
        private float lineWidth;

        public CCBOutlineParams() {
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

        public CCBOutlineParams colored(@NotNull Color c) {
            rgb = c.copy();
            return this;
        }

        public CCBOutlineParams lightmap(int light) {
            lightmap = light;
            return this;
        }

        public CCBOutlineParams lineWidth(float width) {
            lineWidth = width;
            return this;
        }

        @SuppressWarnings("UnusedReturnValue")
        public CCBOutlineParams withFaceTexture(@Nullable BindableTexture texture) {
            faceTexture = texture;
            return this;
        }

        public CCBOutlineParams clearTextures() {
            return withFaceTextures(null, null);
        }

        public CCBOutlineParams withFaceTextures(@Nullable BindableTexture texture, @Nullable BindableTexture highlightTexture) {
            faceTexture = texture;
            highlightedFaceTexture = highlightTexture;
            return this;
        }

        public CCBOutlineParams highlightFace(@Nullable Direction face) {
            highlightedFace = face;
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
            return fadeLineWidth ? alpha * lineWidth : lineWidth;
        }

        @Nullable
        public Direction getHighlightedFace() {
            return highlightedFace;
        }

        public void loadColor(@NotNull Vector4f vec) {
            vec.set(rgb.getRedAsFloat(), rgb.getGreenAsFloat(), rgb.getBlueAsFloat(), rgb.getAlphaAsFloat() * alpha);
        }
    }
}
