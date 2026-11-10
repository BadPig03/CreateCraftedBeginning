package net.ty.createcraftedbeginning.content.opticalpower.amethystmirror;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.createmod.catnip.outliner.Outline;
import net.createmod.catnip.render.PonderRenderTypes;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
abstract class AmethystMirrorControlOutline extends Outline {
    private static final int MAX_CACHED_MESHES = 32;
    private static final LinkedHashMap<MeshKey, Mesh> MESHES = new LinkedHashMap<>(MAX_CACHED_MESHES, 0.75F, true);

    private Vec3 center;
    private final Matrix4f orientation = new Matrix4f();
    private @Nullable Mesh mesh;
    private long[] transformed = new long[0];
    private float meshWidth = -1;

    AmethystMirrorControlOutline(Vec3 center) {
        this.center = center;
    }

    @Override
    public final void render(PoseStack pose, SuperRenderTypeBuffer buffers, Vec3 camera, float partialTicks) {
        float width = params.getLineWidth();
        if (width <= 0) {
            return;
        }

        params.loadColor(colorTemp);
        int color = ARGB32.color((int) (colorTemp.w * 255), (int) (colorTemp.x * 255), (int) (colorTemp.y * 255), (int) (colorTemp.z * 255));
        Vector3f normal = pose.last().normal().transform(new Vector3f(0, 1, 0));
        VertexConsumer consumer = buffers.getBuffer(PonderRenderTypes.outlineSolid());
        pose.pushPose();
        try {
            pose.translate(center.x - camera.x, center.y - camera.y, center.z - camera.z);
            pose.mulPose(orientation);
            Matrix4f transform = pose.last().pose();
            boolean detailChanged = updateDetail(transform, width);
            Mesh current = mesh;
            if (current == null || meshWidth != width || detailChanged) {
                MeshKey key = meshKey(width);
                current = key == null ? null : MESHES.get(key);
                if (current == null) {
                    current = createMesh(width);
                    if (key != null) {
                        if (MESHES.size() >= MAX_CACHED_MESHES) {
                            MESHES.pollFirstEntry();
                        }
                        MESHES.put(key, current);
                    }
                }
                mesh = current;
                int positionCount = current.sharedVertices.length / 3 * 2;
                if (transformed.length < positionCount) {
                    transformed = new long[positionCount];
                }
                meshWidth = width;
            }
            if (AmethystMirrorVertexWriter.write(consumer, current.sharedVertices, current.vertexOffsets, transformed, transform, color, normal)) {
                return;
            }

            Vector3f position = new Vector3f();
            float[] vertices = current.vertices;
            for (int index = 0; index < vertices.length; index += 3) {
                transform.transformPosition(vertices[index], vertices[index + 1], vertices[index + 2], position);
                consumer.addVertex(position.x, position.y, position.z, color, 0, 0, OverlayTexture.NO_OVERLAY, LightTexture.FULL_BRIGHT, normal.x, normal.y, normal.z);
            }
        }
        finally {
            pose.popPose();
        }
    }

    final void setRotation(Quaternionf rotation) {
        orientation.rotation(rotation);
    }

    final void setPose(AmethystMirrorControlPose pose) {
        center = pose.center();
        pose.apply(orientation);
    }

    abstract List<Vec3> createVertices(float width);

    boolean updateDetail(Matrix4f transform, float width) {
        return false;
    }

    @Nullable MeshKey meshKey(float width) {
        return null;
    }

    private Mesh createMesh(float width) {
        List<Vec3> points = createVertices(width);
        int vertexCount = points.size();
        float[] positions = new float[vertexCount * 3];
        for (int index = 0; index < vertexCount; index++) {
            Vec3 vertex = points.get(index);
            positions[index * 3] = (float) vertex.x;
            positions[index * 3 + 1] = (float) vertex.y;
            positions[index * 3 + 2] = (float) vertex.z;
        }

        float[] sharedPositions = new float[positions.length];
        Map<Vec3, Integer> shared = new IdentityHashMap<>();
        int[] offsets = new int[vertexCount];
        int count = 0;
        for (int index = 0; index < vertexCount; index++) {
            Vec3 vertex = points.get(index);
            Integer existing = shared.putIfAbsent(vertex, count);
            offsets[index] = (existing == null ? count : existing) * 2;
            if (existing != null) {
                continue;
            }

            sharedPositions[count * 3] = (float) vertex.x;
            sharedPositions[count * 3 + 1] = (float) vertex.y;
            sharedPositions[count * 3 + 2] = (float) vertex.z;
            count++;
        }
        return new Mesh(positions, Arrays.copyOf(sharedPositions, count * 3), offsets);
    }

    interface MeshKey {
    }

    private record Mesh(float[] vertices, float[] sharedVertices, int[] vertexOffsets) {
    }
}
