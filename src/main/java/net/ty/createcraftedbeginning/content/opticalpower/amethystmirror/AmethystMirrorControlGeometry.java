package net.ty.createcraftedbeginning.content.opticalpower.amethystmirror;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystMirrorControlGeometry {
    private static final double HANDLE_DISTANCE = 0.5625;
    private static final double HANDLE_HALF_SIZE = 0.0625;
    static final int HANDLES_PER_AXIS = 2;
    static final int HANDLE_COUNT = 4;
    static final int HORIZONTAL_RING = HANDLE_COUNT;
    static final int VERTICAL_RING = HORIZONTAL_RING + 1;
    static final int RING_SEGMENTS = 64;
    private static final double RING_PICK_TOLERANCE = 0.03125;
    private static final float[] RING_COSINES = new float[RING_SEGMENTS + 1];
    private static final float[] RING_SINES = new float[RING_SEGMENTS + 1];
    private static final List<AABB> HANDLE_BOUNDS;
    private static final List<AABB> HORIZONTAL_RING_BOUNDS;
    private static final List<AABB> VERTICAL_RING_BOUNDS;
    static final AABB PICK_BOUNDS;

    static {
        List<AABB> handles = new ArrayList<>();
        for (int handle = 0; handle < HANDLE_COUNT; handle++) {
            double x = 0;
            double y = 0;
            if (handle < HANDLES_PER_AXIS) {
                x = handle == 0 ? -HANDLE_DISTANCE : HANDLE_DISTANCE;
            }
            else {
                y = handle == HANDLES_PER_AXIS ? -HANDLE_DISTANCE : HANDLE_DISTANCE;
            }
            handles.add(AABB.ofSize(new Vec3(x, y, 0), HANDLE_HALF_SIZE * 2, HANDLE_HALF_SIZE * 2, HANDLE_HALF_SIZE * 2));
        }
        HANDLE_BOUNDS = List.copyOf(handles);
        for (int segment = 0; segment < RING_SEGMENTS; segment++) {
            double angle = 2 * Math.PI * segment / RING_SEGMENTS;
            RING_COSINES[segment] = (float) (Math.cos(angle) * HANDLE_DISTANCE);
            RING_SINES[segment] = (float) (Math.sin(angle) * HANDLE_DISTANCE);
        }
        RING_COSINES[RING_SEGMENTS] = RING_COSINES[0];
        RING_SINES[RING_SEGMENTS] = RING_SINES[0];
        List<AABB> horizontal = new ArrayList<>();
        List<AABB> vertical = new ArrayList<>();
        for (int segment = 0; segment < RING_SEGMENTS; segment++) {
            double start = RING_COSINES[segment];
            double end = RING_COSINES[segment + 1];
            double startZ = RING_SINES[segment];
            double endZ = RING_SINES[segment + 1];
            horizontal.add(new AABB(0, start, startZ, 0, end, endZ).inflate(RING_PICK_TOLERANCE));
            vertical.add(new AABB(start, 0, startZ, end, 0, endZ).inflate(RING_PICK_TOLERANCE));
        }
        HORIZONTAL_RING_BOUNDS = List.copyOf(horizontal);
        VERTICAL_RING_BOUNDS = List.copyOf(vertical);
        AABB bounds = HANDLE_BOUNDS.getFirst();
        for (AABB handle : HANDLE_BOUNDS) {
            bounds = bounds.minmax(handle);
        }
        for (AABB segment : HORIZONTAL_RING_BOUNDS) {
            bounds = bounds.minmax(segment);
        }
        for (AABB segment : VERTICAL_RING_BOUNDS) {
            bounds = bounds.minmax(segment);
        }
        PICK_BOUNDS = bounds.inflate(AmethystMirrorGeometry.HIT_TOLERANCE);
    }

    private AmethystMirrorControlGeometry() {
    }

    static AABB handleBounds(int handle) {
        return HANDLE_BOUNDS.get(handle);
    }

    static List<AABB> ringBounds(int ring) {
        if (ring == HORIZONTAL_RING) {
            return HORIZONTAL_RING_BOUNDS;
        }

        return VERTICAL_RING_BOUNDS;
    }

    static float ringCosine() {
        return RING_COSINES[0];
    }
}
