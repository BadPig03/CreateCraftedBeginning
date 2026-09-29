package net.ty.createcraftedbeginning.gametests.gas;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtightencasedpipe.AirtightEncasedPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankBlockEntity;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection.FlowDirection;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection.FlowState;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasPumpLimitGameTests {
    private static final long REFERENCE_PRESSURE_PA = GasPressure.REFERENCE_PRESSURE_PA;
    private static final long HIGH_PRESSURE_PA = GasPressure.pascals(16);
    private static final long LOW_INLET_PRESSURE_PA = GasPressure.REFERENCE_PRESSURE_PA / 2;
    private static final long MAX_FACE_FLOW_QUANTIZATION_ERROR_GU = 2;

    private static final BlockPos BRANCH_SOURCE_POS = new BlockPos(1, 1, 3);
    private static final BlockPos BRANCH_PUMP_POS = new BlockPos(2, 1, 3);
    private static final BlockPos BRANCH_MANIFOLD_POS = new BlockPos(3, 1, 3);
    private static final BlockPos BRANCH_EAST_SINK_POS = new BlockPos(4, 1, 3);
    private static final BlockPos BRANCH_SOUTH_SINK_POS = new BlockPos(3, 1, 4);
    private static final BlockPos BRANCH_COG_POS = new BlockPos(2, 1, 2);
    private static final BlockPos BRANCH_MOTOR_POS = new BlockPos(3, 1, 2);

    private static final BlockPos LOW_PRESSURE_SOURCE_POS = new BlockPos(0, 1, 1);
    private static final BlockPos LOW_PRESSURE_PUMP_POS = new BlockPos(1, 1, 1);
    private static final BlockPos LOW_PRESSURE_SINK_POS = new BlockPos(2, 1, 1);
    private static final BlockPos LOW_PRESSURE_COG_POS = new BlockPos(1, 1, 2);
    private static final BlockPos LOW_PRESSURE_MOTOR_POS = new BlockPos(2, 1, 2);

    private GasPumpLimitGameTests() {
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 120)
    public static void airtightPumpSharesMaximumFlowAcrossBranches(GameTestHelper helper) {
        CreativeAirtightTankBlockEntity sourceTank = placeBoundaryTank(helper, BRANCH_SOURCE_POS, HIGH_PRESSURE_PA);
        CreativeAirtightTankBlockEntity eastSinkTank = placeBoundaryTank(helper, BRANCH_EAST_SINK_POS, REFERENCE_PRESSURE_PA);
        CreativeAirtightTankBlockEntity southSinkTank = placeBoundaryTank(helper, BRANCH_SOUTH_SINK_POS, REFERENCE_PRESSURE_PA);
        AirtightPumpBlockEntity pump = placePoweredPump(helper, BRANCH_PUMP_POS, BRANCH_COG_POS, BRANCH_MOTOR_POS);
        helper.setBlock(BRANCH_MANIFOLD_POS, encasedPipeState());

        helper.assertValueEqual(sourceTank.getTankInventory().getPressurePa(), HIGH_PRESSURE_PA, "branched pump source pressure");
        helper.assertValueEqual(eastSinkTank.getTankInventory().getPressurePa(), REFERENCE_PRESSURE_PA, "branched pump east sink pressure");
        helper.assertValueEqual(southSinkTank.getTankInventory().getPressurePa(), REFERENCE_PRESSURE_PA, "branched pump south sink pressure");

        helper.succeedWhen(() -> {
            assertPumpPowered(helper, pump);
            long flowRateLimit = pump.getPumpFlowRateLimit();
            helper.assertTrue(flowRateLimit > 0, "Powered airtight pump reported a zero flow-rate limit at speed " + pump.getSpeed());

            GasTransportBehaviour pumpTransport = transport(helper, BRANCH_PUMP_POS);
            FlowState pumpInletFlow = pumpTransport.getFlowState(Direction.WEST);
            FlowState pumpOutletFlow = pumpTransport.getFlowState(Direction.EAST);
            helper.assertTrue(pumpInletFlow != null, "Maximum-flow pump did not create inlet flow");
            if (pumpInletFlow == null) {
                throw new NullPointerException("Maximum-flow pump did not create inlet flow.");
            }

            helper.assertTrue(pumpOutletFlow != null, "Maximum-flow pump did not create outlet flow");
            if (pumpOutletFlow == null) {
                throw new NullPointerException("Maximum-flow pump did not create outlet flow.");
            }

            helper.assertTrue(pumpInletFlow.direction() == FlowDirection.INBOUND, "Maximum-flow pump west face was not inbound");
            helper.assertTrue(pumpOutletFlow.direction() == FlowDirection.OUTBOUND, "Maximum-flow pump east face was not outbound");
            helper.assertValueEqual(pumpInletFlow.flowRate(), flowRateLimit, "branched airtight pump capped inlet flow rate");
            helper.assertValueEqual(pumpOutletFlow.flowRate(), flowRateLimit, "branched airtight pump capped outlet flow rate");
            helper.assertValueEqual(pumpTransport.getThroughputFlowRate(), flowRateLimit, "branched airtight pump capped throughput");
            helper.assertTrue(pumpInletFlow.gas().is(CCBGases.NATURAL_AIR.get()), "Maximum-flow pump inlet gas was not Natural Air");
            helper.assertTrue(pumpOutletFlow.gas().is(CCBGases.NATURAL_AIR.get()), "Maximum-flow pump outlet gas was not Natural Air");

            GasTransportBehaviour manifold = transport(helper, BRANCH_MANIFOLD_POS);
            FlowState manifoldInletFlow = manifold.getFlowState(Direction.WEST);
            FlowState eastBranchFlow = manifold.getFlowState(Direction.EAST);
            FlowState southBranchFlow = manifold.getFlowState(Direction.SOUTH);
            helper.assertTrue(manifoldInletFlow != null, "Maximum-flow pump did not feed the downstream manifold");
            if (manifoldInletFlow == null) {
                throw new NullPointerException("Maximum-flow pump did not feed the downstream manifold.");
            }

            helper.assertTrue(eastBranchFlow != null, "Maximum-flow manifold east branch did not receive gas");
            if (eastBranchFlow == null) {
                throw new NullPointerException("Maximum-flow manifold east branch did not receive gas.");
            }

            helper.assertTrue(southBranchFlow != null, "Maximum-flow manifold south branch did not receive gas");
            if (southBranchFlow == null) {
                throw new NullPointerException("Maximum-flow manifold south branch did not receive gas.");
            }

            helper.assertTrue(manifoldInletFlow.direction() == FlowDirection.INBOUND, "Maximum-flow manifold west face was not inbound");
            helper.assertTrue(eastBranchFlow.direction() == FlowDirection.OUTBOUND, "Maximum-flow manifold east branch was not outbound");
            helper.assertTrue(southBranchFlow.direction() == FlowDirection.OUTBOUND, "Maximum-flow manifold south branch was not outbound");
            helper.assertTrue(eastBranchFlow.flowRate() > 0, "Maximum-flow manifold east branch flow was not positive");
            helper.assertTrue(southBranchFlow.flowRate() > 0, "Maximum-flow manifold south branch flow was not positive");
            helper.assertTrue(eastBranchFlow.gas().is(CCBGases.NATURAL_AIR.get()), "Maximum-flow manifold east branch gas was not Natural Air");
            helper.assertTrue(southBranchFlow.gas().is(CCBGases.NATURAL_AIR.get()), "Maximum-flow manifold south branch gas was not Natural Air");

            long branchTotal = eastBranchFlow.flowRate() + southBranchFlow.flowRate();
            helper.assertTrue(Math.abs(branchTotal - flowRateLimit) <= MAX_FACE_FLOW_QUANTIZATION_ERROR_GU, "Pump maximum flow budget was not shared across both outlet branches: branches=" + branchTotal + ", pumpLimit=" + flowRateLimit + " GU/t");
            helper.assertTrue(Math.abs(manifoldInletFlow.flowRate() - flowRateLimit) <= MAX_FACE_FLOW_QUANTIZATION_ERROR_GU, "Downstream manifold inlet did not carry the pump maximum flow budget: inlet=" + manifoldInletFlow.flowRate() + ", pumpLimit=" + flowRateLimit + " GU/t");
            helper.assertTrue(Math.abs(manifold.getThroughputFlowRate() - flowRateLimit) <= MAX_FACE_FLOW_QUANTIZATION_ERROR_GU, "Downstream manifold throughput did not match the pump maximum flow budget: throughput=" + manifold.getThroughputFlowRate() + ", pumpLimit=" + flowRateLimit + " GU/t");
            helper.assertTrue(Math.abs(eastBranchFlow.flowRate() - southBranchFlow.flowRate()) <= 1, "Symmetric maximum-flow branches did not split the shared pump budget evenly: east=" + eastBranchFlow.flowRate() + ", south=" + southBranchFlow.flowRate() + " GU/t");
        });
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 120)
    public static void airtightPumpScalesFlowLimitWithLowInletPressure(GameTestHelper helper) {
        CreativeAirtightTankBlockEntity sourceTank = placeBoundaryTank(helper, LOW_PRESSURE_SOURCE_POS, LOW_INLET_PRESSURE_PA);
        CreativeAirtightTankBlockEntity sinkTank = placeBoundaryTank(helper, LOW_PRESSURE_SINK_POS, LOW_INLET_PRESSURE_PA);
        AirtightPumpBlockEntity pump = placePoweredPump(helper, LOW_PRESSURE_PUMP_POS, LOW_PRESSURE_COG_POS, LOW_PRESSURE_MOTOR_POS);

        helper.assertValueEqual(sourceTank.getTankInventory().getPressurePa(), LOW_INLET_PRESSURE_PA, "low-pressure pump source pressure");
        helper.assertValueEqual(sinkTank.getTankInventory().getPressurePa(), LOW_INLET_PRESSURE_PA, "low-pressure pump sink pressure");
        helper.assertValueEqual(sourceTank.getTankInventory().getPressurePa(), sinkTank.getTankInventory().getPressurePa(), "low-pressure pump equal boundary pressure");

        helper.succeedWhen(() -> {
            assertPumpPowered(helper, pump);
            long fullPressureFlowRateLimit = pump.getPumpFlowRateLimit();
            helper.assertTrue(fullPressureFlowRateLimit > 0, "Powered airtight pump reported a zero full-pressure flow-rate limit at speed " + pump.getSpeed());
            helper.assertTrue(pump.getPumpMaxPressureBoostPa() > GasPressure.VACUUM_PA, "Powered airtight pump reported no pressure boost during low-inlet-pressure test");

            GasTransportBehaviour transport = transport(helper, LOW_PRESSURE_PUMP_POS);
            FlowState inletFlow = transport.getFlowState(Direction.WEST);
            FlowState outletFlow = transport.getFlowState(Direction.EAST);
            helper.assertTrue(inletFlow != null, "Low-inlet-pressure pump did not create inlet flow");
            if (inletFlow == null) {
                throw new NullPointerException("Low-inlet-pressure pump did not create inlet flow.");
            }

            helper.assertTrue(outletFlow != null, "Low-inlet-pressure pump did not create outlet flow");
            if (outletFlow == null) {
                throw new NullPointerException("Low-inlet-pressure pump did not create outlet flow.");
            }

            helper.assertTrue(inletFlow.direction() == FlowDirection.INBOUND, "Low-inlet-pressure pump west face was not inbound");
            helper.assertTrue(outletFlow.direction() == FlowDirection.OUTBOUND, "Low-inlet-pressure pump east face was not outbound");
            helper.assertTrue(inletFlow.flowRate() > 0, "Low-inlet-pressure pump flow rate was not positive");
            helper.assertValueEqual(outletFlow.flowRate(), inletFlow.flowRate(), "low-inlet-pressure pump face flow rate");
            helper.assertValueEqual(transport.getThroughputFlowRate(), inletFlow.flowRate(), "low-inlet-pressure pump throughput");
            helper.assertTrue(inletFlow.gas().is(CCBGases.NATURAL_AIR.get()), "Low-inlet-pressure pump inlet gas was not Natural Air");
            helper.assertTrue(outletFlow.gas().is(CCBGases.NATURAL_AIR.get()), "Low-inlet-pressure pump outlet gas was not Natural Air");

            long boundaryScaledCeiling = Mth.lfloor((double) fullPressureFlowRateLimit * LOW_INLET_PRESSURE_PA / REFERENCE_PRESSURE_PA);
            helper.assertTrue(boundaryScaledCeiling > 0 && boundaryScaledCeiling < fullPressureFlowRateLimit, "Low-inlet-pressure test did not create a reduced boundary-scaled pump ceiling");
            helper.assertTrue(inletFlow.flowRate() <= boundaryScaledCeiling, "Low-inlet-pressure pump exceeded the maximum throughput available from its sub-atmospheric source: flow=" + inletFlow.flowRate() + ", boundaryCeiling=" + boundaryScaledCeiling + " GU/t");
            helper.assertTrue(inletFlow.flowRate() < fullPressureFlowRateLimit, "Low-inlet-pressure pump incorrectly retained its full 1-atm flow-rate limit");
        });
    }

    private static CreativeAirtightTankBlockEntity placeBoundaryTank(GameTestHelper helper, BlockPos pos, long pressurePa) {
        helper.setBlock(pos, CCBBlocks.CREATIVE_AIRTIGHT_TANK_BLOCK.get().defaultBlockState());
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        helper.assertTrue(blockEntity instanceof CreativeAirtightTankBlockEntity, "Creative airtight tank was not initialized at " + pos);
        if (!(blockEntity instanceof CreativeAirtightTankBlockEntity tank)) {
            throw new IllegalStateException("Creative airtight tank was not initialized at " + pos + '.');
        }

        tank.getTankInventory().setFixedPressurePa(pressurePa);
        tank.getTankInventory().setContainedGas(new GasStack(CCBGases.NATURAL_AIR.get(), 1));
        return tank;
    }

    private static AirtightPumpBlockEntity placePoweredPump(GameTestHelper helper, BlockPos pumpPos, BlockPos cogPos, BlockPos motorPos) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(pumpPos, CCBBlocks.AIRTIGHT_PUMP_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST));
        helper.setBlock(cogPos, AllBlocks.COGWHEEL.getDefaultState().setValue(CogWheelBlock.AXIS, Axis.X));
        helper.setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.WEST));

        BlockEntity pumpBlockEntity = level.getBlockEntity(helper.absolutePos(pumpPos));
        helper.assertTrue(pumpBlockEntity instanceof AirtightPumpBlockEntity, "Airtight pump was not initialized at " + pumpPos);
        if (!(pumpBlockEntity instanceof AirtightPumpBlockEntity pump)) {
            throw new IllegalStateException("Airtight pump was not initialized at " + pumpPos + '.');
        }

        BlockEntity motorBlockEntity = level.getBlockEntity(helper.absolutePos(motorPos));
        helper.assertTrue(motorBlockEntity instanceof CreativeMotorBlockEntity, "Creative motor was not initialized at " + motorPos);
        if (!(motorBlockEntity instanceof CreativeMotorBlockEntity motor)) {
            throw new IllegalStateException("Creative motor was not initialized at " + motorPos + '.');
        }

        motor.generatedSpeed.setValue(CreativeMotorBlockEntity.MAX_SPEED);
        return pump;
    }

    private static void assertPumpPowered(GameTestHelper helper, AirtightPumpBlockEntity pump) {
        helper.assertTrue(Mth.abs(pump.getSpeed()) >= SpeedLevel.MEDIUM.getSpeedValue(), "Airtight pump did not receive sustained medium-or-faster kinetic power");
    }

    private static BlockState encasedPipeState() {
        BlockState state = CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK.get().defaultBlockState();
        for (Direction face : new Direction[]{Direction.WEST, Direction.EAST, Direction.SOUTH}) {
            state = state.setValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(face), true);
        }
        return state;
    }

    private static GasTransportBehaviour transport(GameTestHelper helper, BlockPos pos) {
        GasTransportBehaviour transport = BlockEntityBehaviour.get(helper.getLevel(), helper.absolutePos(pos), GasTransportBehaviour.TYPE);
        helper.assertTrue(transport != null, "GasTransportBehaviour was not initialized at " + pos);
        if (transport == null) {
            throw new NullPointerException("GasTransportBehaviour was not initialized at " + pos + '.');
        }

        return transport;
    }
}
