package net.ty.createcraftedbeginning.compat.sable;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystMirrorRapierAccess {
    private static final int MAX_COLLIDER_HANDLE_EXCLUSIVE = 65535;
    private static final Map<BlockState, ArrayDeque<Integer>> AVAILABLE = new HashMap<>();
    private static final Method CREATE;
    private static final Method HANDLE;
    private static final Method ADD;
    private static final Method CLEAR;
    private static final Method FRICTION;
    private static final Method VOLUME;
    private static final Method RESTITUTION;
    private static final Method CALLBACK;
    private static final Method UPDATE_BOUNDS;
    private static final Method WAKE;

    static {
        try {
            Class<?> nativeApi = Class.forName("dev.ryanhcode.sable.physics.impl.rapier.Rapier3D");
            Class<?> callback = Class.forName("dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback");
            Class<?> pipeline = Class.forName("dev.ryanhcode.sable.api.physics.PhysicsPipeline");
            UPDATE_BOUNDS = pipeline.getMethod("onStatsChanged", Class.forName("dev.ryanhcode.sable.sublevel.ServerSubLevel"));
            WAKE = pipeline.getMethod("wakeUp", Class.forName("dev.ryanhcode.sable.api.physics.PhysicsPipelineBody"));
            Class<?> properties = Class.forName("dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper");
            CREATE = nativeApi.getMethod("createVoxelColliderEntry", double.class, double.class, double.class, boolean.class, callback);
            HANDLE = Class.forName("dev.ryanhcode.sable.physics.impl.rapier.collider.RapierVoxelColliderData").getMethod("handle");
            ADD = nativeApi.getMethod("addVoxelColliderBox", int.class, double[].class);
            CLEAR = nativeApi.getMethod("clearVoxelColliderBoxes", int.class);
            FRICTION = properties.getMethod("getFriction", BlockState.class);
            VOLUME = properties.getMethod("getVolume", BlockState.class);
            RESTITUTION = properties.getMethod("getRestitution", BlockState.class);
            CALLBACK = Class.forName("dev.ryanhcode.sable.api.block.BlockWithSubLevelCollisionCallback").getMethod("sable$getCallback", BlockState.class);
        }
        catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private AmethystMirrorRapierAccess() {
    }

    public static void refreshBody(Object pipeline, Object body) {
        try {
            UPDATE_BOUNDS.invoke(pipeline, body);
            WAKE.invoke(pipeline, body);
        }
        catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to refresh physical mirror bounds.", exception);
        }
    }

    static synchronized int acquire(BlockState material, List<AABB> boxes) {
        try {
            ArrayDeque<Integer> available = AVAILABLE.computeIfAbsent(material, ignored -> new ArrayDeque<>());
            Integer reused = available.pollFirst();
            int handle;
            if (reused == null) {
                double friction = ((Number) FRICTION.invoke(null, material)).doubleValue();
                double volume = ((Number) VOLUME.invoke(null, material)).doubleValue();
                double restitution = ((Number) RESTITUTION.invoke(null, material)).doubleValue();
                Object data = CREATE.invoke(null, friction, volume, restitution, false, CALLBACK.invoke(null, material));
                handle = (int) HANDLE.invoke(data);
                if (handle < 0 || handle >= MAX_COLLIDER_HANDLE_EXCLUSIVE) {
                    throw new IllegalStateException("Mirror collider exceeded the Rapier block handle range: " + handle + '.');
                }
            }
            else {
                handle = reused;
                CLEAR.invoke(null, handle);
            }
            for (AABB box : boxes) {
                ADD.invoke(null, handle, new double[]{box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ});
            }
            return handle;
        }
        catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to upload a mirror collider to Rapier.", exception);
        }
    }

    static synchronized void release(BlockState material, int handle) {
        AVAILABLE.computeIfAbsent(material, ignored -> new ArrayDeque<>()).addLast(handle);
    }
}
