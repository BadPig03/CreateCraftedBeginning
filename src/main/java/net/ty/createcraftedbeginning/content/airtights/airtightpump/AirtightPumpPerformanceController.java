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

    private static boolean isSideAccessible(BlockState state, Direction direction) {
        return state.getBlock() instanceof AirtightPumpBlock && state.getValue(AirtightPumpBlock.FACING).getAxis() == direction.getAxis();
    }

    boolean allowsGasTransport(BlockState state, Direction direction) {
        return isPumpRunning() && isSideAccessible(state, direction) && direction == getPumpInletDirection();
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

    boolean isSideAccessible(Direction direction) {
        return isSideAccessible(pump.getBlockState(), direction);
    }

    Direction getPumpInletDirection() {
        return getFront().getOpposite();
    }

    Direction getPumpOutletDirection() {
        return getFront();
    }

    private boolean isPumpRunning() {
        return pump.getLevel() != null && !pump.isRemoved() && pump.getBlockState().getBlock() instanceof AirtightPumpBlock block && Mth.abs(pump.getSpeed()) >= block.getMinimumRequiredSpeedLevel().getSpeedValue();
    }

    private double getSpeedProgress() {
        double maximumSpeed = Math.max(1, AllConfigs.server().kinetics.maxRotationSpeed.get());
        return Mth.clamp(Mth.abs(pump.getSpeed()) / maximumSpeed, 0, 1);
    }

    private Direction getFront() {
        return pump.getBlockState().getValue(AirtightPumpBlock.FACING);
    }
}
