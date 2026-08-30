package net.ty.createcraftedbeginning.content.airtights.boilersteamoutlet;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gascanisters.GasConsumptions;
import net.ty.createcraftedbeginning.foundation.CCBNbtUtils;
import net.ty.createcraftedbeginning.foundation.CCBMathUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BoilerSteamOutletProduction {
    private static final int STRESS_PER_STEAM_MB = 1024;
    private static final int TICKS_PER_SECOND = 20;

    private static final String COMPOUND_KEY_PRODUCTION_RATE = "ProductionRate";
    private static final String COMPOUND_KEY_PRODUCTION_REMAINDER = "ProductionRemainder";

    private final BoilerSteamOutletBlockEntity outlet;

    private double productionRemainder;
    private double currentProductionRate;
    private long accountingTick = Long.MIN_VALUE;

    BoilerSteamOutletProduction(BoilerSteamOutletBlockEntity outlet) {
        this.outlet = outlet;
    }

    static long getMaximumOutputCapacity() {
        double fullLoadProductionRate = getFullLoadProductionRate();
        if (!GasConsumptions.isFinite(fullLoadProductionRate) || fullLoadProductionRate <= 0) {
            return 1;
        }

        if (fullLoadProductionRate >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return Math.max(1, (long) Math.ceil(fullLoadProductionRate));
    }

    private static double getFullLoadProductionRate() {
        return BoilerSteamOutletIntegration.getSteamEngineFullLoadStressCapacity() / STRESS_PER_STEAM_MB;
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
        double maximumProductionRate = getMaximumProductionRate();
        boolean productionRateChanged = Double.compare(currentProductionRate, maximumProductionRate) != 0;
        currentProductionRate = maximumProductionRate;
        if (!GasConsumptions.isFinite(maximumProductionRate) || maximumProductionRate <= 0) {
            setAvailableSteam(0);
            return productionRateChanged;
        }

        double accumulatedProduction = productionRemainder + maximumProductionRate;
        long steamProducedThisTick = accumulatedProduction >= Long.MAX_VALUE ? Long.MAX_VALUE : Mth.lfloor(accumulatedProduction);
        productionRemainder = accumulatedProduction >= Long.MAX_VALUE ? 0 : accumulatedProduction - steamProducedThisTick;
        setAvailableSteam(steamProducedThisTick);
        return productionRateChanged;
    }

    double getProductionRatePerSecond() {
        return currentProductionRate * TICKS_PER_SECOND;
    }

    void write(CompoundTag compoundTag, boolean clientPacket) {
        if (clientPacket) {
            CCBNbtUtils.putDouble(compoundTag, COMPOUND_KEY_PRODUCTION_RATE, currentProductionRate);
            return;
        }

        CCBNbtUtils.putDouble(compoundTag, COMPOUND_KEY_PRODUCTION_REMAINDER, productionRemainder);
    }

    void read(CompoundTag compoundTag, boolean clientPacket) {
        if (clientPacket) {
            currentProductionRate = Math.max(0, CCBNbtUtils.getDouble(compoundTag, COMPOUND_KEY_PRODUCTION_RATE));
        }
        else {
            currentProductionRate = 0;
            double savedProductionRemainder = CCBNbtUtils.getDouble(compoundTag, COMPOUND_KEY_PRODUCTION_REMAINDER);
            productionRemainder = GasConsumptions.isFinite(savedProductionRemainder) && savedProductionRemainder >= 0 && savedProductionRemainder < 1 ? savedProductionRemainder : 0;
        }
        resetTickAccounting();
    }

    private void resetTickAccounting() {
        accountingTick = Long.MIN_VALUE;
        setAvailableSteam(0);
    }

    private void setAvailableSteam(long availableSteam) {
        outlet.setAvailableSteamThisTick(availableSteam);
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
        if (!GasConsumptions.isFinite(boilerEfficiency)) {
            return 0;
        }

        double productionRate = getFullLoadProductionRate() * CCBMathUtils.clampUnit(boilerEfficiency);
        if (!GasConsumptions.isFinite(productionRate) || productionRate <= 0) {
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
