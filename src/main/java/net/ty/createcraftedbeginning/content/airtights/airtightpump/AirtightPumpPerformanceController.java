package net.ty.createcraftedbeginning.content.airtights.airtightpump;

import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.config.CCBConfig;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightPumpPerformanceController {
    private final AirtightPumpBlockEntity pump;

    AirtightPumpPerformanceController(AirtightPumpBlockEntity pump) {
        this.pump = pump;
    }

    static boolean isSideAccessible(BlockState state, Direction direction) {
        return state.getBlock() instanceof AirtightPumpBlock && state.getValue(AirtightPumpBlock.FACING).getAxis() == direction.getAxis();
    }

    static Direction getPumpInletDirection(BlockState state) {
        return getPumpOutletDirection(state).getOpposite();
    }

    static Direction getPumpOutletDirection(BlockState state) {
        return state.getValue(AirtightPumpBlock.FACING);
    }

    boolean allowsGasTransport(BlockState state, Direction direction) {
        return isPumpRunning() && isSideAccessible(state, direction) && direction == getPumpInletDirection(state);
    }

    long getPumpMaxPressureBoostPa() {
        if (!isPumpRunning()) {
            return GasPressure.VACUUM_PA;
        }

        double maxPressureBoostAtm = CCBConfig.server().machines.airtightPump.maxPressureBoost.getF();
        return GasPressure.pascals(maxPressureBoostAtm * getSpeedProgress());
    }

    long getPumpFlowRateLimit() {
        if (!isPumpRunning()) {
            return 0;
        }

        long configuredMaximum = Math.max(0, CCBConfig.server().machines.airtightPump.maxFlowPerTick.get());
        if (configuredMaximum <= 0) {
            return 0;
        }

        return Math.max(1, Math.round(configuredMaximum * getSpeedProgress()));
    }

    private boolean isPumpRunning() {
        if (pump.getLevel() == null || pump.isRemoved() || !(pump.getBlockState().getBlock() instanceof AirtightPumpBlock block)) {
            return false;
        }

        float absoluteSpeed = Mth.abs(pump.getSpeed());
        return absoluteSpeed > 0 && absoluteSpeed >= block.getMinimumRequiredSpeedLevel().getSpeedValue();
    }

    private double getSpeedProgress() {
        double maximumSpeed = Math.max(1, AllConfigs.server().kinetics.maxRotationSpeed.get());
        return Mth.clamp(Mth.abs(pump.getSpeed()) / maximumSpeed, 0, 1);
    }
}
