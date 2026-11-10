package net.ty.createcraftedbeginning.compat.sable;

import dev.ryanhcode.sable.companion.ClientSubLevelAccess;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.compat.sable.SableRenderBounds.Region;
import net.ty.createcraftedbeginning.platform.SkyLightCollisionContext;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.CoordinateTransform;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.EntityArea;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.LocalRegion;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.Projection;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.RayProjection;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.RenderGeometry;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.RenderSource;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.Service;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniondc;
import org.joml.Vector3dc;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class SableSubLevelCompat implements Service {
    private static final int MAX_RENDER_SUBLEVELS = 256;
    private static final double SAT_EPSILON = 1.0E-7;
    private static final double SKY_COLUMN_EPSILON = 1.0E-4;
    private static final double SKY_RAY_EPSILON = 1.0E-4;
    private static final Vec3 X_AXIS = new Vec3(1, 0, 0);
    private static final Vec3 Y_AXIS = new Vec3(0, 1, 0);
    private static final Vec3 Z_AXIS = new Vec3(0, 0, 1);
    private static final MethodHandle TRACKING_PLAYERS;

    static {
        try {
            Class<?> owner = Class.forName("dev.ryanhcode.sable.sublevel.ServerSubLevel");
            TRACKING_PLAYERS = MethodHandles.publicLookup().findVirtual(owner, "getTrackingPlayers", MethodType.methodType(Collection.class)).asType(MethodType.methodType(Collection.class, SubLevelAccess.class));
        }
        catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Failed to resolve Sable tracking players.", failure);
        }
    }

    private SableSubLevelCompat() {
    }

    public static void register() {
        SubLevelBridge.install(new SableSubLevelCompat());
        SableAmethystLoaderTickets.register();
        SableLoadingNetwork.register();
    }

    private static RenderGeometry captureRenderGeometry(SubLevelAccess subLevel, Pose3dc pose) {
        Vector3dc position = pose.position();
        Vector3dc pivot = pose.rotationPoint();
        Vector3dc scale = pose.scale();
        Quaterniondc rotation = pose.orientation();
        return new RenderGeometry(new BoundingBox3d(subLevel.boundingBox()).toMojang(), new Vec3(position.x(), position.y(), position.z()), new Vec3(pivot.x(), pivot.y(), pivot.z()), new Vec3(scale.x(), scale.y(), scale.z()), rotation.x(), rotation.y(), rotation.z(), rotation.w());
    }

    private static CoordinateTransform createTransform(Pose3dc pose) {
        return new CoordinateTransform() {
            @Override
            public Vec3 transformPosition(Position position) {
                return pose.transformPosition(new Vec3(position.x(), position.y(), position.z()));
            }

            @Override
            public Vec3 transformNormal(Position normal) {
                return pose.transformNormal(new Vec3(normal.x(), normal.y(), normal.z()));
            }

            @Override
            public boolean inSubLevel() {
                return true;
            }
        };
    }

    private static OrientedBox createOrientedBox(AABB bounds, @Nullable SubLevelAccess subLevel) {
        Vec3 center = bounds.getCenter();
        double extentX = (bounds.maxX - bounds.minX) * 0.5;
        double extentY = (bounds.maxY - bounds.minY) * 0.5;
        double extentZ = (bounds.maxZ - bounds.minZ) * 0.5;
        if (subLevel == null) {
            return new OrientedBox(center, X_AXIS, Y_AXIS, Z_AXIS, extentX, extentY, extentZ);
        }

        Pose3dc pose = subLevel.logicalPose();
        Vec3 transformedX = pose.transformNormal(X_AXIS);
        Vec3 transformedY = pose.transformNormal(Y_AXIS);
        Vec3 transformedZ = pose.transformNormal(Z_AXIS);
        double scaleX = transformedX.length();
        double scaleY = transformedY.length();
        double scaleZ = transformedZ.length();
        return new OrientedBox(pose.transformPosition(center), normalizeOrFallback(transformedX, X_AXIS), normalizeOrFallback(transformedY, Y_AXIS), normalizeOrFallback(transformedZ, Z_AXIS), extentX * scaleX, extentY * scaleY, extentZ * scaleZ);
    }

    private static Vec3 normalizeOrFallback(Vec3 vector, Vec3 fallback) {
        double length = vector.length();
        if (length <= SAT_EPSILON) {
            return fallback;
        }

        return vector.scale(1 / length);
    }

    private static BoundingBox3d createSkyColumn(Level level, Vec3 position) {
        int maxBuildHeight = level.getMaxBuildHeight();
        double worldHeight = maxBuildHeight - level.getMinBuildHeight();
        double searchTop = Math.max(maxBuildHeight + 1, position.y + worldHeight);
        return new BoundingBox3d(position.x - SKY_COLUMN_EPSILON, position.y + SKY_RAY_EPSILON, position.z - SKY_COLUMN_EPSILON, position.x + SKY_COLUMN_EPSILON, searchTop, position.z + SKY_COLUMN_EPSILON);
    }

    @Override
    public @Nullable UUID subLevelId(Level level, BlockPos pos) {
        SubLevelAccess owner = SableCompanion.INSTANCE.getContaining(level, pos);
        if (owner == null) {
            return null;
        }

        return owner.getUniqueId();
    }

    @Override
    public List<ServerPlayer> trackingPlayers(ServerLevel level, BlockPos pos) {
        SubLevelAccess owner = SableCompanion.INSTANCE.getContaining(level, pos);
        if (owner == null) {
            return Service.super.trackingPlayers(level, pos);
        }

        try {
            Collection<?> tracked = (Collection<?>) TRACKING_PLAYERS.invokeExact(owner);
            return level.players().stream().filter(player -> tracked.contains(player.getUUID())).toList();
        }
        catch (Throwable failure) {
            throw new IllegalStateException("Failed to read Sable tracking players.", failure);
        }
    }

    @Override
    public <T> @Nullable T findAt(Level level, BlockPos origin, BlockPos sample, Function<BlockPos, @Nullable T> query) {
        SableCompanion companion = SableCompanion.INSTANCE;
        SubLevelAccess source = companion.getContaining(level, origin);
        Position sampleCenter = Vec3.atCenterOf(sample);
        return companion.runIncludingSubLevels(level, sampleCenter, true, source, (subLevel, pos) -> query.apply(pos));
    }

    @Override
    public boolean testLocalFrames(Level level, Vec3 position, Vec3 direction, double radius, BiPredicate<Vec3, Vec3> query) {
        if (query.test(position, direction)) {
            return true;
        }

        BoundingBox3d bounds = new BoundingBox3d(new AABB(position, position).inflate(radius));
        for (SubLevelAccess subLevel : SableCompanion.INSTANCE.getAllIntersecting(level, bounds)) {
            Pose3dc pose = subLevel.logicalPose();
            if (!query.test(pose.transformPositionInverse(position), pose.transformNormalInverse(direction))) {
                continue;
            }

            return true;
        }
        return false;
    }

    @Override
    public boolean testLocalBounds(Level level, AABB bounds, Predicate<AABB> query) {
        if (query.test(bounds)) {
            return true;
        }

        BoundingBox3d worldBounds = new BoundingBox3d(bounds);
        for (SubLevelAccess subLevel : SableCompanion.INSTANCE.getAllIntersecting(level, worldBounds)) {
            AABB localBounds = worldBounds.transformInverse(subLevel.logicalPose(), new BoundingBox3d()).toMojang();
            if (!query.test(localBounds)) {
                continue;
            }

            return true;
        }
        return false;
    }

    @Override
    public List<LocalRegion> localRegions(Level level, AABB bounds) {
        return localRegions(level, bounds, 1, false);
    }

    @Override
    public List<LocalRegion> localRegions(Level level, AABB bounds, float partialTick, boolean rendering) {
        List<LocalRegion> regions = new ArrayList<>(Service.super.localRegions(level, bounds));
        BoundingBox3d worldBounds = new BoundingBox3d(bounds);
        if (rendering && level.isClientSide) {
            for (Region region : SableRenderBounds.query(level, List.of(bounds), partialTick)) {
                Pose3dc pose = region.pose();
                AABB localBounds = worldBounds.transformInverse(pose, new BoundingBox3d()).toMojang().intersect(region.local());
                Vector3dc pivot = pose.rotationPoint();
                Vector3dc position = pose.position();
                regions.add(new LocalRegion(localBounds, pose.transformPosition(Vec3.ZERO), pose.transformNormal(X_AXIS), pose.transformNormal(Y_AXIS), pose.transformNormal(Z_AXIS), new Vec3(pivot.x(), pivot.y(), pivot.z()), new Vec3(position.x(), position.y(), position.z())));
            }
            return List.copyOf(regions);
        }

        for (SubLevelAccess subLevel : SableCompanion.INSTANCE.getAllIntersecting(level, worldBounds)) {
            Pose3dc logicalPose = subLevel.logicalPose();
            Pose3dc pose = logicalPose;
            if (rendering && subLevel instanceof ClientSubLevelAccess client) {
                pose = client.renderPose(partialTick);
            }
            AABB localBounds = worldBounds.transformInverse(pose, new BoundingBox3d()).toMojang();
            AABB nativeBounds = new BoundingBox3d(subLevel.boundingBox()).transformInverse(logicalPose, new BoundingBox3d()).toMojang();
            if (!localBounds.intersects(nativeBounds)) {
                continue;
            }

            localBounds = localBounds.intersect(nativeBounds);
            Vector3dc pivot = pose.rotationPoint();
            Vector3dc position = pose.position();
            regions.add(new LocalRegion(localBounds, pose.transformPosition(Vec3.ZERO), pose.transformNormal(X_AXIS), pose.transformNormal(Y_AXIS), pose.transformNormal(Z_AXIS), new Vec3(pivot.x(), pivot.y(), pivot.z()), new Vec3(position.x(), position.y(), position.z())));
        }
        return List.copyOf(regions);
    }

    @Override
    public Projection resolve(Level level, Position position) {
        boolean inSubLevel = SableCompanion.INSTANCE.getContaining(level, position) != null;
        Vec3 projectedPosition = SableCompanion.INSTANCE.projectOutOfSubLevel(level, position);
        return new Projection(projectedPosition, inSubLevel);
    }

    @Override
    public RayProjection projectRay(Level level, Position start, Position end) {
        SubLevelAccess subLevel = SableCompanion.INSTANCE.getContaining(level, start);
        if (subLevel == null) {
            return Service.super.projectRay(level, start, end);
        }

        Pose3dc pose = subLevel.logicalPose();
        Vec3 worldStart = pose.transformPosition(new Vec3(start.x(), start.y(), start.z()));
        Vec3 worldEnd = pose.transformPosition(new Vec3(end.x(), end.y(), end.z()));
        return new RayProjection(worldStart, worldEnd, true);
    }

    @Override
    public CoordinateTransform createLogicalTransform(Level level, Position anchor) {
        SubLevelAccess subLevel = SableCompanion.INSTANCE.getContaining(level, anchor);
        if (subLevel == null) {
            return Service.super.createLogicalTransform(level, anchor);
        }

        return createTransform(subLevel.logicalPose());
    }

    @Override
    public CoordinateTransform createRenderTransform(Level level, Position anchor, float partialTicks) {
        SubLevelAccess subLevel = SableCompanion.INSTANCE.getContaining(level, anchor);
        if (subLevel == null) {
            return Service.super.createRenderTransform(level, anchor, partialTicks);
        }

        Pose3dc pose = subLevel instanceof ClientSubLevelAccess clientSubLevel ? clientSubLevel.renderPose(partialTicks) : subLevel.logicalPose();
        return createTransform(pose);
    }

    @Override
    public BlockHitResult clipAtRenderPose(Level level, ClipContext context, float partialTicks) {
        return SableRenderRaycast.clip(level, context, partialTicks);
    }

    @Override
    public @Nullable RenderSource captureRenderSource(Level level, Position anchor, float partialTick) {
        SubLevelAccess subLevel = SableCompanion.INSTANCE.getContaining(level, anchor);
        if (subLevel == null) {
            return null;
        }

        Pose3dc pose = subLevel instanceof ClientSubLevelAccess client ? client.renderPose(partialTick) : subLevel.logicalPose();
        return new RenderSource(subLevel.getUniqueId(), captureRenderGeometry(subLevel, pose));
    }

    @Override
    public @Nullable AABB sourceBounds(Level level, Position anchor) {
        SubLevelAccess owner = SableCompanion.INSTANCE.getContaining(level, anchor);
        if (owner == null) {
            return null;
        }

        return SableRenderBounds.localBounds(owner);
    }

    @Override
    public @Nullable Map<UUID, RenderGeometry> captureRenderGeometry(Level level, List<AABB> queries, List<AABB> paths, float partialTick) {
        Map<UUID, RenderGeometry> states = new LinkedHashMap<>();
        for (Region region : SableRenderBounds.query(level, queries, partialTick)) {
            for (AABB path : paths) {
                if (!region.world().intersects(path)) {
                    continue;
                }

                if (states.size() >= MAX_RENDER_SUBLEVELS) {
                    return null;
                }

                SubLevelAccess owner = region.owner();
                states.put(owner.getUniqueId(), captureRenderGeometry(owner, region.pose()));
                break;
            }
        }
        return Map.copyOf(states);
    }

    @Override
    public boolean canSeeWorldSky(Level level, Position position) {
        Vec3 worldPosition = SableCompanion.INSTANCE.projectOutOfSubLevel(level, position);
        BlockPos skyCheckPos = BlockPos.containing(worldPosition);
        if (!level.isLoaded(skyCheckPos)) {
            return false;
        }

        int maxBuildHeight = level.getMaxBuildHeight();
        double worldHeight = maxBuildHeight - level.getMinBuildHeight();
        double searchTop = Math.max(maxBuildHeight + 1, worldPosition.y + worldHeight);
        BoundingBox3d skyColumn = new BoundingBox3d(worldPosition.x - SKY_COLUMN_EPSILON, worldPosition.y + SKY_RAY_EPSILON, worldPosition.z - SKY_COLUMN_EPSILON, worldPosition.x + SKY_COLUMN_EPSILON, searchTop, worldPosition.z + SKY_COLUMN_EPSILON);
        double skyTop = Math.max(worldPosition.y, level.getHeight(Types.WORLD_SURFACE, skyCheckPos.getX(), skyCheckPos.getZ()));
        for (SubLevelAccess subLevel : SableCompanion.INSTANCE.getAllIntersecting(level, skyColumn)) {
            skyTop = Math.max(skyTop, subLevel.boundingBox().maxY() + SKY_RAY_EPSILON);
        }
        if (skyTop <= worldPosition.y + SKY_RAY_EPSILON) {
            return true;
        }

        Vec3 traceStart = worldPosition.add(0, SKY_RAY_EPSILON, 0);
        Vec3 traceEnd = new Vec3(worldPosition.x, skyTop, worldPosition.z);
        BlockHitResult hitResult = level.clip(new ClipContext(traceStart, traceEnd, Block.COLLIDER, Fluid.NONE, SkyLightCollisionContext.INSTANCE));
        return hitResult.getType() == Type.MISS;
    }

    @Override
    public int countVisibleWorldSkySamples(Level level, List<Vec3> samples) {
        if (samples.isEmpty()) {
            return 0;
        }

        SableCompanion companion = SableCompanion.INSTANCE;
        SubLevelAccess source = companion.getContaining(level, samples.getFirst());
        List<Vec3> worldSamples = samples;
        if (source != null) {
            Pose3dc pose = source.logicalPose();
            worldSamples = new ArrayList<>(samples.size());
            for (Vec3 sample : samples) {
                worldSamples.add(pose.transformPosition(sample));
            }
        }
        BoundingBox3d bounds = createSkyColumn(level, worldSamples.getFirst());
        for (int index = 1; index < worldSamples.size(); index++) {
            bounds.expandTo(createSkyColumn(level, worldSamples.get(index)), bounds);
        }
        List<SubLevelAccess> allCandidates = new ArrayList<>();
        double columnTop = bounds.maxY();
        double rayTop = columnTop;
        for (SubLevelAccess candidate : companion.getAllIntersecting(level, bounds)) {
            allCandidates.add(candidate);
            rayTop = Math.max(rayTop, candidate.boundingBox().maxY() + SKY_RAY_EPSILON);
        }
        if (rayTop > columnTop) {
            BoundingBox3d extendedBounds = new BoundingBox3d(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), rayTop, bounds.maxZ());
            allCandidates.clear();
            for (SubLevelAccess candidate : companion.getAllIntersecting(level, extendedBounds)) {
                allCandidates.add(candidate);
            }
        }
        SableSkyLightQuery query = new SableSkyLightQuery(level, allCandidates, samples.size());
        int visibleSamples = 0;
        for (Vec3 sample : worldSamples) {
            if (!query.isVisible(level, sample, createSkyColumn(level, sample))) {
                continue;
            }

            visibleSamples++;
        }
        return visibleSamples;
    }

    @Override
    public boolean isRainingAtWorld(Level level, Position position) {
        Vec3 worldPosition = SableCompanion.INSTANCE.projectOutOfSubLevel(level, position);
        BlockPos rainCheckPos = BlockPos.containing(worldPosition).above();
        return level.isLoaded(rainCheckPos) && level.isRainingAt(rainCheckPos);
    }

    @Override
    public EntityArea createEntityArea(Level level, BlockPos origin, AABB localBounds) {
        SubLevelAccess subLevel = SableCompanion.INSTANCE.getContaining(level, origin);
        return new SableEntityArea(createOrientedBox(localBounds, subLevel));
    }

    private static final class SableEntityArea implements EntityArea {
        private final OrientedBox bounds;
        private final double[] rotation;
        private final double[] absoluteRotation;
        private final double[] translatedCenter;

        private SableEntityArea(OrientedBox bounds) {
            this.bounds = bounds;
            rotation = new double[9];
            absoluteRotation = new double[9];
            translatedCenter = new double[3];
        }

        private static boolean intersects(OrientedBox first, OrientedBox second, double[] rotation, double[] absoluteRotation, double[] translatedCenter) {
            for (int firstAxis = 0; firstAxis < 3; firstAxis++) {
                for (int secondAxis = 0; secondAxis < 3; secondAxis++) {
                    int rotationIndex = firstAxis * 3 + secondAxis;
                    double dot = first.axis(firstAxis).dot(second.axis(secondAxis));
                    rotation[rotationIndex] = dot;
                    absoluteRotation[rotationIndex] = Math.abs(dot) + SAT_EPSILON;
                }
            }

            Vec3 centerDelta = second.center.subtract(first.center);
            for (int firstAxis = 0; firstAxis < 3; firstAxis++) {
                translatedCenter[firstAxis] = centerDelta.dot(first.axis(firstAxis));
            }

            for (int firstAxis = 0; firstAxis < 3; firstAxis++) {
                double secondRadius = 0;
                for (int secondAxis = 0; secondAxis < 3; secondAxis++) {
                    secondRadius += second.extent(secondAxis) * absoluteRotation[firstAxis * 3 + secondAxis];
                }
                if (!(Math.abs(translatedCenter[firstAxis]) > first.extent(firstAxis) + secondRadius)) {
                    continue;
                }

                return false;
            }

            for (int secondAxis = 0; secondAxis < 3; secondAxis++) {
                double firstRadius = 0;
                double projectedCenter = 0;
                for (int firstAxis = 0; firstAxis < 3; firstAxis++) {
                    int rotationIndex = firstAxis * 3 + secondAxis;
                    firstRadius += first.extent(firstAxis) * absoluteRotation[rotationIndex];
                    projectedCenter += translatedCenter[firstAxis] * rotation[rotationIndex];
                }
                if (Math.abs(projectedCenter) <= firstRadius + second.extent(secondAxis)) {
                    continue;
                }

                return false;
            }

            for (int firstAxis = 0; firstAxis < 3; firstAxis++) {
                int firstNext = (firstAxis + 1) % 3;
                int firstLast = (firstAxis + 2) % 3;
                for (int secondAxis = 0; secondAxis < 3; secondAxis++) {
                    int secondNext = (secondAxis + 1) % 3;
                    int secondLast = (secondAxis + 2) % 3;
                    double firstRadius = first.extent(firstNext) * absoluteRotation[firstLast * 3 + secondAxis] + first.extent(firstLast) * absoluteRotation[firstNext * 3 + secondAxis];
                    double secondRadius = second.extent(secondNext) * absoluteRotation[firstAxis * 3 + secondLast] + second.extent(secondLast) * absoluteRotation[firstAxis * 3 + secondNext];
                    double projectedCenter = Math.abs(translatedCenter[firstLast] * rotation[firstNext * 3 + secondAxis] - translatedCenter[firstNext] * rotation[firstLast * 3 + secondAxis]);
                    if (projectedCenter <= firstRadius + secondRadius) {
                        continue;
                    }

                    return false;
                }
            }

            return true;
        }

        @Override
        public boolean intersects(Entity entity, AABB localBounds) {
            SubLevelAccess entitySubLevel = SableCompanion.INSTANCE.getContaining(entity);
            OrientedBox entityBounds = createOrientedBox(localBounds, entitySubLevel);
            return intersects(bounds, entityBounds, rotation, absoluteRotation, translatedCenter);
        }
    }

    private record OrientedBox(Vec3 center, Vec3 axisX, Vec3 axisY, Vec3 axisZ, double extentX, double extentY, double extentZ) {
        private Vec3 axis(int axisIndex) {
            return switch (axisIndex) {
                case 0 -> axisX;
                case 1 -> axisY;
                default -> axisZ;
            };
        }

        private double extent(int axisIndex) {
            return switch (axisIndex) {
                case 0 -> extentX;
                case 1 -> extentY;
                default -> extentZ;
            };
        }
    }
}
