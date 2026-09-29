package net.ty.createcraftedbeginning.content.airtights.balloon;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record BalloonWorldPhysics(long ambientPressurePa, long localStandardAmount, double volumeRatio, double effectiveVolumeRatio, float linearScale, double gasLiftFactor, boolean vacuum) {
    public static final double MAX_EFFECTIVE_VOLUME_RATIO = 2.5;
    private static final double MIN_VISUAL_VOLUME_RATIO = 0.125;
    private static final double BASE_WATER_LIFT = 0.003;
    private static final double GAS_WATER_LIFT_SCALE = 0.007;
    private static final double BASE_AIR_LIFT = 0.085;
    private static final double GAS_AIR_LIFT_SCALE = 0.015;
    private static final double BASE_WATER_RISE_SPEED_LIMIT = 0.12;
    private static final double GAS_WATER_RISE_SPEED_LIMIT_SCALE = 0.04;
    private static final double BASE_AIR_RISE_SPEED_LIMIT = 0.16;
    private static final double GAS_AIR_RISE_SPEED_LIMIT_SCALE = 0.04;

    public static BalloonWorldPhysics of(ItemStack balloon, Level level, BlockPos pos) {
        GasStack gas = BalloonItem.getGas(balloon);
        return of(gas.getAmount(), BalloonPressureSemantics.ambientPressurePa(level, pos));
    }

    public static BalloonWorldPhysics of(long gasAmount, long ambientPressurePa) {
        long amount = Math.max(0, gasAmount);
        long pressure = Math.max(GasPressure.VACUUM_PA, ambientPressurePa);
        long localStandardAmount = BalloonPackingLimits.getLocalPackingLimit(pressure);
        boolean vacuum = pressure == GasPressure.VACUUM_PA;
        double volumeRatio;
        if (amount <= 0) {
            volumeRatio = 0;
        }
        else if (localStandardAmount <= 0) {
            volumeRatio = Double.POSITIVE_INFINITY;
        }
        else {
            volumeRatio = (double) amount / localStandardAmount;
        }

        double effectiveVolumeRatio = Double.isFinite(volumeRatio) ? Math.min(volumeRatio, MAX_EFFECTIVE_VOLUME_RATIO) : MAX_EFFECTIVE_VOLUME_RATIO;
        effectiveVolumeRatio = Math.max(0, effectiveVolumeRatio);
        float linearScale = (float) Math.cbrt(Math.max(effectiveVolumeRatio, MIN_VISUAL_VOLUME_RATIO));
        double gasLiftFactor = Math.sqrt(effectiveVolumeRatio);
        return new BalloonWorldPhysics(pressure, localStandardAmount, volumeRatio, effectiveVolumeRatio, linearScale, gasLiftFactor, vacuum);
    }

    public double buoyancyAcceleration(boolean submergedInWater) {
        if (submergedInWater) {
            return BASE_WATER_LIFT + GAS_WATER_LIFT_SCALE * gasLiftFactor;
        }

        if (effectiveVolumeRatio <= 0) {
            return 0;
        }

        return BASE_AIR_LIFT + GAS_AIR_LIFT_SCALE * gasLiftFactor;
    }

    public double buoyancyRiseSpeedLimit(boolean submergedInWater) {
        if (submergedInWater) {
            return BASE_WATER_RISE_SPEED_LIMIT + GAS_WATER_RISE_SPEED_LIMIT_SCALE * gasLiftFactor;
        }

        if (effectiveVolumeRatio <= 0) {
            return 0;
        }

        return BASE_AIR_RISE_SPEED_LIMIT + GAS_AIR_RISE_SPEED_LIMIT_SCALE * gasLiftFactor;
    }

    public double nextBuoyantVerticalSpeed(double currentVerticalSpeed, boolean submergedInWater) {
        double acceleration = buoyancyAcceleration(submergedInWater);
        if (acceleration <= 0) {
            return currentVerticalSpeed;
        }

        double speedLimit = buoyancyRiseSpeedLimit(submergedInWater);
        if (currentVerticalSpeed >= speedLimit) {
            return currentVerticalSpeed;
        }

        return Math.min(speedLimit, currentVerticalSpeed + acceleration);
    }
}
