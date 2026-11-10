package net.ty.createcraftedbeginning.platform;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class SubLevelBridge {
    private static final Service VANILLA = new Service() {
        @Override
        public int countVisibleWorldSkySamples(Level level, List<Vec3> samples) {
            SkyLightCollisionContext skyLight = SkyLightCollisionContext.createQuery(level, samples.size());
            int visibleSamples = 0;
            for (Vec3 sample : samples) {
                if (!canSeeSky(level, sample, skyLight)) {
                    continue;
                }

                visibleSamples++;
            }
            return visibleSamples;
        }
    };
    private static volatile Service service = VANILLA;

    private SubLevelBridge() {
    }

    public static void install(Service service) {
        SubLevelBridge.service = service;
    }

    public static @Nullable UUID subLevelId(Level level, BlockPos pos) {
        return service.subLevelId(level, pos);
    }

    public static Projection resolve(Level level, Position position) {
        return service.resolve(level, position);
    }

    public static Projection resolve(Level level, BlockPos blockPos) {
        return resolve(level, Vec3.atCenterOf(blockPos));
    }

    public static RayProjection projectRay(Level level, Position start, Position end) {
        return service.projectRay(level, start, end);
    }

    public static CoordinateTransform createLogicalTransform(Level level, Position anchor) {
        return service.createLogicalTransform(level, anchor);
    }

    public static CoordinateTransform createRenderTransform(Level level, Position anchor, float partialTicks) {
        return service.createRenderTransform(level, anchor, partialTicks);
    }

    public static @Nullable RenderSource captureRenderSource(Level level, Position anchor, float partialTick) {
        return service.captureRenderSource(level, anchor, partialTick);
    }

    public static List<ServerPlayer> trackingPlayers(ServerLevel level, BlockPos pos) {
        return service.trackingPlayers(level, pos);
    }

    public static BlockHitResult clipAtRenderPose(Level level, ClipContext context, float partialTicks) {
        return service.clipAtRenderPose(level, context, partialTicks);
    }

    public static @Nullable Map<UUID, RenderGeometry> captureRenderGeometry(Level level, List<AABB> queries, List<AABB> paths, float partialTick) {
        return service.captureRenderGeometry(level, queries, paths, partialTick);
    }

    public static boolean canSeeWorldSky(Level level, Position position) {
        return service.canSeeWorldSky(level, position);
    }

    public static int countVisibleWorldSkySamples(Level level, List<Vec3> samples) {
        return service.countVisibleWorldSkySamples(level, samples);
    }

    public static boolean isRainingAtWorld(Level level, Position position) {
        return service.isRainingAtWorld(level, position);
    }

    public static EntityArea createEntityArea(Level level, BlockPos origin, AABB localBounds) {
        return service.createEntityArea(level, origin, localBounds);
    }

    public static <T> @Nullable T findAt(Level level, BlockPos origin, BlockPos sample, Function<BlockPos, @Nullable T> query) {
        return service.findAt(level, origin, sample, pos -> {
            if (!level.isLoaded(pos)) {
                return null;
            }

            return query.apply(pos);
        });
    }

    public static boolean testLocalFrames(Level level, Vec3 position, Vec3 direction, double radius, BiPredicate<Vec3, Vec3> query) {
        return service.testLocalFrames(level, position, direction, radius, query);
    }

    public static boolean testLocalBounds(Level level, AABB bounds, Predicate<AABB> query) {
        return service.testLocalBounds(level, bounds, query);
    }

    public static @Nullable AABB sourceBounds(Level level, Position anchor) {
        return service.sourceBounds(level, anchor);
    }

    public static List<LocalRegion> localRegions(Level level, AABB bounds) {
        return service.localRegions(level, bounds);
    }

    public static List<LocalRegion> localRegions(Level level, AABB bounds, float partialTick, boolean rendering) {
        return service.localRegions(level, bounds, partialTick, rendering);
    }

    private static boolean canSeeSky(Level level, Position position, SkyLightCollisionContext skyLight) {
        Vec3 start = new Vec3(position.x(), position.y(), position.z());
        BlockPos samplePos = BlockPos.containing(start);
        if (!level.isLoaded(samplePos)) {
            return false;
        }

        double top = level.getHeight(Types.WORLD_SURFACE, samplePos.getX(), samplePos.getZ());
        if (top <= start.y) {
            return true;
        }

        Vec3 end = new Vec3(start.x, top, start.z);
        return skyLight.clip(level, new ClipContext(start, end, Block.COLLIDER, Fluid.NONE, skyLight)).getType() == Type.MISS;
    }

    @FunctionalInterface
    public interface EntityArea {
        boolean intersects(Entity entity, AABB entityBounds);

        default boolean intersects(Entity entity) {
            return intersects(entity, entity.getBoundingBox());
        }
    }

    @FunctionalInterface
    public interface CoordinateTransform {
        Vec3 transformPosition(Position position);

        default Vec3 transformNormal(Position normal) {
            return new Vec3(normal.x(), normal.y(), normal.z());
        }

        default boolean inSubLevel() {
            return false;
        }
    }

    public record RenderGeometry(AABB bounds, Vec3 position, Vec3 pivot, Vec3 scale, double rotationX, double rotationY, double rotationZ, double rotationW) {}

    public record RenderSource(UUID id, RenderGeometry geometry) {}

    public interface Service {
        default @Nullable UUID subLevelId(Level level, BlockPos pos) {
            return null;
        }

        default @Nullable AABB sourceBounds(Level level, Position anchor) {
            return null;
        }

        default List<ServerPlayer> trackingPlayers(ServerLevel level, BlockPos pos) {
            return level.getChunkSource().chunkMap.getPlayers(new ChunkPos(pos), false);
        }

        default @Nullable RenderSource captureRenderSource(Level level, Position anchor, float partialTick) {
            return null;
        }

        default @Nullable Map<UUID, RenderGeometry> captureRenderGeometry(Level level, List<AABB> queries, List<AABB> paths, float partialTick) {
            return Map.of();
        }

        default List<LocalRegion> localRegions(Level level, AABB bounds) {
            return List.of(new LocalRegion(bounds, Vec3.ZERO, new Vec3(1, 0, 0), new Vec3(0, 1, 0), new Vec3(0, 0, 1)));
        }

        default List<LocalRegion> localRegions(Level level, AABB bounds, float partialTick, boolean rendering) {
            return localRegions(level, bounds);
        }

        default BlockHitResult clipAtRenderPose(Level level, ClipContext context, float partialTicks) {
            return level.clip(context);
        }

        default boolean testLocalBounds(Level level, AABB bounds, Predicate<AABB> query) {
            return query.test(bounds);
        }

        default boolean testLocalFrames(Level level, Vec3 position, Vec3 direction, double radius, BiPredicate<Vec3, Vec3> query) {
            return query.test(position, direction);
        }

        default <T> @Nullable T findAt(Level level, BlockPos origin, BlockPos sample, Function<BlockPos, @Nullable T> query) {
            return query.apply(sample);
        }

        default Projection resolve(Level level, Position position) {
            return new Projection(new Vec3(position.x(), position.y(), position.z()), false);
        }

        default RayProjection projectRay(Level level, Position start, Position end) {
            return new RayProjection(new Vec3(start.x(), start.y(), start.z()), new Vec3(end.x(), end.y(), end.z()), false);
        }

        default CoordinateTransform createLogicalTransform(Level level, Position anchor) {
            return position -> new Vec3(position.x(), position.y(), position.z());
        }

        default CoordinateTransform createRenderTransform(Level level, Position anchor, float partialTicks) {
            return position -> new Vec3(position.x(), position.y(), position.z());
        }

        default boolean canSeeWorldSky(Level level, Position position) {
            return canSeeSky(level, position, SkyLightCollisionContext.INSTANCE);
        }

        default int countVisibleWorldSkySamples(Level level, List<Vec3> samples) {
            int visibleSamples = 0;
            for (Vec3 sample : samples) {
                if (!canSeeWorldSky(level, sample)) {
                    continue;
                }

                visibleSamples++;
            }
            return visibleSamples;
        }

        default boolean isRainingAtWorld(Level level, Position position) {
            return level.isRainingAt(BlockPos.containing(position).above());
        }

        default EntityArea createEntityArea(Level level, BlockPos origin, AABB localBounds) {
            return (entity, entityBounds) -> localBounds.intersects(entityBounds);
        }
    }

    public record Projection(Vec3 worldPosition, boolean inSubLevel) {
        public BlockPos blockPos() {
            return BlockPos.containing(worldPosition);
        }
    }

    public record RayProjection(Vec3 worldStart, Vec3 worldEnd, boolean inSubLevel) {}

    public record LocalRegion(AABB bounds, Vec3 origin, Vec3 axisX, Vec3 axisY, Vec3 axisZ, Vec3 pivot, Vec3 worldPivot) {
        public LocalRegion(AABB bounds, Vec3 origin, Vec3 axisX, Vec3 axisY, Vec3 axisZ) {
            this(bounds, origin, axisX, axisY, axisZ, Vec3.ZERO, origin);
        }
    }
}
