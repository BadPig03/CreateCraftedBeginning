package net.ty.createcraftedbeginning.gas.flow;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.CoordinateTransform;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.EntityArea;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasIntakeFlow {
    private static final double REFERENCE_FLOW_GU_PER_TICK = 1000;
    private static final double MAX_ACCELERATION = 0.12;
    private static final double APPROACH_SPEED = 0.25;
    private static final double ARRIVAL_RESPONSE = 0.5;
    private static final double MOUTH_CLEARANCE = 0.03;
    private static final double RAY_EPSILON = 0.0001;

    private final GasFlowFrame frame;
    private final BlockPos sourcePos;
    private final Vec3 direction;
    private double amount;

    GasIntakeFlow(Level level, BlockPos pos, Direction direction) {
        frame = GasFlowFrame.capture(level, pos);
        sourcePos = pos.immutable();
        this.direction = Vec3.atLowerCornerOf(direction.getNormal());
    }

    void add(long amount) {
        this.amount += amount;
    }

    void collect(ServerLevel level, Map<Entity, GasFlowTarget> targets) {
        AABB localBounds = GasIntakeArea.bounds(frame.halfSize(), direction);
        Vec3 halfSize = new Vec3(localBounds.getXsize(), localBounds.getYsize(), localBounds.getZsize()).scale(0.5);
        Vec3 face = direction.multiply(frame.halfSize());
        EntityArea area = SubLevelBridge.createEntityArea(level, frame, localBounds);
        Vec3 axis = frame.transformNormal(direction);
        Vec3 mouth = frame.transformPosition(face);
        Vec3 rayStart = mouth.add(axis.scale(RAY_EPSILON));
        double strength = MAX_ACCELERATION * amount / (amount + REFERENCE_FLOW_GU_PER_TICK);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, frame.worldBounds(localBounds))) {
            if (!(entity instanceof ItemEntity) && !(entity instanceof LivingEntity) || !GasFlowTarget.canAffect(entity) || entity.isPassenger() || !area.intersects(entity)) {
                continue;
            }

            CoordinateTransform transform = SubLevelBridge.createEntityTransform(entity);
            AABB bounds = entity.getBoundingBox();
            Vec3 center = transform.transformPosition(bounds.getCenter());
            Vec3 point = GasFlowGeometry.closestPoint(entity, transform, mouth);
            if (center.subtract(mouth).dot(axis) < 0 || !GasFlowGeometry.canSee(level, entity, sourcePos, rayStart, point)) {
                continue;
            }

            Vec3 localPoint = frame.inverseTransformPosition(point);
            Vec3 crossPoint = localPoint.subtract(direction.scale(localPoint.dot(direction)));
            double axial = localPoint.subtract(face).dot(direction) / (2 * halfSize.dot(direction.multiply(direction)));
            double lateral = Math.max(Math.abs(crossPoint.x) / halfSize.x, Math.max(Math.abs(crossPoint.y) / halfSize.y, Math.abs(crossPoint.z) / halfSize.z));
            double attenuation = 1 - Mth.clamp(Math.max(axial, lateral), 0, 1);
            if (!(entity instanceof ItemEntity item)) {
                Vec3 offset = mouth.subtract(center);
                double distance = offset.length();
                if (distance <= RAY_EPSILON || attenuation <= 0) {
                    continue;
                }

                double multiplier = 1;
                if (entity instanceof Player && entity.isShiftKeyDown()) {
                    multiplier = 0.25;
                }

                Vec3 impulse = offset.scale(strength * attenuation * multiplier / distance);
                targets.computeIfAbsent(entity, ignored -> new GasFlowTarget()).add(impulse, true);
                continue;
            }

            double radius = Math.abs(transform.transformNormal(new Vec3(bounds.getXsize() * 0.5, 0, 0)).dot(axis)) + Math.abs(transform.transformNormal(new Vec3(0, bounds.getYsize() * 0.5, 0)).dot(axis)) + Math.abs(transform.transformNormal(new Vec3(0, 0, bounds.getZsize() * 0.5)).dot(axis));
            Vec3 offset = mouth.add(axis.scale(radius + MOUTH_CLEARANCE)).subtract(center);
            double distance = offset.length();
            Vec3 desired = offset.scale(ARRIVAL_RESPONSE);
            if (distance * ARRIVAL_RESPONSE > APPROACH_SPEED) {
                desired = offset.scale(APPROACH_SPEED / distance);
            }

            Vec3 movement = transform.transformNormal(item.getDeltaMovement());
            Vec3 gravity = transform.transformNormal(new Vec3(0, item.getGravity(), 0));
            Vec3 impulse = desired.add(gravity).subtract(movement);
            double limit = strength * attenuation;
            double length = impulse.length();
            if (length <= 0 || limit <= 0) {
                continue;
            }

            if (length > limit) {
                impulse = impulse.scale(limit / length);
            }

            targets.computeIfAbsent(item, ignored -> new GasFlowTarget()).add(impulse, true);
        }
    }
}
