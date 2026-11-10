package net.ty.createcraftedbeginning.content.opticalpower.amethystmirror;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.FastColor.ABGR32;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.platform.access.client.AmethystMirrorBufferAccess;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.ByteOrder;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystMirrorVertexWriter {
    private static final int VERTEX_SIZE = 36;
    private static final long PACKED_FULL_BRIGHT = 67555025202708480L;
    private static final boolean LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;

    private AmethystMirrorVertexWriter() {
    }

    static boolean write(VertexConsumer consumer, float[] vertices, int[] offsets, long[] positions, Matrix4f transform, int color, Vector3f normal) {
        if (!LITTLE_ENDIAN || DefaultVertexFormat.NEW_ENTITY.getVertexSize() != VERTEX_SIZE || !(consumer instanceof AmethystMirrorBufferAccess access)) {
            return false;
        }

        long pointer = access.ccb$reserveMirrorVertices(offsets.length);
        if (pointer == -1) {
            return false;
        }

        long packedColor = (long) ABGR32.fromArgb32(color) << 32;
        int normalX = (byte) (Mth.clamp(normal.x, -1, 1) * 127) & 255;
        int normalY = (byte) (Mth.clamp(normal.y, -1, 1) * 127) & 255;
        int normalZ = (byte) (Mth.clamp(normal.z, -1, 1) * 127) & 255;
        int packedNormal = normalX | normalY << 8 | normalZ << 16;
        long packedLight = PACKED_FULL_BRIGHT | OverlayTexture.NO_OVERLAY & 0xFFFFFFFFL;
        Vector3f position = new Vector3f();
        for (int index = 0; index < vertices.length; index += 3) {
            transform.transformPosition(vertices[index], vertices[index + 1], vertices[index + 2], position);
            long packedPosition = (long) Float.floatToRawIntBits(position.y) << 32 | Float.floatToRawIntBits(position.x) & 0xFFFFFFFFL;
            int offset = index / 3 * 2;
            positions[offset] = packedPosition;
            positions[offset + 1] = Float.floatToRawIntBits(position.z) & 0xFFFFFFFFL;
        }
        for (int offset : offsets) {
            MemoryUtil.memPutLong(pointer, positions[offset]);
            MemoryUtil.memPutLong(pointer + 8, packedColor | positions[offset + 1]);
            MemoryUtil.memPutLong(pointer + 16, 0);
            MemoryUtil.memPutLong(pointer + 24, packedLight);
            MemoryUtil.memPutInt(pointer + 32, packedNormal);
            pointer += VERTEX_SIZE;
        }
        return true;
    }
}
