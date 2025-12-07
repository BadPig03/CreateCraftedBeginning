package net.ty.createcraftedbeginning.api.outliner;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.BindableTexture;
import net.createmod.catnip.render.PonderRenderTypes;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public class XRayBlockClusterOutline extends CCBOutline {
    protected final Vector3f pos0Temp = new Vector3f();
    protected final Vector3f pos1Temp = new Vector3f();
    protected final Vector3f pos2Temp = new Vector3f();
    protected final Vector3f pos3Temp = new Vector3f();
    protected final Vector3f normalTemp = new Vector3f();
    protected final Vector3f originTemp = new Vector3f();
    private final Cluster cluster;

    public XRayBlockClusterOutline(@NotNull Iterable<BlockPos> positions) {
        cluster = new Cluster();
        positions.forEach(cluster::include);
    }

    public static void loadFaceData(@NotNull Direction face, Vector3f pos0, Vector3f pos1, Vector3f pos2, Vector3f pos3, Vector3f normal) {
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

    public static void addPos(float x, float y, float z, @NotNull Vector3f pos0, @NotNull Vector3f pos1, @NotNull Vector3f pos2, @NotNull Vector3f pos3) {
        pos0.add(x, y, z);
        pos1.add(x, y, z);
        pos2.add(x, y, z);
        pos3.add(x, y, z);
    }

    @Override
    public void render(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, float pt) {
        params.loadColor(colorTemp);
        Vector4f color = colorTemp;
        int lightmap = params.lightmap;
        renderFaces(ms, buffer, camera, pt, color, lightmap);
        renderEdges(ms, buffer, camera, pt, color, lightmap, params.disableLineNormals);
    }

    protected void renderFaces(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, float pt, Vector4f color, int lightmap) {
        BindableTexture faceTexture = params.faceTexture;
        if (faceTexture == null || cluster.isEmpty()) {
            return;
        }

        ms.pushPose();
        ms.translate(cluster.anchor.getX() - camera.x, cluster.anchor.getY() - camera.y, cluster.anchor.getZ() - camera.z);

        Pose pose = ms.last();
        RenderType renderType = PonderRenderTypes.outlineTranslucent(faceTexture.getLocation(), false);
        VertexConsumer consumer = buffer.getLateBuffer(renderType);
        cluster.visibleFaces.forEach((face, axisDirection) -> {
            Direction direction = Direction.get(axisDirection, face.axis);
            BlockPos pos = face.pos;
            if (axisDirection == AxisDirection.POSITIVE) {
                pos = pos.relative(direction.getOpposite());
            }
            bufferBlockFace(pose, consumer, pos, direction, color, lightmap);
        });

        ms.popPose();
    }

    protected void renderEdges(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, float pt, Vector4f color, int lightmap, boolean disableNormals) {
        float lineWidth = params.getLineWidth();
        if (lineWidth == 0 || cluster.isEmpty()) {
            return;
        }

        ms.pushPose();
        ms.translate(cluster.anchor.getX() - camera.x, cluster.anchor.getY() - camera.y, cluster.anchor.getZ() - camera.z);

        Pose pose = ms.last();
        VertexConsumer consumer = buffer.getBuffer(CCBRenderTypes.SOLID_NO_DEPTH_TEST);
        cluster.visibleEdges.forEach(edge -> {
            BlockPos pos = edge.pos;
            Vector3f origin = originTemp;
            origin.set(pos.getX(), pos.getY(), pos.getZ());
            Direction direction = Direction.get(AxisDirection.POSITIVE, edge.axis);
            bufferCuboidLine(pose, consumer, origin, direction, 1, lineWidth, color, lightmap, disableNormals);
        });

        ms.popPose();
    }

    protected void bufferBlockFace(Pose pose, VertexConsumer consumer, @NotNull BlockPos pos, Direction face, Vector4f color, int lightmap) {
        Vector3f pos0 = pos0Temp;
        Vector3f pos1 = pos1Temp;
        Vector3f pos2 = pos2Temp;
        Vector3f pos3 = pos3Temp;
        Vector3f normal = normalTemp;
        loadFaceData(face, pos0, pos1, pos2, pos3, normal);
        addPos(pos.getX() + face.getStepX() / 128.0f, pos.getY() + face.getStepY() / 128.0f, pos.getZ() + face.getStepZ() / 128.0f, pos0, pos1, pos2, pos3);
        bufferQuad(pose, consumer, pos0, pos1, pos2, pos3, color, lightmap, normal);
    }

    private static class Cluster {
        private final Map<MergeEntry, AxisDirection> visibleFaces;
        private final Set<MergeEntry> visibleEdges;
        private BlockPos anchor;

        public Cluster() {
            visibleEdges = new HashSet<>();
            visibleFaces = new HashMap<>();
        }

        public boolean isEmpty() {
            return anchor == null;
        }

        public void include(BlockPos pos) {
            if (anchor == null) {
                anchor = pos;
            }

            pos = pos.subtract(anchor);
            for (Axis axis : Iterate.axes) {
                Direction direction = Direction.get(AxisDirection.POSITIVE, axis);
                for (int offset : Iterate.zeroAndOne) {
                    MergeEntry entry = new MergeEntry(axis, pos.relative(direction, offset));
                    if (visibleFaces.remove(entry) != null) {
                        continue;
                    }

                    visibleFaces.put(entry, offset == 0 ? AxisDirection.NEGATIVE : AxisDirection.POSITIVE);
                }
            }

            for (Axis axis : Iterate.axes) {
                for (Axis axis2 : Iterate.axes) {
                    if (axis == axis2) {
                        continue;
                    }

                    for (Axis axis3 : Iterate.axes) {
                        if (axis == axis3 || axis2 == axis3) {
                            continue;
                        }

                        Direction direction = Direction.get(AxisDirection.POSITIVE, axis2);
                        Direction direction2 = Direction.get(AxisDirection.POSITIVE, axis3);
                        for (int offset : Iterate.zeroAndOne) {
                            BlockPos entryPos = pos.relative(direction, offset);
                            for (int offset2 : Iterate.zeroAndOne) {
                                entryPos = entryPos.relative(direction2, offset2);
                                MergeEntry entry = new MergeEntry(axis, entryPos);
                                if (visibleEdges.remove(entry)) {
                                    continue;
                                }

                                visibleEdges.add(entry);
                            }
                        }
                    }
                    break;
                }
            }
        }
    }

    private record MergeEntry(Axis axis, BlockPos pos) {
        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof MergeEntry(Axis axis1, BlockPos pos1) && axis == axis1 && pos.equals(pos1);
        }

        @Override
        public int hashCode() {
            return pos.hashCode() * 31 + axis.ordinal();
        }
    }
}