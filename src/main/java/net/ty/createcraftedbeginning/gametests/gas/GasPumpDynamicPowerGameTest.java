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
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankBlockEntity;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection.FlowDirection;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection.FlowState;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology.Snapshot;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasPumpDynamicPowerGameTest {
    private static final long BOUNDARY_PRESSURE_PA = GasPressure.REFERENCE_PRESSURE_PA;
    private static final int INITIAL_FLOW_DEADLINE_TICKS = 40;
    private static final int TRANSITION_DEADLINE_TICKS = 10;

    private static final BlockPos SOURCE_TANK_POS = new BlockPos(0, 1, 1);
    private static final BlockPos PUMP_POS = new BlockPos(1, 1, 1);
    private static final BlockPos SINK_TANK_POS = new BlockPos(2, 1, 1);
    private static final BlockPos COG_POS = new BlockPos(1, 1, 2);
    private static final BlockPos MOTOR_POS = new BlockPos(2, 1, 2);

    private GasPumpDynamicPowerGameTest() {
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void losingAndRestoringPumpPowerStopsAndRestoresActiveTransport(GameTestHelper helper) {
        placeBoundaryTank(helper, SOURCE_TANK_POS);
        placeBoundaryTank(helper, SINK_TANK_POS);
        helper.setBlock(PUMP_POS, CCBBlocks.AIRTIGHT_PUMP_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST));
        helper.setBlock(COG_POS, AllBlocks.COGWHEEL.getDefaultState().setValue(CogWheelBlock.AXIS, Axis.X));
        helper.setBlock(MOTOR_POS, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.WEST));

        AirtightPumpBlockEntity pump = pump(helper);
        CreativeMotorBlockEntity motor = motor(helper);
        GasTransportBehaviour transport = transport(helper);
        motor.generatedSpeed.setValue(CreativeMotorBlockEntity.MAX_SPEED);

        assertTopology(helper, "initial");

        long[] initialFlowRate = new long[1];
        long[] initialPressureBoostPa = new long[1];
        long[] initialFlowRateLimit = new long[1];
        int[] phase = new int[1];
        int[] testTicks = new int[1];
        int[] transitionTicks = new int[1];

        helper.onEachTick(() -> {
            testTicks[0]++;

            if (phase[0] == 0) {
                if (!isPumpPowered(pump) || !hasPositiveForwardFlow(transport)) {
                    if (testTicks[0] > INITIAL_FLOW_DEADLINE_TICKS) {
                        helper.fail("Airtight pump never established its initial powered flow between equal-pressure boundaries");
                    }
                    return;
                }

                initialPressureBoostPa[0] = pump.getPumpMaxPressureBoostPa();
                initialFlowRateLimit[0] = pump.getPumpFlowRateLimit();
                helper.assertTrue(initialPressureBoostPa[0] > GasPressure.VACUUM_PA, "Powered airtight pump reported no initial pressure boost");
                helper.assertTrue(initialFlowRateLimit[0] > 0, "Powered airtight pump reported no initial flow-rate limit");
                initialFlowRate[0] = assertForwardFlow(helper, transport, "powered airtight pump before power loss");
                assertNoOffAxisFlow(helper, transport, "powered airtight pump before power loss");
                assertTopology(helper, "powered before power loss");

                motor.generatedSpeed.setValue(0);
                helper.assertValueEqual(motor.generatedSpeed.getValue(), 0, "creative motor commanded speed after power loss");

                phase[0] = 1;
                transitionTicks[0] = 0;
                return;
            }

            transitionTicks[0]++;
            if (phase[0] == 1) {
                assertTopology(helper, "unpowered transition");

                boolean pumpStopped = Mth.abs(pump.getSpeed()) < SpeedLevel.MEDIUM.getSpeedValue() && pump.getPumpMaxPressureBoostPa() == GasPressure.VACUUM_PA && pump.getPumpFlowRateLimit() == 0;
                boolean flowCleared = transport.getFlowState(Direction.WEST) == null && transport.getFlowState(Direction.EAST) == null && transport.getThroughputFlowRate() == 0;
                if (!pumpStopped || !flowCleared) {
                    if (transitionTicks[0] > TRANSITION_DEADLINE_TICKS) {
                        helper.fail("Airtight pump did not promptly lose active pressure boost and clear stale server-side flow state after kinetic power was removed");
                    }
                    return;
                }

                helper.assertValueEqual(pump.getPumpMaxPressureBoostPa(), GasPressure.VACUUM_PA, "unpowered airtight pump pressure boost");
                helper.assertValueEqual(pump.getPumpFlowRateLimit(), 0L, "unpowered airtight pump flow-rate limit");
                assertNoOffAxisFlow(helper, transport, "unpowered airtight pump");
                assertTopology(helper, "fully unpowered");

                motor.generatedSpeed.setValue(CreativeMotorBlockEntity.MAX_SPEED);
                helper.assertValueEqual(motor.generatedSpeed.getValue(), CreativeMotorBlockEntity.MAX_SPEED, "creative motor commanded speed after power restoration");

                phase[0] = 2;
                transitionTicks[0] = 0;
                return;
            }

            assertTopology(helper, "restoring power");
            if (!isPumpPowered(pump) || !hasPositiveForwardFlow(transport)) {
                if (transitionTicks[0] > TRANSITION_DEADLINE_TICKS) {
                    helper.fail("Airtight pump did not promptly restore active transport after kinetic power returned");
                }
                return;
            }

            helper.assertValueEqual(pump.getPumpMaxPressureBoostPa(), initialPressureBoostPa[0], "restored airtight pump pressure boost versus initial pressure boost");
            helper.assertValueEqual(pump.getPumpFlowRateLimit(), initialFlowRateLimit[0], "restored airtight pump flow-rate limit versus initial flow-rate limit");
            long restoredFlowRate = assertForwardFlow(helper, transport, "airtight pump after power restoration");
            helper.assertValueEqual(restoredFlowRate, initialFlowRate[0], "restored airtight pump flow rate versus initial powered flow rate");
            assertNoOffAxisFlow(helper, transport, "airtight pump after power restoration");
            assertTopology(helper, "restored");
            helper.succeed();
        });
    }

    private static boolean isPumpPowered(AirtightPumpBlockEntity pump) {
        return Mth.abs(pump.getSpeed()) >= SpeedLevel.MEDIUM.getSpeedValue() && pump.getPumpMaxPressureBoostPa() > GasPressure.VACUUM_PA && pump.getPumpFlowRateLimit() > 0;
    }

    private static boolean hasPositiveForwardFlow(GasTransportBehaviour transport) {
        return isPositiveFlow(transport.getFlowState(Direction.WEST), FlowDirection.INBOUND) && isPositiveFlow(transport.getFlowState(Direction.EAST), FlowDirection.OUTBOUND);
    }

    private static boolean isPositiveFlow(@Nullable FlowState flowState, FlowDirection expectedDirection) {
        return flowState != null && flowState.direction() == expectedDirection && flowState.flowRate() > 0;
    }

    private static long assertForwardFlow(GameTestHelper helper, GasTransportBehaviour transport, String stage) {
        FlowState west = transport.getFlowState(Direction.WEST);
        FlowState east = transport.getFlowState(Direction.EAST);
        helper.assertTrue(west != null, stage + " did not have west-face flow");
        if (west == null) {
            throw new NullPointerException("Required test object is missing: " + stage + " did not have west-face flow" + '.');
        }

        helper.assertTrue(east != null, stage + " did not have east-face flow");
        if (east == null) {
            throw new NullPointerException("Required test object is missing: " + stage + " did not have east-face flow" + '.');
        }

        helper.assertTrue(west.direction() == FlowDirection.INBOUND, stage + " west face was not inbound");
        helper.assertTrue(east.direction() == FlowDirection.OUTBOUND, stage + " east face was not outbound");
        helper.assertTrue(west.flowRate() > 0, stage + " flow rate was not positive");
        helper.assertValueEqual(east.flowRate(), west.flowRate(), stage + " face flow rate");
        helper.assertValueEqual(transport.getThroughputFlowRate(), west.flowRate(), stage + " throughput");
        helper.assertTrue(west.gas().is(CCBGases.NATURAL_AIR.get()), stage + " west-face gas was not Natural Air");
        helper.assertTrue(east.gas().is(CCBGases.NATURAL_AIR.get()), stage + " east-face gas was not Natural Air");
        return west.flowRate();
    }

    private static void assertNoOffAxisFlow(GameTestHelper helper, GasTransportBehaviour transport, String stage) {
        helper.assertTrue(transport.getFlowState(Direction.NORTH) == null, stage + " produced ghost north flow");
        helper.assertTrue(transport.getFlowState(Direction.SOUTH) == null, stage + " produced ghost south flow");
        helper.assertTrue(transport.getFlowState(Direction.UP) == null, stage + " produced ghost up flow");
        helper.assertTrue(transport.getFlowState(Direction.DOWN) == null, stage + " produced ghost down flow");
    }

    private static void assertTopology(GameTestHelper helper, String stage) {
        BlockPos absolutePumpPos = helper.absolutePos(PUMP_POS);
        Snapshot snapshot = GasNetworkTopology.get(helper.getLevel(), absolutePumpPos);
        helper.assertTrue(snapshot.pipePositions().size() == 1, stage + " pump topology did not contain exactly one gas transport node");
        helper.assertTrue(snapshot.pipePositions().contains(absolutePumpPos), stage + " pump topology lost the airtight pump");
        helper.assertTrue(snapshot.endpointFaces().size() == 2, stage + " pump topology did not contain exactly the source and sink endpoint faces");
        helper.assertTrue(snapshot.atmosphericFaces().isEmpty(), stage + " pump topology unexpectedly contained an atmospheric face");
    }

    private static AirtightPumpBlockEntity pump(GameTestHelper helper) {
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(PUMP_POS));
        helper.assertTrue(blockEntity instanceof AirtightPumpBlockEntity, "Airtight pump block entity was not initialized");
        if (!(blockEntity instanceof AirtightPumpBlockEntity pump)) {
            throw new IllegalStateException("Airtight pump block entity was not initialized at " + PUMP_POS + '.');
        }

        return pump;
    }

    private static CreativeMotorBlockEntity motor(GameTestHelper helper) {
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(MOTOR_POS));
        helper.assertTrue(blockEntity instanceof CreativeMotorBlockEntity, "Creative motor block entity was not initialized");
        if (!(blockEntity instanceof CreativeMotorBlockEntity motor)) {
            throw new IllegalStateException("Creative motor block entity was not initialized at " + MOTOR_POS + '.');
        }

        return motor;
    }

    private static GasTransportBehaviour transport(GameTestHelper helper) {
        GasTransportBehaviour transport = BlockEntityBehaviour.get(helper.getLevel(), helper.absolutePos(PUMP_POS), GasTransportBehaviour.TYPE);
        helper.assertTrue(transport != null, "Airtight pump transport behaviour was not initialized");
        if (transport == null) {
            throw new NullPointerException("Airtight pump transport behaviour was not initialized.");
        }

        return transport;
    }

    private static void placeBoundaryTank(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, CCBBlocks.CREATIVE_AIRTIGHT_TANK_BLOCK.get().defaultBlockState());
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        helper.assertTrue(blockEntity instanceof CreativeAirtightTankBlockEntity, "Creative airtight tank was not initialized at " + pos);
        if (!(blockEntity instanceof CreativeAirtightTankBlockEntity tank)) {
            throw new IllegalStateException("Creative airtight tank was not initialized at " + pos + '.');
        }

        tank.getTankInventory().setFixedPressurePa(BOUNDARY_PRESSURE_PA);
        tank.getTankInventory().setContainedGas(new GasStack(CCBGases.NATURAL_AIR.get(), 1));
        helper.assertValueEqual(tank.getTankInventory().getPressurePa(), BOUNDARY_PRESSURE_PA, "creative boundary pressure at " + pos);
    }
}
