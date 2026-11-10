package net.ty.createcraftedbeginning.compat.sable;

import dev.ryanhcode.sable.companion.ClientSubLevelAccess;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserBlockClip;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserRenderFrame;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class SableRenderBounds {
    private static final int MAX_ENTRIES = 256;
    private static final Map<UUID, Geometry> geometries = new HashMap<>();
    private static @Nullable Object frame;
    private static final MethodHandle CONTAINER;
    private static final MethodHandle OWNERS;
    private static final MethodHandle PLOT;
    private static final MethodHandle BOUNDS;

    static {
        try {
            Class<?> containers = Class.forName("dev.ryanhcode.sable.api.sublevel.SubLevelContainer");
            Class<?> subLevel = Class.forName("dev.ryanhcode.sable.sublevel.SubLevel");
            Class<?> plot = Class.forName("dev.ryanhcode.sable.sublevel.plot.LevelPlot");
            CONTAINER = MethodHandles.publicLookup().findStatic(containers, "getContainer", MethodType.methodType(containers, Level.class)).asType(MethodType.methodType(Object.class, Level.class));
            OWNERS = MethodHandles.publicLookup().findVirtual(containers, "getAllSubLevels", MethodType.methodType(List.class)).asType(MethodType.methodType(List.class, Object.class));
            PLOT = MethodHandles.publicLookup().findVirtual(subLevel, "getPlot", MethodType.methodType(plot)).asType(MethodType.methodType(Object.class, SubLevelAccess.class));
            BOUNDS = MethodHandles.publicLookup().findVirtual(plot, "getBoundingBox", MethodType.methodType(BoundingBox3ic.class)).asType(MethodType.methodType(BoundingBox3ic.class, Object.class));
        }
        catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Failed to resolve Sable optical bounds access.", failure);
        }
    }

    private SableRenderBounds() {}

    static @Nullable AABB localBounds(SubLevelAccess owner) {
        try {
            Object plot = PLOT.invokeExact(owner);
            BoundingBox3ic bounds = (BoundingBox3ic) BOUNDS.invokeExact(plot);
            if (bounds == null) {
                return null;
            }

            return new AABB(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX() + 1.0, bounds.maxY() + 1.0, bounds.maxZ() + 1.0);
        }
        catch (Throwable failure) {
            throw new IllegalStateException("Failed to read Sable optical local bounds.", failure);
        }
    }

    static List<Region> query(Level level, List<AABB> queries, float partialTick) {
        Object current = LaserRenderFrame.current(level, partialTick);
        if (current != null && current != frame) {
            geometries.clear();
            frame = current;
        }
        try {
            Object container = CONTAINER.invokeExact(level);
            if (container == null) {
                return List.of();
            }

            List<?> owners = (List<?>) OWNERS.invokeExact(container);
            List<Region> result = new ArrayList<>();
            for (Object value : owners) {
                SubLevelAccess owner = (SubLevelAccess) value;
                AABB bounds = localBounds(owner);
                if (bounds == null) {
                    continue;
                }

                AABB local = bounds.inflate(LaserBlockClip.NEIGHBOR_REACH);
                Pose3dc pose = owner.logicalPose();
                if (owner instanceof ClientSubLevelAccess client) {
                    pose = client.renderPose(partialTick);
                }
                UUID id = owner.getUniqueId();
                Geometry geometry = current == null ? null : geometries.get(id);
                if (geometry == null || !geometry.matches(local, pose)) {
                    Pose3d snapshot = new Pose3d(pose);
                    AABB world = new BoundingBox3d(local).transform(snapshot, new BoundingBox3d()).toMojang();
                    geometry = new Geometry(local, snapshot, world);
                    if (current != null && geometries.size() < MAX_ENTRIES) {
                        geometries.put(id, geometry);
                    }
                }
                AABB world = geometry.world();
                for (AABB query : queries) {
                    if (!world.intersects(query)) {
                        continue;
                    }

                    result.add(new Region(owner, local, geometry.pose(), world));
                    break;
                }
            }
            return List.copyOf(result);
        }
        catch (Throwable failure) {
            throw new IllegalStateException("Failed to query Sable optical render bounds.", failure);
        }
    }

    record Region(SubLevelAccess owner, AABB local, Pose3dc pose, AABB world) {}

    private record Geometry(AABB local, Pose3d pose, AABB world) {
        private boolean matches(AABB bounds, Pose3dc current) {
            return local.equals(bounds) && pose.position().equals(current.position()) && pose.rotationPoint().equals(current.rotationPoint()) && pose.scale().equals(current.scale()) && pose.orientation().equals(current.orientation());
        }
    }
}
