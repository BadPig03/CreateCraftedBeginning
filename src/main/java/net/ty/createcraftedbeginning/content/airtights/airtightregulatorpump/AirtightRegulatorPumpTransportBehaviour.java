package net.ty.createcraftedbeginning.content.airtights.airtightregulatorpump;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasTransportEdgeProperties;
import net.ty.createcraftedbeginning.gas.network.GasTransportPressureDrive;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightRegulatorPumpTransportBehaviour extends GasTransportBehaviour {
    private final AirtightRegulatorPumpBlockEntity regulatorPump;

    AirtightRegulatorPumpTransportBehaviour(AirtightRegulatorPumpBlockEntity regulatorPump) {
        super(regulatorPump);
        this.regulatorPump = regulatorPump;
    }

    @Override
    public boolean canConnectOnFace(BlockState state, Direction direction) {
        return isConnectionFaceEnabled(state, direction);
    }

    @Override
    public boolean isConnectionFaceEnabled(BlockState state, Direction direction) {
        return direction.getAxis() == AirtightRegulatorPumpBlock.getOutputDirection(state).getAxis();
    }

    @Override
    public boolean allowsInboundFlow(BlockState state, Direction direction) {
        return isConnectionFaceEnabled(state, direction) && direction == AirtightRegulatorPumpBlock.getInputDirection(state);
    }

    @Override
    public boolean allowsOutboundFlow(BlockState state, Direction direction) {
        return isConnectionFaceEnabled(state, direction) && direction == AirtightRegulatorPumpBlock.getOutputDirection(state);
    }

    @Override
    public GasTransportEdgeProperties getTransportEdgeProperties(BlockState state, Direction entryFace, Direction exitFace, GasStack gasStack) {
        long resistanceUnits = getFlowResistanceUnits(state, entryFace, exitFace);
        if (entryFace != AirtightRegulatorPumpBlock.getInputDirection(state) || exitFace != AirtightRegulatorPumpBlock.getOutputDirection(state)) {
            return GasTransportEdgeProperties.passive(resistanceUnits);
        }

        long outletSetPressurePa = Mth.clamp(regulatorPump.getOutletSetPressurePa(), GasPressure.VACUUM_PA, AirtightRegulatorPumpPressureBehaviour.maxOutletSetPressurePa());
        long maximumPressureRisePa = Math.max(GasPressure.VACUUM_PA, regulatorPump.getMaxPressureRisePa());
        long flowRateLimit = Math.max(0, regulatorPump.getFlowRateLimit());
        long regulationBandPa = Math.max(1, GasPressure.REFERENCE_PRESSURE_PA);
        double regulationConductance = flowRateLimit <= 0 ? Double.NaN : (double) flowRateLimit / regulationBandPa;
        return new GasTransportEdgeProperties(resistanceUnits, GasTransportPressureDrive.outletPressureTarget(outletSetPressurePa, maximumPressureRisePa), flowRateLimit, GasPressure.REFERENCE_PRESSURE_PA, regulationConductance, true, true);
    }
}
