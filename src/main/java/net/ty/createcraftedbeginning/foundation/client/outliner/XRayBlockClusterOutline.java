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
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
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
    protected final Vector3f pos0Temp = new Vector3f();
    protected final Vector3f pos1Temp = new Vector3f();
    protected final Vector3f pos2Temp = new Vector3f();
    protected final Vector3f pos3Temp = new Vector3f();
    protected final Vector3f normalTemp = new Vector3f();
    protected final Vector3f originTemp = new Vector3f();
    protected final Cluster cluster;

    public XRayBlockClusterOutline(Iterable<BlockPos> positions) {
        cluster = new Cluster();
        positions.forEach(cluster::include);
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

    @Override
    public void render(PoseStack poseStack, SuperRenderTypeBuffer buffer, Vec3 camera, float partialTicks) {
        params.loadColor(colorTemp);
        Vector4f color = colorTemp;
        int lightmap = params.lightmap;
        renderFaces(poseStack, buffer, camera, color, lightmap);
        renderEdges(poseStack, buffer, camera, color, lightmap, params.disableLineNormals);
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
        cluster.visibleFaces.forEach((face, axisDirection) -> {
            Direction direction = Direction.get(axisDirection, face.axis);
            BlockPos pos = axisDirection == AxisDirection.POSITIVE ? face.pos.relative(direction.getOpposite()) : face.pos;
            bufferBlockFace(pose, consumer, pos, direction, color, lightmap);
        });

        poseStack.popPose();
    }

    protected void renderEdges(PoseStack poseStack, SuperRenderTypeBuffer buffer, Vec3 camera, Vector4f color, int lightmap, boolean disableNormals) {
        float lineWidth = params.getLineWidth();
        if (lineWidth == 0 || cluster.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(cluster.anchor.getX() - camera.x, cluster.anchor.getY() - camera.y, cluster.anchor.getZ() - camera.z);

        Pose pose = poseStack.last();
        VertexConsumer consumer = buffer.getBuffer(CCBRenderTypes.SOLID_NO_DEPTH_TEST);
        cluster.visibleEdges.forEach(edge -> {
            BlockPos pos = edge.pos;
            Vector3f origin = originTemp;
            origin.set(pos.getX(), pos.getY(), pos.getZ());
            Direction direction = Direction.get(AxisDirection.POSITIVE, edge.axis);
            bufferCuboidLine(pose, consumer, origin, direction, 1, lineWidth, color, lightmap, disableNormals);
        });

        poseStack.popPose();
    }

    protected void bufferBlockFace(Pose pose, VertexConsumer consumer, BlockPos pos, Direction face, Vector4f color, int lightmap) {
        Vector3f pos0 = pos0Temp;
        Vector3f pos1 = pos1Temp;
        Vector3f pos2 = pos2Temp;
        Vector3f pos3 = pos3Temp;
        Vector3f normal = normalTemp;
        loadFaceData(face, pos0, pos1, pos2, pos3, normal);
        addPos(pos.getX() + face.getStepX() / 128.0f, pos.getY() + face.getStepY() / 128.0f, pos.getZ() + face.getStepZ() / 128.0f, pos0, pos1, pos2, pos3);
        bufferQuad(pose, consumer, pos0, pos1, pos2, pos3, color, lightmap, normal);
    }

    protected static class Cluster {
        private final Map<MergeEntry, AxisDirection> visibleFaces = new HashMap<>();
        private final Set<MergeEntry> visibleEdges = new HashSet<>();
        private BlockPos anchor;

        public boolean isEmpty() {
            return anchor == null;
        }

        public void include(BlockPos pos) {
            if (anchor == null) {
                anchor = pos;
            }

            BlockPos relativePos = pos.subtract(anchor);
            includeFaces(relativePos);
            includeEdges(relativePos);
        }

        private void includeFaces(BlockPos pos) {
            for (Axis axis : Iterate.axes) {
                Direction direction = Direction.get(AxisDirection.POSITIVE, axis);
                for (int offset : Iterate.zeroAndOne) {
                    MergeEntry entry = new MergeEntry(axis, pos.relative(direction, offset));
                    if (visibleFaces.remove(entry) != null) {
                        continue;
                    }

                    AxisDirection axisDirection = offset == 0 ? AxisDirection.NEGATIVE : AxisDirection.POSITIVE;
                    visibleFaces.put(entry, axisDirection);
                }
            }
        }

        private void includeEdges(BlockPos pos) {
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
                        includeEdgeOffsets(pos, edgeAxis, firstDirection, secondDirection);
                    }
                    break;
                }
            }
        }

        private void includeEdgeOffsets(BlockPos pos, Axis edgeAxis, Direction firstDirection, Direction secondDirection) {
            for (int firstOffset : Iterate.zeroAndOne) {
                BlockPos entryPos = pos.relative(firstDirection, firstOffset);
                for (int secondOffset : Iterate.zeroAndOne) {
                    entryPos = entryPos.relative(secondDirection, secondOffset);
                    MergeEntry entry = new MergeEntry(edgeAxis, entryPos);
                    if (visibleEdges.remove(entry)) {
                        continue;
                    }

                    visibleEdges.add(entry);
                }
            }
        }
    }

    protected record MergeEntry(Axis axis, BlockPos pos) {
        @Override
        public boolean equals(Object object) {
            return this == object || object instanceof MergeEntry(Axis axis1, BlockPos pos1) && axis == axis1 && pos.equals(pos1);
        }

        @Override
        public int hashCode() {
            return pos.hashCode() * 31 + axis.ordinal();
        }
    }
}