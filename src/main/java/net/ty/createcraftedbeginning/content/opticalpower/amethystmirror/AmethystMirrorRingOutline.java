package net.ty.createcraftedbeginning.content.opticalpower.amethystmirror;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystMirrorRingOutline extends AmethystMirrorControlOutline {
    private static final int HIGH_SEGMENTS = 128;
    private static final int MEDIUM_SEGMENTS = 64;
    private static final int LOW_SEGMENTS = 32;
    private static final double MEDIUM_PIXEL_RADIUS = 200;
    private static final double LOW_PIXEL_RADIUS = 50;
    private static final double REDUCTION_RATIO = 0.8;
    private static final double MIN_LOD_DEPTH = 2;
    private static final int SECTION_CORNERS = 4;
    private final double radius;
    private final boolean horizontalAxis;
    private final Matrix4f projectionTransform = new Matrix4f();
    private int segments = HIGH_SEGMENTS;

    AmethystMirrorRingOutline(Vec3 center, double radius, boolean horizontalAxis) {
        super(center);
        this.radius = radius;
        this.horizontalAxis = horizontalAxis;
    }

    @Override
    MeshKey meshKey(float width) {
        return new RingMeshKey(radius, horizontalAxis, width, segments);
    }

    @Override
    boolean updateDetail(Matrix4f transform, float width) {
        double pixelRadius = projectedRadius(transform, width);
        int previous = segments;
        if (!Double.isFinite(pixelRadius) || pixelRadius >= MEDIUM_PIXEL_RADIUS) {
            segments = HIGH_SEGMENTS;
        }
        else if (pixelRadius >= LOW_PIXEL_RADIUS) {
            if (segments == LOW_SEGMENTS || pixelRadius <= MEDIUM_PIXEL_RADIUS * REDUCTION_RATIO) {
                segments = MEDIUM_SEGMENTS;
            }
        }
        else if (pixelRadius <= LOW_PIXEL_RADIUS * REDUCTION_RATIO) {
            segments = LOW_SEGMENTS;
        }
        else if (segments == HIGH_SEGMENTS) {
            segments = MEDIUM_SEGMENTS;
        }
        return previous != segments;
    }

    @Override
    List<Vec3> createVertices(float width) {
        double halfWidth = width / 2.0;
        Vec3[][] sections = new Vec3[segments][SECTION_CORNERS];
        for (int segment = 0; segment < segments; segment++) {
            double angle = 2 * Math.PI * segment / segments;
            double cosine = Math.cos(angle);
            double sine = Math.sin(angle);
            for (int corner = 0; corner < SECTION_CORNERS; corner++) {
                double distance = corner < 2 ? radius + halfWidth : radius - halfWidth;
                double depth = corner == 0 || corner == 3 ? -halfWidth : halfWidth;
                if (horizontalAxis) {
                    sections[segment][corner] = new Vec3(depth, distance * cosine, distance * sine);
                    continue;
                }

                sections[segment][corner] = new Vec3(distance * cosine, depth, -distance * sine);
            }
        }
        List<Vec3> vertices = new ArrayList<>(segments * SECTION_CORNERS * 4);
        for (int segment = 0; segment < segments; segment++) {
            Vec3[] start = sections[segment];
            Vec3[] end = sections[(segment + 1) % segments];
            for (int corner = 0; corner < SECTION_CORNERS; corner++) {
                int next = (corner + 1) % SECTION_CORNERS;
                vertices.add(start[corner]);
                vertices.add(end[corner]);
                vertices.add(end[next]);
                vertices.add(start[next]);
            }
        }
        return List.copyOf(vertices);
    }

    private double projectedRadius(Matrix4f transform, float width) {
        Matrix4f projection = RenderSystem.getProjectionMatrix();
        if (projection.m23() != -1 || projection.m33() != 0 || projection.m00() <= 0 || projection.m11() <= 0) {
            return Double.POSITIVE_INFINITY;
        }

        RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
        int viewWidth = target.viewWidth;
        int viewHeight = target.viewHeight;
        if (viewWidth <= 0 || viewHeight <= 0 || radius <= 0) {
            return Double.POSITIVE_INFINITY;
        }

        Matrix4f clip = projectionTransform.set(projection).mul(RenderSystem.getModelViewMatrix()).mul(transform);
        double centerW = clip.m33();
        if (centerW <= MIN_LOD_DEPTH) {
            return Double.POSITIVE_INFINITY;
        }

        int firstAxis = horizontalAxis ? 1 : 0;
        int depthAxis = horizontalAxis ? 0 : 1;
        double halfWidth = width / 2.0;
        double outerRadius = radius + halfWidth;
        double planeX = Math.hypot(clip.get(firstAxis, 0), clip.m20());
        double planeY = Math.hypot(clip.get(firstAxis, 1), clip.m21());
        double firstW = clip.get(firstAxis, 3);
        double secondW = clip.m23();
        double planeW = Math.hypot(firstW, secondW);
        double depthW = clip.get(depthAxis, 3);
        double minimumW = centerW - outerRadius * planeW - halfWidth * Math.abs(depthW);
        double nearFirst = clip.get(firstAxis, 2) + firstW;
        double nearSecond = clip.m22() + secondW;
        double nearDepth = clip.get(depthAxis, 2) + depthW;
        double minimumNear = clip.m32() + centerW - outerRadius * Math.hypot(nearFirst, nearSecond) - halfWidth * Math.abs(nearDepth);
        if (minimumW <= 0 || minimumNear <= 0) {
            return Double.POSITIVE_INFINITY;
        }

        double maximumX = Math.abs(clip.m30()) + outerRadius * planeX + halfWidth * Math.abs(clip.get(depthAxis, 0));
        double maximumY = Math.abs(clip.m31()) + outerRadius * planeY + halfWidth * Math.abs(clip.get(depthAxis, 1));
        double perspective = planeW / (minimumW * minimumW);
        double pixelsX = viewWidth / 2.0 * (planeX / minimumW + maximumX * perspective);
        double pixelsY = viewHeight / 2.0 * (planeY / minimumW + maximumY * perspective);
        return outerRadius * Math.hypot(pixelsX, pixelsY);
    }

    private record RingMeshKey(double radius, boolean horizontalAxis, float width, int segments) implements MeshKey {
    }
}
