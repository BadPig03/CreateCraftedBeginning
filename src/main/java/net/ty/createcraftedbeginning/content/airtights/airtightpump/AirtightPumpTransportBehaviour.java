package net.ty.createcraftedbeginning.content.airtights.airtightpump;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasTransportEdgeProperties;
import net.ty.createcraftedbeginning.gas.network.GasTransportPressureDrive;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightPumpTransportBehaviour extends GasTransportBehaviour {
    private final AirtightPumpBlockEntity pump;

    AirtightPumpTransportBehaviour(AirtightPumpBlockEntity pump) {
        super(pump);
        this.pump = pump;
    }

    @Override
    public boolean canConnectOnFace(BlockState state, Direction direction) {
        return isConnectionFaceEnabled(state, direction);
    }

    @Override
    public boolean isConnectionFaceEnabled(BlockState state, Direction direction) {
        return AirtightPumpPerformanceController.isSideAccessible(state, direction);
    }

    @Override
    public boolean allowsInboundFlow(BlockState state, Direction direction) {
        return isConnectionFaceEnabled(state, direction) && direction == AirtightPumpPerformanceController.getPumpInletDirection(state);
    }

    @Override
    public boolean allowsOutboundFlow(BlockState state, Direction direction) {
        return isConnectionFaceEnabled(state, direction) && direction == AirtightPumpPerformanceController.getPumpOutletDirection(state);
    }

    @Override
    public boolean passesNaturalForwardFlow() {
        return true;
    }

    @Override
    public GasTransportEdgeProperties getTransportEdgeProperties(BlockState state, Direction entryFace, Direction exitFace, GasStack gasStack) {
        long resistanceUnits = getFlowResistanceUnits(state, entryFace, exitFace);
        if (!allowsInboundFlow(state, entryFace) || !allowsOutboundFlow(state, exitFace)) {
            return GasTransportEdgeProperties.passive(resistanceUnits);
        }

        long maxPressureBoostPa = Math.max(GasPressure.VACUUM_PA, pump.getPumpMaxPressureBoostPa());
        long flowRateLimit = Math.max(0, pump.getPumpFlowRateLimit());
        return new GasTransportEdgeProperties(resistanceUnits, GasTransportPressureDrive.pressureBoost(maxPressureBoostPa), flowRateLimit, GasPressure.VACUUM_PA, Double.NaN, true, true);
    }

    @Override
    protected long getFlowResistanceUnits(BlockState state, Direction entryFace, Direction exitFace) {
        return 0;
    }
}
