package net.ty.createcraftedbeginning.api.gasreleasehandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;
import java.util.OptionalLong;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record GasReleaseArea(double sideLength) {
    public GasReleaseArea {
        if (!Double.isFinite(sideLength) || sideLength < 1 || sideLength > 5) {
            throw new IllegalArgumentException("Gas release side length must be between 1 and 5 blocks; got " + sideLength + '.');
        }
    }

    public static GasReleaseArea fromPressure(OptionalLong sourcePressurePa) {
        double pressure = (double) GasPressureLimits.clampToHardLimit(sourcePressurePa.orElse(GasPressure.REFERENCE_PRESSURE_PA)) / GasPressure.REFERENCE_PRESSURE_PA;
        if (pressure <= 1) {
            return new GasReleaseArea(1 + 2 * pressure);
        }

        return new GasReleaseArea(3 + 2 * (pressure - 1) / (GasPressureLimits.HARD_PRESSURE_ATM - 1));
    }

    public AABB bounds(BlockPos sourcePos, Optional<Direction> direction) {
        Vec3 axis = direction.map(value -> Vec3.atLowerCornerOf(value.getNormal())).orElse(Vec3.ZERO);
        return bounds(sourcePos.getCenter(), new Vec3(0.5, 0.5, 0.5), axis);
    }

    public AABB bounds(Vec3 sourceCenter, Vec3 sourceHalfSize, Vec3 direction) {
        Vec3 center = sourceCenter.add(direction.multiply(sourceHalfSize).scale(1 + sideLength));
        Vec3 halfSize = sourceHalfSize.scale(sideLength);
        return new AABB(center.subtract(halfSize), center.add(halfSize));
    }
}
