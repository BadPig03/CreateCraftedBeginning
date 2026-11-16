package net.ty.createcraftedbeginning.content.airtights.airtightregulatorpump;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureBoundary;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasTransportEdgeProperties;
import net.ty.createcraftedbeginning.gas.network.GasTransportPressureDrive;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightRegulatorPumpTransportBehaviour extends GasTransportBehaviour {
    private static final String COMPOUND_KEY_EXHAUST = "RegulatorExhaust";
    private static final long REGULATION_BAND_PA = 1000;

    private final AirtightRegulatorPumpBlockEntity regulatorPump;
    private final AirtightRegulatorPumpExhaust exhaust;

    AirtightRegulatorPumpTransportBehaviour(AirtightRegulatorPumpBlockEntity regulatorPump) {
        super(regulatorPump);
        this.regulatorPump = regulatorPump;
        exhaust = new AirtightRegulatorPumpExhaust(regulatorPump);
    }

    @Override
    public void read(CompoundTag tag, Provider provider, boolean clientPacket) {
        super.read(tag, provider, clientPacket);
        if (clientPacket) {
            return;
        }

        exhaust.read(tag.getCompound(COMPOUND_KEY_EXHAUST), provider);
    }

    @Override
    public void write(CompoundTag tag, Provider provider, boolean clientPacket) {
        super.write(tag, provider, clientPacket);
        if (clientPacket) {
            return;
        }

        tag.put(COMPOUND_KEY_EXHAUST, exhaust.write(provider));
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
        return isConnectionFaceEnabled(state, direction);
    }

    @Override
    public boolean allowsOutboundFlow(BlockState state, Direction direction) {
        return isConnectionFaceEnabled(state, direction) && direction == AirtightRegulatorPumpBlock.getOutputDirection(state);
    }

    @Override
    public @Nullable GasPressureBoundary getInternalPressureBoundary(Direction face) {
        if (face != AirtightRegulatorPumpBlock.getOutputDirection(regulatorPump.getBlockState())) {
            return null;
        }

        return exhaust;
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
        double regulationConductance = flowRateLimit <= 0 ? Double.NaN : (double) flowRateLimit / REGULATION_BAND_PA;
        return new GasTransportEdgeProperties(resistanceUnits, GasTransportPressureDrive.outletPressureTarget(outletSetPressurePa, maximumPressureRisePa), flowRateLimit, GasPressure.REFERENCE_PRESSURE_PA, regulationConductance, true, true);
    }
}
