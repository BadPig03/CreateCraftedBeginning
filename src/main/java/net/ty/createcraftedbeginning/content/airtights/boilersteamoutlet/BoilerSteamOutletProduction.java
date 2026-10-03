package net.ty.createcraftedbeginning.content.airtights.boilersteamoutlet;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.canister.GasConsumptionMath;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BoilerSteamOutletProduction {
    private static final int OUTPUT_BUFFER_TICKS = 5;

    private static final String COMPOUND_KEY_PRODUCTION_RATE = "ProductionRate";
    private static final String COMPOUND_KEY_PRODUCTION_REMAINDER = "ProductionRemainder";

    private final BoilerSteamOutletBlockEntity outlet;

    private double productionRemainder;
    private double currentProductionRate;
    private long accountingTick = Long.MIN_VALUE;

    BoilerSteamOutletProduction(BoilerSteamOutletBlockEntity outlet) {
        this.outlet = outlet;
    }

    static long getOutputVolume() {
        double fullLoadProductionRate = getFullLoadProductionRate();
        if (!GasConsumptionMath.isFinite(fullLoadProductionRate) || fullLoadProductionRate <= 0) {
            return 0;
        }

        if (fullLoadProductionRate >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }

        return Math.max(1, (long) Math.ceil(fullLoadProductionRate));
    }

    static long getMaximumOutputPressurePa() {
        return GasPressure.pascals(OUTPUT_BUFFER_TICKS);
    }

    static long getMaximumOutputCapacity() {
        long outputVolume = getOutputVolume();
        if (outputVolume <= 0) {
            return 0;
        }

        return GasPressure.amount(outputVolume, getMaximumOutputPressurePa());
    }

    static double getFullLoadProductionRate() {
        double stressPerGasUnit = BoilerSteamOutletIntegration.getNormalPressureSteamStressCapacityPerGasUnit();
        if (!GasConsumptionMath.isFinite(stressPerGasUnit) || stressPerGasUnit <= 0) {
            return 0;
        }

        double fullLoadStressCapacity = BoilerSteamOutletIntegration.getSteamEngineFullLoadStressCapacity();
        if (!GasConsumptionMath.isFinite(fullLoadStressCapacity) || fullLoadStressCapacity <= 0) {
            return 0;
        }

        return fullLoadStressCapacity / stressPerGasUnit;
    }

    long getMaximumOutputAmount() {
        return getMaximumOutputCapacity();
    }

    boolean ensureCurrentTick() {
        Level level = outlet.getLevel();
        if (level == null || level.isClientSide) {
            return false;
        }

        long gameTime = level.getGameTime();
        if (accountingTick == gameTime) {
            return false;
        }

        accountingTick = gameTime;
        if (!BoilerSteamOutletBlock.isActive(outlet.getBlockState())) {
            boolean productionRateChanged = currentProductionRate != 0;
            currentProductionRate = 0;
            outlet.clearBufferedSteam();
            return productionRateChanged;
        }

        long maximumOutputAmount = getMaximumOutputAmount();
        double maximumProductionRate = getMaximumProductionRate();
        double pressureLimitedProductionRate = Math.min(maximumProductionRate, maximumOutputAmount);
        boolean productionRateChanged = currentProductionRate != pressureLimitedProductionRate;
        currentProductionRate = pressureLimitedProductionRate;
        if (!GasConsumptionMath.isFinite(pressureLimitedProductionRate) || pressureLimitedProductionRate <= 0) {
            return productionRateChanged;
        }

        double accumulatedProduction = productionRemainder + pressureLimitedProductionRate;
        long steamProducedThisTick = accumulatedProduction >= Long.MAX_VALUE ? Long.MAX_VALUE : Mth.lfloor(accumulatedProduction);
        productionRemainder = accumulatedProduction >= Long.MAX_VALUE ? 0 : accumulatedProduction - steamProducedThisTick;
        outlet.addProducedSteam(steamProducedThisTick);
        return productionRateChanged;
    }

    double getProductionRatio() {
        double fullLoadProductionRate = getFullLoadProductionRate();
        if (fullLoadProductionRate <= 0) {
            return 0;
        }

        return Mth.clamp(currentProductionRate / fullLoadProductionRate, 0.0, 1.0);
    }

    void write(CompoundTag compoundTag, boolean clientPacket) {
        if (clientPacket) {
            compoundTag.putDouble(COMPOUND_KEY_PRODUCTION_RATE, currentProductionRate);
            return;
        }

        compoundTag.putDouble(COMPOUND_KEY_PRODUCTION_REMAINDER, productionRemainder);
    }

    void read(CompoundTag compoundTag, boolean clientPacket) {
        if (clientPacket) {
            currentProductionRate = Math.max(0, compoundTag.getDouble(COMPOUND_KEY_PRODUCTION_RATE));
        }
        else {
            currentProductionRate = 0;
            double savedProductionRemainder = compoundTag.getDouble(COMPOUND_KEY_PRODUCTION_REMAINDER);
            productionRemainder = GasConsumptionMath.isFinite(savedProductionRemainder) && savedProductionRemainder >= 0 && savedProductionRemainder < 1 ? savedProductionRemainder : 0;
        }
        resetTickAccounting();
    }

    void invalidateCurrentTick() {
        resetTickAccounting();
    }

    private void resetTickAccounting() {
        accountingTick = Long.MIN_VALUE;
    }

    private double getMaximumProductionRate() {
        if (!BoilerSteamOutletBlock.isActive(outlet.getBlockState())) {
            return 0;
        }

        FluidTankBlockEntity controllerTank = getControllerTank();
        if (controllerTank == null || !BoilerSteamOutletIntegration.ensureVerified(controllerTank) || !controllerTank.boiler.isActive()) {
            return 0;
        }

        double boilerEfficiency = controllerTank.boiler.getEngineEfficiency(controllerTank.getTotalTankSize());
        if (!GasConsumptionMath.isFinite(boilerEfficiency)) {
            return 0;
        }

        double productionRate = getFullLoadProductionRate() * Mth.clamp(boilerEfficiency, 0.0, 1.0);
        if (!GasConsumptionMath.isFinite(productionRate) || productionRate <= 0) {
            return 0;
        }

        return productionRate;
    }

    private @Nullable FluidTankBlockEntity getControllerTank() {
        Level level = outlet.getLevel();
        if (level == null) {
            return null;
        }

        BlockPos attachedTankPos = BoilerSteamOutletBlock.getAttachedTankPos(outlet.getBlockState(), outlet.getBlockPos());
        if (!(level.getBlockEntity(attachedTankPos) instanceof FluidTankBlockEntity tank)) {
            return null;
        }

        return tank.getControllerBE();
    }
}
