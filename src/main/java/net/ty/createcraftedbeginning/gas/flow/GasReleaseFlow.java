package net.ty.createcraftedbeginning.gas.flow;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseArea;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseContext;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.CoordinateTransform;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.EntityArea;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasReleaseFlow {
    private static final double LOG_2 = Math.log(2);
    private static final double PRESSURE_STEPS = 3;
    private static final double REFERENCE_FLOW_GU_PER_TICK = 1000;
    private static final double REFERENCE_BURST_GU = 2000;
    private static final double BASE_ACCELERATION = 0.12;
    private static final double BASE_IMPULSE = 0.8;
    private static final double RAY_EPSILON = 0.0001;

    private final GasFlowFrame frame;
    private final BlockPos sourcePos;
    private final Vec3 direction;
    private final boolean continuous;
    private final Map<GasReleaseArea, Emission> emissions = new LinkedHashMap<>();
    private double amount;
    private GasReleaseArea largestArea = new GasReleaseArea(1);

    GasReleaseFlow(GasReleaseContext context, boolean continuous) {
        frame = GasFlowFrame.capture(context.level(), context.sourcePos());
        sourcePos = context.sourcePos().immutable();
        direction = context.direction().map(value -> Vec3.atLowerCornerOf(value.getNormal())).orElse(Vec3.ZERO);
        this.continuous = continuous;
    }

    void add(GasReleaseContext context) {
        double released = context.gasAmount();
        double pressureDelta = context.pressureDeltaPa().orElse(0);
        double pressure = Mth.clamp(Math.log1p(pressureDelta / GasPressure.REFERENCE_PRESSURE_PA) / LOG_2 / PRESSURE_STEPS, 0, 1);
        amount += released;
        GasReleaseArea area = context.releaseArea();
        Emission emission = emissions.computeIfAbsent(area, ignored -> new Emission());
        emission.amount += released;
        emission.weightedPressure += released * pressure;
        if (area.sideLength() <= largestArea.sideLength()) {
            return;
        }

        largestArea = area;
    }

    void collect(ServerLevel level, double multiplier, Map<Entity, GasFlowTarget> targets) {
        boolean directional = direction.lengthSqr() > 0;
        Vec3 halfSize = frame.halfSize();
        Vec3 face = direction.multiply(halfSize);
        AABB localBounds = largestArea.bounds(Vec3.ZERO, halfSize, direction);
        AABB worldBounds = frame.worldBounds(localBounds);
        Vec3 origin = frame.center();
        Vec3 worldDirection = frame.transformNormal(direction);
        Vec3 mouth = frame.transformPosition(face);
        double referenceAmount = continuous ? REFERENCE_FLOW_GU_PER_TICK : REFERENCE_BURST_GU;
        double base = continuous ? BASE_ACCELERATION : BASE_IMPULSE;
        List<Contribution> contributions = new ArrayList<>(emissions.size());
        for (Entry<GasReleaseArea, Emission> entry : emissions.entrySet()) {
            GasReleaseArea area = entry.getKey();
            Emission emission = entry.getValue();
            double pressure = emission.weightedPressure / emission.amount;
            double strength = base * emission.amount / (amount + referenceAmount) * (0.5 + 0.5 * pressure) * multiplier;
            EntityArea targetsArea = SubLevelBridge.createEntityArea(level, frame, area.bounds(Vec3.ZERO, halfSize, direction));
            contributions.add(new Contribution(targetsArea, halfSize.scale(area.sideLength()), strength));
        }
        Set<Entity> processed = new HashSet<>();
        for (Entity candidate : level.getEntitiesOfClass(Entity.class, worldBounds)) {
            Entity entity = candidate.getRootVehicle();
            if (!processed.add(entity) || !GasFlowTarget.canAffect(entity)) {
                continue;
            }

            CoordinateTransform transform = SubLevelBridge.createEntityTransform(entity);
            Vec3 point = GasFlowGeometry.closestPoint(entity, transform, directional ? mouth : origin);
            Vec3 offset = point.subtract(origin);
            Vec3 pushDirection = worldDirection;
            Vec3 rayStart = mouth.add(worldDirection.scale(RAY_EPSILON));
            if (!directional) {
                double distance = offset.length();
                if (distance <= RAY_EPSILON) {
                    continue;
                }

                pushDirection = offset.scale(1 / distance);
                Vec3 localOffset = frame.inverseTransformNormal(offset);
                double exitScale = Math.max(Math.abs(localOffset.x) / halfSize.x, Math.max(Math.abs(localOffset.y) / halfSize.y, Math.abs(localOffset.z) / halfSize.z));
                rayStart = origin.add(offset.scale(1 / Math.max(1, exitScale))).add(pushDirection.scale(RAY_EPSILON));
            }

            if (!GasFlowGeometry.canSee(level, entity, sourcePos, rayStart, point)) {
                continue;
            }

            double entityMultiplier = 1;
            if (entity instanceof ItemEntity || entity instanceof ExperienceOrb) {
                entityMultiplier = 0.5;
            }

            if (entity instanceof Player && entity.isShiftKeyDown()) {
                entityMultiplier *= 0.25;
            }

            Vec3 localPoint = frame.inverseTransformPosition(point);
            Vec3 crossPoint = localPoint.subtract(direction.scale(localPoint.dot(direction)));
            double strength = 0;
            for (Contribution contribution : contributions) {
                if (!contribution.area().intersects(entity)) {
                    continue;
                }

                Vec3 extent = contribution.halfSize();
                double distance = Math.max(Math.abs(localPoint.x) / extent.x, Math.max(Math.abs(localPoint.y) / extent.y, Math.abs(localPoint.z) / extent.z));
                if (directional) {
                    double axial = localPoint.subtract(face).dot(direction) / (2 * extent.dot(direction.multiply(direction)));
                    double lateral = Math.max(Math.abs(crossPoint.x) / extent.x, Math.max(Math.abs(crossPoint.y) / extent.y, Math.abs(crossPoint.z) / extent.z));
                    distance = Math.max(axial, lateral);
                }

                strength += contribution.strength() * (1 - Mth.clamp(distance, 0, 1));
            }
            if (strength <= 0) {
                continue;
            }

            targets.computeIfAbsent(entity, ignored -> new GasFlowTarget()).add(pushDirection.scale(strength * entityMultiplier), continuous);
        }
    }

    private record Contribution(EntityArea area, Vec3 halfSize, double strength) {}

    private static final class Emission {
        private double amount;
        private double weightedPressure;
    }
}
