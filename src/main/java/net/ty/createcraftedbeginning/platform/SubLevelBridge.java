package net.ty.createcraftedbeginning.platform;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class SubLevelBridge {
    private static final Service VANILLA = new Service() {};
    private static volatile Service service = VANILLA;

    private SubLevelBridge() {
    }

    public static void install(Service service) {
        SubLevelBridge.service = service;
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

    public static CoordinateTransform createRenderTransform(Level level, Position anchor, float partialTicks) {
        return service.createRenderTransform(level, anchor, partialTicks);
    }

    public static boolean canSeeWorldSky(Level level, Position position) {
        return service.canSeeWorldSky(level, position);
    }

    public static boolean isRainingAtWorld(Level level, Position position) {
        return service.isRainingAtWorld(level, position);
    }

    public static EntityArea createEntityArea(Level level, BlockPos origin, AABB localBounds) {
        return service.createEntityArea(level, origin, localBounds);
    }

    @FunctionalInterface
    public interface EntityArea {
        boolean intersects(Entity entity);
    }

    public interface CoordinateTransform {
        Vec3 transformPosition(Position position);

        default Vec3 transformNormal(Position normal) {
            return new Vec3(normal.x(), normal.y(), normal.z());
        }

        default boolean inSubLevel() {
            return false;
        }
    }

    public interface Service {
        default Projection resolve(Level level, Position position) {
            return new Projection(new Vec3(position.x(), position.y(), position.z()), false);
        }

        default RayProjection projectRay(Level level, Position start, Position end) {
            return new RayProjection(new Vec3(start.x(), start.y(), start.z()), new Vec3(end.x(), end.y(), end.z()), false);
        }

        default CoordinateTransform createRenderTransform(Level level, Position anchor, float partialTicks) {
            return position -> new Vec3(position.x(), position.y(), position.z());
        }

        default boolean canSeeWorldSky(Level level, Position position) {
            return level.canSeeSky(BlockPos.containing(position).above());
        }

        default boolean isRainingAtWorld(Level level, Position position) {
            return level.isRainingAt(BlockPos.containing(position).above());
        }

        default EntityArea createEntityArea(Level level, BlockPos origin, AABB localBounds) {
            return entity -> localBounds.intersects(entity.getBoundingBox());
        }
    }

    public record Projection(Vec3 worldPosition, boolean inSubLevel) {
        public BlockPos blockPos() {
            return BlockPos.containing(worldPosition);
        }
    }

    public record RayProjection(Vec3 worldStart, Vec3 worldEnd, boolean inSubLevel) {}
}
