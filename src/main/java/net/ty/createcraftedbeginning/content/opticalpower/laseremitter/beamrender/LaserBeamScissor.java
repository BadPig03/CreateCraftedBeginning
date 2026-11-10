package net.ty.createcraftedbeginning.content.opticalpower.laseremitter.beamrender;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
record LaserBeamScissor(int x, int y, int width, int height) {
    private static final int PIXEL_MARGIN = 2;
    private static final float CLIP_TOLERANCE = 1.0E-5F;

    static LaserBeamScissor project(AABB bounds, Matrix4f pose, Matrix4f modelView, Matrix4f projection, int width, int height) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        Vector3f position = new Vector3f();
        Vector4f clip = new Vector4f();
        for (int corner = 0; corner < 8; corner++) {
            float x = (float) ((corner & 1) == 0 ? bounds.minX : bounds.maxX);
            float y = (float) ((corner & 2) == 0 ? bounds.minY : bounds.maxY);
            float z = (float) ((corner & 4) == 0 ? bounds.minZ : bounds.maxZ);
            pose.transformPosition(x, y, z, position);
            clip.set(position, 1);
            modelView.transform(clip);
            projection.transform(clip);
            if (!clip.isFinite() || clip.w <= CLIP_TOLERANCE || clip.z + clip.w <= CLIP_TOLERANCE) {
                return new LaserBeamScissor(0, 0, width, height);
            }

            double screenX = (clip.x / (double) clip.w * 0.5 + 0.5) * width;
            double screenY = (clip.y / (double) clip.w * 0.5 + 0.5) * height;
            minX = Math.min(minX, screenX);
            minY = Math.min(minY, screenY);
            maxX = Math.max(maxX, screenX);
            maxY = Math.max(maxY, screenY);
        }
        int x = (int) Math.clamp(Math.floor(minX) - PIXEL_MARGIN, 0, width);
        int y = (int) Math.clamp(Math.floor(minY) - PIXEL_MARGIN, 0, height);
        int endX = (int) Math.clamp(Math.ceil(maxX) + PIXEL_MARGIN, 0, width);
        int endY = (int) Math.clamp(Math.ceil(maxY) + PIXEL_MARGIN, 0, height);
        return new LaserBeamScissor(x, y, endX - x, endY - y);
    }
}
