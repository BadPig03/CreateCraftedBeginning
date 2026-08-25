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

    public static EntityArea createEntityArea(Level level, BlockPos origin, AABB localBounds) {
        return service.createEntityArea(level, origin, localBounds);
    }

    @FunctionalInterface
    public interface EntityArea {
        boolean intersects(Entity entity);
    }

    public interface Service {
        default Projection resolve(Level level, Position position) {
            return new Projection(new Vec3(position.x(), position.y(), position.z()), false);
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
}
