package net.ty.createcraftedbeginning.content.airtights.boilersteamoutlet;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.SmartGasTank;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BoilerSteamOutletProduction {
    static final int STRESS_PER_STEAM_MB = 1024;
    private static final int STEAM_ENGINE_BASE_SPEED = 16;

    private final BoilerSteamOutletBlockEntity outlet;

    private double productionRemainder;
    private long accountingTick = Long.MIN_VALUE;
    private long productionThisTick;

    BoilerSteamOutletProduction(BoilerSteamOutletBlockEntity outlet) {
        this.outlet = outlet;
    }

    static long getMaximumOutputCapacity() {
        double capacity = getFullLoadProductionRate();
        if (!Double.isFinite(capacity) || capacity <= 0) {
            return 1;
        }

        if (capacity >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return Math.max(1, (long) Math.ceil(capacity));
    }

    private static double getFullLoadProductionRate() {
        double fullLoadStress = STEAM_ENGINE_BASE_SPEED * BlockStressValues.getCapacity(AllBlocks.STEAM_ENGINE.get());
        return fullLoadStress / STRESS_PER_STEAM_MB;
    }

    void ensureCurrentTick() {
        Level level = outlet.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        long gameTime = level.getGameTime();
        if (accountingTick == gameTime) {
            return;
        }

        accountingTick = gameTime;
        double idealProduction = getMaximumProductionRate();
        if (!Double.isFinite(idealProduction) || idealProduction <= 0) {
            productionThisTick = 0;
            setAvailableSteam(0);
            return;
        }

        double availableProduction = productionRemainder + idealProduction;
        productionThisTick = availableProduction >= Long.MAX_VALUE ? Long.MAX_VALUE : Mth.lfloor(availableProduction);
        productionRemainder = availableProduction >= Long.MAX_VALUE ? 0 : availableProduction - productionThisTick;
        setAvailableSteam(productionThisTick);
    }

    void resetTickAccounting() {
        accountingTick = Long.MIN_VALUE;
        productionThisTick = 0;
        setAvailableSteam(0);
    }

    private void setAvailableSteam(long amount) {
        SmartGasTankBehaviour steamTank = outlet.getSteamTankBehaviour();
        if (steamTank == null) {
            return;
        }

        long capacity = getMaximumOutputCapacity();
        long clamped = Math.clamp(amount, 0, capacity);
        SmartGasTank tank = steamTank.getPrimaryHandler();
        tank.setCapacity(capacity);
        GasStack replacement = clamped <= 0 ? GasStack.EMPTY : new GasStack(CCBGases.STEAM.get(), clamped);
        GasStack current = tank.getGasStack();
        boolean unchanged = replacement.isEmpty() && current.isEmpty() || replacement.getAmount() == current.getAmount() && GasStack.isSameGasSameComponents(replacement, current);
        if (unchanged) {
            return;
        }

        tank.setGasStack(replacement);
    }

    private double getMaximumProductionRate() {
        if (!BoilerSteamOutletBlock.isActive(outlet.getBlockState())) {
            return 0;
        }

        FluidTankBlockEntity controller = getControllerTank();
        if (controller == null || !controller.boiler.isActive()) {
            return 0;
        }

        double efficiency = controller.boiler.getEngineEfficiency(controller.getTotalTankSize());
        double production = getFullLoadProductionRate() * efficiency;
        return Double.isFinite(production) && production > 0 ? production : 0;
    }

    private @Nullable FluidTankBlockEntity getControllerTank() {
        Level level = outlet.getLevel();
        if (level == null) {
            return null;
        }

        BlockPos tankPos = BoilerSteamOutletBlock.getAttachedTankPos(outlet.getBlockState(), outlet.getBlockPos());
        BlockEntity blockEntity = level.getBlockEntity(tankPos);
        if (!(blockEntity instanceof FluidTankBlockEntity tank)) {
            return null;
        }
        return tank.getControllerBE();
    }
}
