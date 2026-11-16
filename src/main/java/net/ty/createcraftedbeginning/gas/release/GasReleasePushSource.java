package net.ty.createcraftedbeginning.gas.release;

import com.simibubi.create.content.logistics.box.PackageEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseContext;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.CoordinateTransform;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.EntityArea;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasReleasePushSource {
    private static final double LOG_2 = Math.log(2);
    private static final double PRESSURE_STEPS = 3;
    private static final double REFERENCE_FLOW_GU_PER_TICK = 1000;
    private static final double REFERENCE_BURST_GU = 2000;
    private static final double BASE_ACCELERATION = 0.12;
    private static final double BASE_IMPULSE = 0.8;
    private static final double RAY_EPSILON = 0.0001;

    private final GasReleasePushFrame frame;
    private final Vec3 direction;
    private final boolean continuous;
    private double amount;
    private double weightedPressure;

    GasReleasePushSource(GasReleaseContext context, boolean continuous) {
        frame = GasReleasePushFrame.capture(context);
        direction = context.direction().map(value -> Vec3.atLowerCornerOf(value.getNormal())).orElse(Vec3.ZERO);
        this.continuous = continuous;
    }

    static boolean canPush(Entity entity) {
        if (!entity.isAlive() || entity.isSpectator() || entity instanceof ArmorStand) {
            return false;
        }

        if (entity instanceof Player player && player.isCreative() && player.getAbilities().flying) {
            return false;
        }

        return entity instanceof LivingEntity || entity instanceof ItemEntity || entity instanceof ExperienceOrb || entity instanceof PackageEntity || entity instanceof Boat || entity instanceof AbstractMinecart;
    }

    private static Vec3 closestPoint(Entity entity, CoordinateTransform transform, Vec3 worldPoint) {
        AABB bounds = entity.getBoundingBox();
        Vec3 local = transform.inverseTransformPosition(worldPoint);
        return transform.transformPosition(new Vec3(Mth.clamp(local.x, bounds.minX + RAY_EPSILON, bounds.maxX - RAY_EPSILON), Mth.clamp(local.y, bounds.minY + RAY_EPSILON, bounds.maxY - RAY_EPSILON), Mth.clamp(local.z, bounds.minZ + RAY_EPSILON, bounds.maxZ - RAY_EPSILON)));
    }

    private static boolean canSee(ServerLevel level, Entity entity, Vec3 start, Vec3 end) {
        int steps = Math.max(1, Mth.ceil(start.distanceTo(end) * 2));
        for (int step = 0; step <= steps; step++) {
            if (level.isLoaded(BlockPos.containing(start.lerp(end, (double) step / steps)))) {
                continue;
            }

            return false;
        }

        return level.clip(new ClipContext(start, end, Block.COLLIDER, Fluid.NONE, entity)).getType() == Type.MISS;
    }

    void add(GasReleaseContext context) {
        double released = context.gasAmount();
        double pressureDelta = context.pressureDeltaPa().orElse(0);
        double pressure = Mth.clamp(Math.log1p(pressureDelta / GasPressure.REFERENCE_PRESSURE_PA) / LOG_2 / PRESSURE_STEPS, 0, 1);
        amount += released;
        weightedPressure += released * pressure;
    }

    void collect(ServerLevel level, double multiplier, Map<Entity, GasReleasePushTarget> targets) {
        double pressure = weightedPressure / amount;
        boolean directional = direction.lengthSqr() > 0;
        double range = directional ? 2 + 2 * pressure : 1.5 + 1.5 * pressure;
        Vec3 halfSize = frame.halfSize();
        Vec3 face = direction.multiply(halfSize);
        AABB localBounds = new AABB(-range, -range, -range, range, range, range);
        if (directional) {
            localBounds = new AABB(face, face.add(direction.scale(range))).inflate(0.5 * (1 - Math.abs(direction.x)), 0.5 * (1 - Math.abs(direction.y)), 0.5 * (1 - Math.abs(direction.z)));
        }

        EntityArea area = SubLevelBridge.createEntityArea(level, frame, localBounds);
        AABB worldBounds = frame.worldBounds(localBounds);
        Vec3 origin = frame.center();
        Vec3 worldDirection = frame.transformNormal(direction);
        Vec3 mouth = frame.transformPosition(face);
        double referenceAmount = continuous ? REFERENCE_FLOW_GU_PER_TICK : REFERENCE_BURST_GU;
        double base = continuous ? BASE_ACCELERATION : BASE_IMPULSE;
        double strength = base * amount / (amount + referenceAmount) * (0.5 + 0.5 * pressure) * multiplier;
        Set<Entity> processed = new HashSet<>();
        for (Entity candidate : level.getEntitiesOfClass(Entity.class, worldBounds)) {
            Entity entity = candidate.getRootVehicle();
            if (!processed.add(entity) || !canPush(entity) || !area.intersects(entity)) {
                continue;
            }

            CoordinateTransform transform = SubLevelBridge.createEntityTransform(entity);
            Vec3 point = closestPoint(entity, transform, directional ? mouth : origin);
            Vec3 offset = point.subtract(origin);
            Vec3 pushDirection = worldDirection;
            Vec3 rayStart = mouth.add(worldDirection.scale(RAY_EPSILON));
            double distance = point.subtract(mouth).dot(worldDirection);
            if (!directional) {
                distance = offset.length();
                if (distance <= RAY_EPSILON || distance >= range) {
                    continue;
                }

                pushDirection = offset.scale(1 / distance);
                Vec3 localOffset = frame.inverseTransformNormal(offset);
                double exitScale = Math.max(Math.abs(localOffset.x) / halfSize.x, Math.max(Math.abs(localOffset.y) / halfSize.y, Math.abs(localOffset.z) / halfSize.z));
                rayStart = origin.add(offset.scale(1 / Math.max(1, exitScale))).add(pushDirection.scale(RAY_EPSILON));
            }

            double attenuation = 1 - Mth.clamp(distance / range, 0, 1);
            if (attenuation <= 0 || !canSee(level, entity, rayStart, point)) {
                continue;
            }

            double entityMultiplier = 1;
            if (entity instanceof ItemEntity || entity instanceof ExperienceOrb) {
                entityMultiplier = 0.5;
            }

            if (entity instanceof Player && entity.isShiftKeyDown()) {
                entityMultiplier *= 0.25;
            }

            Vec3 impulse = pushDirection.scale(strength * attenuation * entityMultiplier);
            targets.computeIfAbsent(entity, ignored -> new GasReleasePushTarget()).add(impulse, continuous);
        }
    }
}
