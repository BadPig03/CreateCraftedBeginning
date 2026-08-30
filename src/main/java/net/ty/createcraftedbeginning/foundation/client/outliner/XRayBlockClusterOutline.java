package net.ty.createcraftedbeginning.foundation.client.outliner;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.BindableTexture;
import net.createmod.catnip.render.PonderRenderTypes;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.CoordinateTransform;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class XRayBlockClusterOutline extends CCBOutline {
    private static final double DIRECTION_LENGTH_SQR_EPSILON = 1.0E-12;

    protected final Vector3f pos0Temp = new Vector3f();
    protected final Vector3f pos1Temp = new Vector3f();
    protected final Vector3f pos2Temp = new Vector3f();
    protected final Vector3f pos3Temp = new Vector3f();
    protected final Vector3f pos4Temp = new Vector3f();
    protected final Vector3f pos5Temp = new Vector3f();
    protected final Vector3f pos6Temp = new Vector3f();
    protected final Vector3f pos7Temp = new Vector3f();
    protected final Vector3f normalTemp = new Vector3f();
    protected final Vector3f originTemp = new Vector3f();
    protected final Cluster cluster;
    protected final @Nullable Level level;
    protected final boolean anchoredToSubLevel;

    public XRayBlockClusterOutline(Iterable<BlockPos> positions) {
        this(null, positions);
    }

    public XRayBlockClusterOutline(@Nullable Level level, Iterable<BlockPos> positions) {
        this.level = level;
        cluster = new Cluster();
        positions.forEach(cluster::include);
        anchoredToSubLevel = level != null && !cluster.isEmpty() && SubLevelBridge.resolve(level, Vec3.atCenterOf(cluster.anchor)).inSubLevel();
    }

    public static void loadFaceData(Direction face, Vector3f pos0, Vector3f pos1, Vector3f pos2, Vector3f pos3, Vector3f normal) {
        switch (face) {
            case DOWN -> {
                pos0.set(0, 0, 1);
                pos1.set(0, 0, 0);
                pos2.set(1, 0, 0);
                pos3.set(1, 0, 1);
                normal.set(0, -1, 0);
            }
            case UP -> {
                pos0.set(0, 1, 0);
                pos1.set(0, 1, 1);
                pos2.set(1, 1, 1);
                pos3.set(1, 1, 0);
                normal.set(0, 1, 0);
            }
            case NORTH -> {
                pos0.set(1, 1, 0);
                pos1.set(1, 0, 0);
                pos2.set(0, 0, 0);
                pos3.set(0, 1, 0);
                normal.set(0, 0, -1);
            }
            case SOUTH -> {
                pos0.set(0, 1, 1);
                pos1.set(0, 0, 1);
                pos2.set(1, 0, 1);
                pos3.set(1, 1, 1);
                normal.set(0, 0, 1);
            }
            case WEST -> {
                pos0.set(0, 1, 0);
                pos1.set(0, 0, 0);
                pos2.set(0, 0, 1);
                pos3.set(0, 1, 1);
                normal.set(-1, 0, 0);
            }
            case EAST -> {
                pos0.set(1, 1, 1);
                pos1.set(1, 0, 1);
                pos2.set(1, 0, 0);
                pos3.set(1, 1, 0);
                normal.set(1, 0, 0);
            }
        }
    }

    public static void addPos(float offsetX, float offsetY, float offsetZ, Vector3f pos0, Vector3f pos1, Vector3f pos2, Vector3f pos3) {
        pos0.add(offsetX, offsetY, offsetZ);
        pos1.add(offsetX, offsetY, offsetZ);
        pos2.add(offsetX, offsetY, offsetZ);
        pos3.add(offsetX, offsetY, offsetZ);
    }

    protected static Vec3 axisVector(Axis axis) {
        return switch (axis) {
            case X -> new Vec3(1, 0, 0);
            case Y -> new Vec3(0, 1, 0);
            case Z -> new Vec3(0, 0, 1);
        };
    }

    protected static Vec3 normalizeOrFallback(Vec3 vector, Vec3 fallback) {
        double lengthSqr = vector.lengthSqr();
        if (lengthSqr <= DIRECTION_LENGTH_SQR_EPSILON) {
            return fallback;
        }
        return vector.scale(1 / Math.sqrt(lengthSqr));
    }

    protected static Vector3f setPosition(Vector3f target, Vec3 position) {
        return target.set((float) position.x, (float) position.y, (float) position.z);
    }

    protected static Matrix4f createLinearTransformMatrix(CoordinateTransform transform, Vec3 localOrigin, Vec3 worldOrigin) {
        Vec3 axisX = transform.transformPosition(localOrigin.add(1, 0, 0)).subtract(worldOrigin);
        Vec3 axisY = transform.transformPosition(localOrigin.add(0, 1, 0)).subtract(worldOrigin);
        Vec3 axisZ = transform.transformPosition(localOrigin.add(0, 0, 1)).subtract(worldOrigin);
        return new Matrix4f((float) axisX.x, (float) axisX.y, (float) axisX.z, 0, (float) axisY.x, (float) axisY.y, (float) axisY.z, 0, (float) axisZ.x, (float) axisZ.y, (float) axisZ.z, 0, 0, 0, 0, 1);
    }

    @Override
    public void render(PoseStack poseStack, SuperRenderTypeBuffer buffer, Vec3 camera, float partialTicks) {
        params.loadColor(colorTemp);
        Vector4f outlineColor = colorTemp;
        int lightmap = params.lightmap;
        if (!anchoredToSubLevel || level == null || cluster.isEmpty()) {
            renderFaces(poseStack, buffer, camera, outlineColor, lightmap);
            renderEdges(poseStack, buffer, camera, outlineColor, lightmap, params.disableLineNormals);
            return;
        }

        CoordinateTransform transform = SubLevelBridge.createRenderTransform(level, Vec3.atCenterOf(cluster.anchor), partialTicks);
        if (!transform.inSubLevel()) {
            return;
        }

        renderTransformedFaces(poseStack, buffer, camera, outlineColor, lightmap, transform);
        renderTransformedEdges(poseStack, buffer, camera, outlineColor, lightmap, params.disableLineNormals, transform);
    }

    protected void renderFaces(PoseStack poseStack, SuperRenderTypeBuffer buffer, Vec3 camera, Vector4f color, int lightmap) {
        BindableTexture faceTexture = params.faceTexture;
        if (faceTexture == null || cluster.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(cluster.anchor.getX() - camera.x, cluster.anchor.getY() - camera.y, cluster.anchor.getZ() - camera.z);

        Pose pose = poseStack.last();
        RenderType renderType = PonderRenderTypes.outlineTranslucent(faceTexture.getLocation(), false);
        VertexConsumer consumer = buffer.getLateBuffer(renderType);
        cluster.visibleFaces.forEach((faceEntry, axisDirection) -> {
            Direction faceDirection = Direction.get(axisDirection, faceEntry.axis);
            BlockPos blockPos = axisDirection == AxisDirection.POSITIVE ? faceEntry.pos.relative(faceDirection.getOpposite()) : faceEntry.pos;
            bufferBlockFace(pose, consumer, blockPos, faceDirection, color, lightmap);
        });

        poseStack.popPose();
    }

    protected void renderEdges(PoseStack poseStack, SuperRenderTypeBuffer buffer, Vec3 camera, Vector4f color, int lightmap, boolean disableLineNormals) {
        float lineWidth = params.getLineWidth();
        if (lineWidth == 0 || cluster.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(cluster.anchor.getX() - camera.x, cluster.anchor.getY() - camera.y, cluster.anchor.getZ() - camera.z);

        Pose pose = poseStack.last();
        VertexConsumer consumer = buffer.getBuffer(CCBRenderTypes.SOLID_NO_DEPTH_TEST);
        cluster.visibleEdges.forEach(edgeEntry -> {
            BlockPos blockPos = edgeEntry.pos;
            Vector3f origin = originTemp;
            origin.set(blockPos.getX(), blockPos.getY(), blockPos.getZ());
            Direction edgeDirection = Direction.get(AxisDirection.POSITIVE, edgeEntry.axis);
            bufferCuboidLine(pose, consumer, origin, edgeDirection, 1, lineWidth, color, lightmap, disableLineNormals);
        });

        poseStack.popPose();
    }

    protected void renderTransformedFaces(PoseStack poseStack, SuperRenderTypeBuffer buffer, Vec3 camera, Vector4f color, int lightmap, CoordinateTransform transform) {
        BindableTexture faceTexture = params.faceTexture;
        if (faceTexture == null || cluster.isEmpty()) {
            return;
        }

        Vec3 localOrigin = new Vec3(cluster.anchor.getX(), cluster.anchor.getY(), cluster.anchor.getZ());
        Vec3 worldOrigin = transform.transformPosition(localOrigin);
        Matrix4f linearTransform = createLinearTransformMatrix(transform, localOrigin, worldOrigin);

        poseStack.pushPose();
        poseStack.translate(worldOrigin.x - camera.x, worldOrigin.y - camera.y, worldOrigin.z - camera.z);
        poseStack.mulPose(linearTransform);

        Pose pose = poseStack.last();
        RenderType renderType = PonderRenderTypes.outlineTranslucent(faceTexture.getLocation(), false);
        VertexConsumer consumer = buffer.getLateBuffer(renderType);
        cluster.visibleFaces.forEach((faceEntry, axisDirection) -> {
            Direction faceDirection = Direction.get(axisDirection, faceEntry.axis);
            BlockPos relativePos = axisDirection == AxisDirection.POSITIVE ? faceEntry.pos.relative(faceDirection.getOpposite()) : faceEntry.pos;
            bufferBlockFace(pose, consumer, relativePos, faceDirection, color, lightmap);
        });

        poseStack.popPose();
    }

    protected void renderTransformedEdges(PoseStack poseStack, SuperRenderTypeBuffer buffer, Vec3 camera, Vector4f color, int lightmap, boolean disableLineNormals, CoordinateTransform transform) {
        float lineWidth = params.getLineWidth();
        if (lineWidth == 0 || cluster.isEmpty()) {
            return;
        }

        Pose pose = poseStack.last();
        VertexConsumer consumer = buffer.getBuffer(CCBRenderTypes.SOLID_NO_DEPTH_TEST);
        cluster.visibleEdges.forEach(edgeEntry -> bufferTransformedEdge(pose, consumer, edgeEntry, lineWidth, color, lightmap, disableLineNormals, camera, transform));
    }

    protected void bufferTransformedEdge(Pose pose, VertexConsumer consumer, MergeEntry edgeEntry, float lineWidth, Vector4f color, int lightmap, boolean disableLineNormals, Vec3 camera, CoordinateTransform transform) {
        BlockPos relativePos = edgeEntry.pos;
        Vec3 localStart = new Vec3(cluster.anchor.getX() + relativePos.getX(), cluster.anchor.getY() + relativePos.getY(), cluster.anchor.getZ() + relativePos.getZ());
        Vec3 localDirection = axisVector(edgeEntry.axis);
        Vec3 worldStart = transform.transformPosition(localStart).subtract(camera);
        Vec3 worldEnd = transform.transformPosition(localStart.add(localDirection)).subtract(camera);
        Vec3 lineDirection = worldEnd.subtract(worldStart);
        if (lineDirection.lengthSqr() <= DIRECTION_LENGTH_SQR_EPSILON) {
            return;
        }

        lineDirection = lineDirection.normalize();
        Axis firstPerpendicularAxis = edgeEntry.axis == Axis.X ? Axis.Y : Axis.X;
        Axis secondPerpendicularAxis = edgeEntry.axis == Axis.Z ? Axis.Y : Axis.Z;
        Vec3 firstPerpendicular = normalizeOrFallback(transform.transformNormal(axisVector(firstPerpendicularAxis)), axisVector(firstPerpendicularAxis)).scale(lineWidth * 0.5);
        Vec3 secondPerpendicular = normalizeOrFallback(transform.transformNormal(axisVector(secondPerpendicularAxis)), axisVector(secondPerpendicularAxis)).scale(lineWidth * 0.5);
        Vector3f start0 = setPosition(pos0Temp, worldStart.subtract(firstPerpendicular).subtract(secondPerpendicular));
        Vector3f start1 = setPosition(pos1Temp, worldStart.subtract(firstPerpendicular).add(secondPerpendicular));
        Vector3f start2 = setPosition(pos2Temp, worldStart.add(firstPerpendicular).add(secondPerpendicular));
        Vector3f start3 = setPosition(pos3Temp, worldStart.add(firstPerpendicular).subtract(secondPerpendicular));
        Vector3f end0 = setPosition(pos4Temp, worldEnd.subtract(firstPerpendicular).subtract(secondPerpendicular));
        Vector3f end1 = setPosition(pos5Temp, worldEnd.subtract(firstPerpendicular).add(secondPerpendicular));
        Vector3f end2 = setPosition(pos6Temp, worldEnd.add(firstPerpendicular).add(secondPerpendicular));
        Vector3f end3 = setPosition(pos7Temp, worldEnd.add(firstPerpendicular).subtract(secondPerpendicular));
        bufferTransformedLineQuad(pose, consumer, start0, start1, start2, start3, lineDirection.scale(-1), color, lightmap, disableLineNormals);
        bufferTransformedLineQuad(pose, consumer, end3, end2, end1, end0, lineDirection, color, lightmap, disableLineNormals);
        bufferTransformedLineQuad(pose, consumer, start0, end0, end1, start1, firstPerpendicular.scale(-1), color, lightmap, disableLineNormals);
        bufferTransformedLineQuad(pose, consumer, start1, end1, end2, start2, secondPerpendicular, color, lightmap, disableLineNormals);
        bufferTransformedLineQuad(pose, consumer, start2, end2, end3, start3, firstPerpendicular, color, lightmap, disableLineNormals);
        bufferTransformedLineQuad(pose, consumer, start3, end3, end0, start0, secondPerpendicular.scale(-1), color, lightmap, disableLineNormals);
    }

    protected void bufferTransformedLineQuad(Pose pose, VertexConsumer consumer, Vector3f pos0, Vector3f pos1, Vector3f pos2, Vector3f pos3, Vec3 faceNormal, Vector4f color, int lightmap, boolean disableLineNormals) {
        Vec3 normalized = disableLineNormals ? new Vec3(0, 1, 0) : normalizeOrFallback(faceNormal, new Vec3(0, 1, 0));
        normalTemp.set((float) normalized.x, (float) normalized.y, (float) normalized.z);
        bufferQuad(pose, consumer, pos0, pos1, pos2, pos3, color, lightmap, normalTemp);
    }

    protected void bufferBlockFace(Pose pose, VertexConsumer consumer, BlockPos blockPos, Direction face, Vector4f color, int lightmap) {
        Vector3f pos0 = pos0Temp;
        Vector3f pos1 = pos1Temp;
        Vector3f pos2 = pos2Temp;
        Vector3f pos3 = pos3Temp;
        Vector3f normal = normalTemp;
        loadFaceData(face, pos0, pos1, pos2, pos3, normal);
        addPos(blockPos.getX() + face.getStepX() / 128.0f, blockPos.getY() + face.getStepY() / 128.0f, blockPos.getZ() + face.getStepZ() / 128.0f, pos0, pos1, pos2, pos3);
        bufferQuad(pose, consumer, pos0, pos1, pos2, pos3, color, lightmap, normal);
    }

    protected static class Cluster {
        private final Map<MergeEntry, AxisDirection> visibleFaces = new HashMap<>();
        private final Set<MergeEntry> visibleEdges = new HashSet<>();
        private BlockPos anchor;

        public boolean isEmpty() {
            return anchor == null;
        }

        public void include(BlockPos blockPos) {
            if (anchor == null) {
                anchor = blockPos;
            }

            BlockPos relativePos = blockPos.subtract(anchor);
            includeFaces(relativePos);
            includeEdges(relativePos);
        }

        private void includeFaces(BlockPos relativePos) {
            for (Axis axis : Iterate.axes) {
                Direction positiveDirection = Direction.get(AxisDirection.POSITIVE, axis);
                for (int faceOffset : Iterate.zeroAndOne) {
                    MergeEntry faceEntry = new MergeEntry(axis, relativePos.relative(positiveDirection, faceOffset));
                    if (visibleFaces.remove(faceEntry) != null) {
                        continue;
                    }

                    AxisDirection axisDirection = faceOffset == 0 ? AxisDirection.NEGATIVE : AxisDirection.POSITIVE;
                    visibleFaces.put(faceEntry, axisDirection);
                }
            }
        }

        private void includeEdges(BlockPos relativePos) {
            for (Axis edgeAxis : Iterate.axes) {
                for (Axis firstAxis : Iterate.axes) {
                    if (edgeAxis == firstAxis) {
                        continue;
                    }

                    for (Axis secondAxis : Iterate.axes) {
                        if (edgeAxis == secondAxis || firstAxis == secondAxis) {
                            continue;
                        }

                        Direction firstDirection = Direction.get(AxisDirection.POSITIVE, firstAxis);
                        Direction secondDirection = Direction.get(AxisDirection.POSITIVE, secondAxis);
                        includeEdgeOffsets(relativePos, edgeAxis, firstDirection, secondDirection);
                    }
                    break;
                }
            }
        }

        private void includeEdgeOffsets(BlockPos relativePos, Axis edgeAxis, Direction firstDirection, Direction secondDirection) {
            for (int firstOffset : Iterate.zeroAndOne) {
                BlockPos edgePos = relativePos.relative(firstDirection, firstOffset);
                for (int secondOffset : Iterate.zeroAndOne) {
                    edgePos = edgePos.relative(secondDirection, secondOffset);
                    MergeEntry edgeEntry = new MergeEntry(edgeAxis, edgePos);
                    if (visibleEdges.remove(edgeEntry)) {
                        continue;
                    }

                    visibleEdges.add(edgeEntry);
                }
            }
        }
    }

    protected record MergeEntry(Axis axis, BlockPos pos) {
        @Override
        public boolean equals(Object object) {
            return this == object || object instanceof MergeEntry(Axis otherAxis, BlockPos otherPos) && axis == otherAxis && pos.equals(otherPos);
        }

        @Override
        public int hashCode() {
            return pos.hashCode() * 31 + axis.ordinal();
        }
    }
}