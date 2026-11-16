package net.ty.createcraftedbeginning.gas.flow;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.CoordinateTransform;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
record GasFlowFrame(Vec3 center, Vec3 axisX, Vec3 axisY, Vec3 axisZ, Vec3 halfSize) implements CoordinateTransform {
    static GasFlowFrame capture(Level level, BlockPos pos) {
        Vec3 localCenter = pos.getCenter();
        CoordinateTransform transform = SubLevelBridge.createLogicalTransform(level, localCenter);
        Vec3 x = transform.transformNormal(new Vec3(1, 0, 0));
        Vec3 y = transform.transformNormal(new Vec3(0, 1, 0));
        Vec3 z = transform.transformNormal(new Vec3(0, 0, 1));
        return new GasFlowFrame(transform.transformPosition(localCenter), x.normalize(), y.normalize(), z.normalize(), new Vec3(x.length() * 0.5, y.length() * 0.5, z.length() * 0.5));
    }

    @Override
    public Vec3 transformPosition(Position position) {
        return center.add(transformNormal(position));
    }

    @Override
    public Vec3 inverseTransformPosition(Position position) {
        return inverseTransformNormal(new Vec3(position.x(), position.y(), position.z()).subtract(center));
    }

    @Override
    public Vec3 transformNormal(Position normal) {
        return axisX.scale(normal.x()).add(axisY.scale(normal.y())).add(axisZ.scale(normal.z()));
    }

    @Override
    public Vec3 inverseTransformNormal(Position normal) {
        Vec3 vector = new Vec3(normal.x(), normal.y(), normal.z());
        return new Vec3(vector.dot(axisX), vector.dot(axisY), vector.dot(axisZ));
    }

    AABB worldBounds(AABB bounds) {
        Vec3 middle = transformPosition(bounds.getCenter());
        double x = bounds.getXsize() * 0.5;
        double y = bounds.getYsize() * 0.5;
        double z = bounds.getZsize() * 0.5;
        return new AABB(middle, middle).inflate(Math.abs(axisX.x) * x + Math.abs(axisY.x) * y + Math.abs(axisZ.x) * z, Math.abs(axisX.y) * x + Math.abs(axisY.y) * y + Math.abs(axisZ.y) * z, Math.abs(axisX.z) * x + Math.abs(axisY.z) * y + Math.abs(axisZ.z) * z);
    }
}
