package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionHandler;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserQuerySpace.Pose;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LaserContraptionBounds {
    private static final int MAX_ENTRIES = 4096;
    private static final int MAX_RETAINED_ENTRIES = 128;
    private static Map<WeakReference<AbstractContraptionEntity>, Entry> entries = new IdentityHashMap<>();
    private static @Nullable Object frame;

    private LaserContraptionBounds() {}

    static MountedGeometry mounted(Level level, WeakReference<AbstractContraptionEntity> reference, AbstractContraptionEntity entity, AABB local, float partialTick) {
        Entry entry = prepare(level, reference, entity, local, partialTick);
        MountedGeometry cached = entry.mounted;
        if (cached != null) {
            return cached;
        }

        Vec3 pivot = new Vec3(0.5, 0.5, 0.5);
        Vec3 worldPivot = entry.previous.lerp(entry.anchor, partialTick).add(pivot);
        Vec3 x = entity.applyRotation(new Vec3(1, 0, 0), partialTick);
        Vec3 y = entity.applyRotation(new Vec3(0, 1, 0), partialTick);
        Vec3 z = entity.applyRotation(new Vec3(0, 0, 1), partialTick);
        double determinant = x.dot(y.cross(z));
        Pose pose = new Pose(local, pivot, worldPivot, x, y, z, y.cross(z).scale(1 / determinant), z.cross(x).scale(1 / determinant), x.cross(y).scale(1 / determinant));
        AABB expanded = local.inflate(LaserBlockClip.NEIGHBOR_REACH);
        Vec3 delta = expanded.getCenter().subtract(pivot);
        Vec3 center = worldPivot.add(x.scale(delta.x)).add(y.scale(delta.y)).add(z.scale(delta.z));
        Vec3 extentX = x.scale(expanded.getXsize() / 2);
        Vec3 extentY = y.scale(expanded.getYsize() / 2);
        Vec3 extentZ = z.scale(expanded.getZsize() / 2);
        AABB world = AABB.ofSize(center, 2 * (Math.abs(extentX.x) + Math.abs(extentY.x) + Math.abs(extentZ.x)), 2 * (Math.abs(extentX.y) + Math.abs(extentY.y) + Math.abs(extentZ.y)), 2 * (Math.abs(extentX.z) + Math.abs(extentY.z) + Math.abs(extentZ.z)));
        MountedGeometry result = new MountedGeometry(pose, world);
        entry.mounted = result;
        return result;
    }

    static boolean isClear(Level level, List<AABB> paths, float partialTick) {
        for (WeakReference<AbstractContraptionEntity> reference : ContraptionHandler.loadedContraptions.get(level).values()) {
            AbstractContraptionEntity entity = reference.get();
            if (entity == null || !entity.isAlive()) {
                continue;
            }

            Contraption contraption = entity.getContraption();
            if (contraption == null || contraption.bounds == null) {
                continue;
            }

            Entry entry = prepare(level, reference, entity, contraption.bounds, partialTick);
            AABB posed = entry.world;
            if (posed == null) {
                AABB local = entry.local.inflate(LaserBlockClip.NEIGHBOR_REACH);
                Vec3 anchor = entry.anchor;
                Vec3 offset = entry.previous.lerp(anchor, partialTick).subtract(anchor);
                Vec3 center = entity.toGlobalVector(local.getCenter(), partialTick).add(offset);
                Vec3 x = entity.applyRotation(new Vec3(local.getXsize() * 0.5, 0, 0), partialTick);
                Vec3 y = entity.applyRotation(new Vec3(0, local.getYsize() * 0.5, 0), partialTick);
                Vec3 z = entity.applyRotation(new Vec3(0, 0, local.getZsize() * 0.5), partialTick);
                posed = AABB.ofSize(center, 2 * (Math.abs(x.x) + Math.abs(y.x) + Math.abs(z.x)), 2 * (Math.abs(x.y) + Math.abs(y.y) + Math.abs(z.y)), 2 * (Math.abs(x.z) + Math.abs(y.z) + Math.abs(z.z)));
                entry.world = posed;
            }
            for (AABB query : paths) {
                if (posed.intersects(query)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static Entry prepare(Level level, WeakReference<AbstractContraptionEntity> reference, AbstractContraptionEntity entity, AABB bounds, float partialTick) {
        Object current = LaserRenderFrame.current(level, partialTick);
        if (current != null && frame != current) {
            if (entries.size() > MAX_RETAINED_ENTRIES) {
                entries = new IdentityHashMap<>();
            }
            else {
                entries.clear();
            }
            frame = current;
        }
        Rotation rotation = null;
        Class<?> entityClass = entity.getClass();
        if (current != null && entityClass == ControlledContraptionEntity.class) {
            ControlledContraptionEntity controlled = (ControlledContraptionEntity) entity;
            rotation = new Rotation(controlled.getRotationAxis(), controlled.getAngle(partialTick), 0, 0);
        }
        else if (current != null && entityClass == OrientedContraptionEntity.class) {
            OrientedContraptionEntity oriented = (OrientedContraptionEntity) entity;
            rotation = new Rotation(null, oriented.getInitialYaw(), oriented.getViewXRot(partialTick), oriented.getViewYRot(partialTick));
        }
        Vec3 anchor = entity.getAnchorVec();
        Vec3 previous = entity.getPrevAnchorVec();
        boolean shared = current != null && rotation != null;
        Entry cached = shared ? entries.get(reference) : null;
        if (cached != null && cached.local.equals(bounds) && cached.anchor.equals(anchor) && cached.previous.equals(previous) && rotation.equals(cached.rotation)) {
            return cached;
        }

        Entry result = new Entry(bounds, anchor, previous, rotation);
        if (shared && entries.size() < MAX_ENTRIES) {
            entries.put(reference, result);
        }
        return result;
    }

    record MountedGeometry(Pose pose, AABB world) {}

    private record Rotation(@Nullable Axis axis, float first, float second, float third) {}

    private static final class Entry {
        private final AABB local;
        private final Vec3 anchor;
        private final Vec3 previous;
        private final @Nullable Rotation rotation;
        private @Nullable AABB world;
        private @Nullable MountedGeometry mounted;

        private Entry(AABB local, Vec3 anchor, Vec3 previous, @Nullable Rotation rotation) {
            this.local = local;
            this.anchor = anchor;
            this.previous = previous;
            this.rotation = rotation;
        }
    }
}
