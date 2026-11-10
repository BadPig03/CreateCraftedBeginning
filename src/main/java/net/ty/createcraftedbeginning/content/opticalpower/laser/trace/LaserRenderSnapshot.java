package net.ty.createcraftedbeginning.content.opticalpower.laser.trace;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation;
import net.ty.createcraftedbeginning.content.opticalpower.laser.BeamPropagation.Segment;
import net.ty.createcraftedbeginning.content.opticalpower.laser.PropagationBudget;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamFrame;
import net.ty.createcraftedbeginning.content.opticalpower.laser.beam.BeamState;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.CoordinateTransform;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record LaserRenderSnapshot(BeamPropagation propagation, Vec3 localOrigin, Vec3 worldOrigin, Vec3 xAxis, Vec3 yAxis, Vec3 zAxis) {
    public static LaserRenderSnapshot trace(Level level, BeamState localSource, Vec3 localOrigin, double range, float partialTick) {
        return trace(level, localSource, localOrigin, range, new LaserCollisionContext(partialTick));
    }

    static LaserRenderSnapshot trace(Level level, BeamState localSource, Vec3 localOrigin, double range, LaserCollisionContext collision) {
        float partialTick = collision.getPartialTick();
        CoordinateTransform transform = SubLevelBridge.createRenderTransform(level, localOrigin.add(0.5, 0.5, 0.5), partialTick);
        BeamFrame local = localSource.frame();
        BeamFrame world = new BeamFrame(transform.transformPosition(local.origin()), transform.transformNormal(local.direction()), transform.transformNormal(local.transverseAxis()));
        BeamState source = new BeamState(world, localSource.profile(), localSource.fluxLm(), localSource.footprint());
        BeamPropagation propagation = BeamOcclusionSolver.solve(level, source, PropagationBudget.standard(range), collision, true);
        return new LaserRenderSnapshot(propagation, localOrigin, transform.transformPosition(localOrigin), transform.transformNormal(new Vec3(1, 0, 0)), transform.transformNormal(new Vec3(0, 1, 0)), transform.transformNormal(new Vec3(0, 0, 1)));
    }

    public double distanceTo(Vec3 position) {
        double distanceSquared = Double.POSITIVE_INFINITY;
        for (Segment segment : propagation.segments()) {
            BeamFrame frame = segment.start().frame();
            Vec3 origin = frame.origin();
            Vec3 direction = frame.direction();
            double x = position.x - origin.x;
            double y = position.y - origin.y;
            double z = position.z - origin.z;
            double along = Math.clamp(x * direction.x + y * direction.y + z * direction.z, 0, segment.length());
            x -= direction.x * along;
            y -= direction.y * along;
            z -= direction.z * along;
            distanceSquared = Math.min(distanceSquared, x * x + y * y + z * z);
        }
        return Math.sqrt(distanceSquared);
    }

    public Vec3 toLocalPosition(Vec3 worldPosition) {
        return localOrigin.add(toLocalDirection(worldPosition.subtract(worldOrigin)));
    }

    public Vec3 toLocalDirection(Vec3 worldDirection) {
        return new Vec3(worldDirection.dot(xAxis) / xAxis.length(), worldDirection.dot(yAxis) / yAxis.length(), worldDirection.dot(zAxis) / zAxis.length());
    }

    public Vec3 toWorldPosition(Vec3 localPosition) {
        Vec3 local = localPosition.subtract(localOrigin);
        return worldOrigin.add(xAxis.scale(local.x / xAxis.length())).add(yAxis.scale(local.y / yAxis.length())).add(zAxis.scale(local.z / zAxis.length()));
    }
}
