package net.ty.createcraftedbeginning.gametests.gas;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.content.airtights.airtightregulatorpump.AirtightRegulatorPumpBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankBlockEntity;
import net.ty.createcraftedbeginning.gas.atmosphere.AtmosphereStateResolver;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.multiblock.GasTankMultiblockConnectivity;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection.FlowDirection;
import net.ty.createcraftedbeginning.gas.network.GasPipeConnection.FlowState;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology.Snapshot;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint.PressureState;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint.Recovery;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint.TransferAccess;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint.TransferLimits;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution.TransferComponent;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolver;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolver.PreparedGraph;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasEndpointTransferPlanner;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasQuantizedEquilibriumPlanner;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferExecutor;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferExecutor.PooledTransferExecutionResult;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferPlan;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransportFlowBudget;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasAirtightRegulatorPumpGameTests {
    private static final BlockPos SOURCE_POS = new BlockPos(0, 1, 1);
    private static final BlockPos REGULATOR_POS = new BlockPos(1, 1, 1);
    private static final BlockPos SINK_POS = new BlockPos(2, 1, 1);
    private static final BlockPos POWER_POS = new BlockPos(1, 1, 2);
    private static final long SOURCE_PRESSURE_PA = GasPressure.REFERENCE_PRESSURE_PA;
    private static final long OUTLET_SET_PRESSURE_PA = GasPressure.pascals(6);
    private static final long DEFAULT_OUTLET_SET_PRESSURE_PA = GasPressure.pascals(4);
    private static final long FLOW_LIMIT_SOURCE_PRESSURE_PA = GasPressure.pascals(2);
    private static final long FLOW_LIMIT_SINK_PRESSURE_PA = GasPressure.pascals(3);
    private static final long PRESSURE_TOLERANCE_PA = 1000;
    private static final int DYNAMIC_INITIAL_FLOW_DEADLINE_TICKS = 40;
    private static final int DYNAMIC_TRANSITION_DEADLINE_TICKS = 12;
    private static final int TARGET_CHANGE_FLOW_DEADLINE_TICKS = 80;
    private static final int POST_LOWER_TARGET_STABILITY_TICKS = 24;
    private static final long PENDING_REMOVAL_INITIAL_SOURCE_AMOUNT = 8000;
    private static final long PENDING_REMOVAL_TRANSFER_AMOUNT = 2500;
    private static final int PENDING_REMOVAL_RECOVERY_DEADLINE_TICKS = 5;
    private static final int PENDING_REMOVAL_STABILITY_TICKS = 25;
    private static final int ATMOSPHERIC_OUTLET_FLOW_ESTABLISH_DEADLINE_TICKS = 60;
    private static final int ATMOSPHERIC_OUTLET_REQUIRED_CONTINUOUS_FLOW_TICKS = 100;
    private static final long PRESSURE_SCALED_FLOW_LIMIT_TRANSITION_SOURCE_PRESSURE_PA = 108400;
    private static final long FINITE_SOURCE_ACTIVE_SET_INITIAL_PRESSURE_PA = GasPressure.pascals(6);
    private static final long FINITE_SOURCE_ACTIVE_SET_SUCCESS_PRESSURE_PA = GasPressure.pascals(5);
    private static final int FINITE_SOURCE_ACTIVE_SET_FLOW_ESTABLISH_DEADLINE_TICKS = 40;

    private GasAirtightRegulatorPumpGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 320)
    public static void regulatorPumpFillsFiniteTankToConfiguredOutletPressure(GameTestHelper helper) {
        CreativeAirtightTankBlockEntity source = placeCreativeBoundary(helper, SOURCE_POS, SOURCE_PRESSURE_PA);
        AirtightTankBlockEntity sink = placeFiniteTank(helper, SINK_POS);
        AirtightRegulatorPumpBlockEntity regulator = placePoweredRegulator(helper, REGULATOR_POS, POWER_POS, OUTLET_SET_PRESSURE_PA);

        helper.assertValueEqual(source.getTankInventory().getPressurePa(), SOURCE_PRESSURE_PA, "regulator pump source pressure");
        helper.assertTrue(sink.getTankInventory().isEmpty(), "Regulator pump finite sink did not start empty");

        helper.succeedWhen(() -> {
            assertRegulatorPowered(helper, regulator);
            helper.assertValueEqual(regulator.getOutletSetPressurePa(), OUTLET_SET_PRESSURE_PA, "regulator pump outlet set pressure");

            long sinkPressurePa = sink.getTankInventory().getPressurePa();
            helper.assertTrue(sinkPressurePa <= OUTLET_SET_PRESSURE_PA, "Regulator pump overshot its configured outlet pressure: sink=" + sinkPressurePa + ", target=" + OUTLET_SET_PRESSURE_PA + " Pa");
            helper.assertTrue(OUTLET_SET_PRESSURE_PA - sinkPressurePa <= PRESSURE_TOLERANCE_PA, "Regulator pump finite sink had not converged to the configured outlet pressure: sink=" + sinkPressurePa + ", target=" + OUTLET_SET_PRESSURE_PA + " Pa");
            helper.assertTrue(sink.getTankInventory().getGasStack().is(CCBGases.NATURAL_AIR.get()), "Regulator pump changed the transported gas species");
            helper.assertValueEqual(source.getTankInventory().getPressurePa(), SOURCE_PRESSURE_PA, "regulator pump fixed source pressure");

            GasTransportBehaviour transport = transport(helper, REGULATOR_POS);
            BlockState regulatorState = helper.getLevel().getBlockState(helper.absolutePos(REGULATOR_POS));
            helper.assertTrue(transport.allowsInboundFlow(regulatorState, Direction.WEST), "East-facing regulator pump did not accept gas on its west inlet");
            helper.assertTrue(!transport.allowsOutboundFlow(regulatorState, Direction.WEST), "East-facing regulator pump allowed outbound gas on its west inlet");
            helper.assertTrue(!transport.allowsInboundFlow(regulatorState, Direction.EAST), "East-facing regulator pump allowed inbound gas on its east outlet");
            helper.assertTrue(transport.allowsOutboundFlow(regulatorState, Direction.EAST), "East-facing regulator pump did not allow gas on its east outlet");
        });
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 520)
    public static void regulatorPumpStopsAtConfiguredMaximumPressureRise(GameTestHelper helper) {
        CreativeAirtightTankBlockEntity source = placeCreativeBoundary(helper, SOURCE_POS, SOURCE_PRESSURE_PA);
        AirtightTankBlockEntity sink = placeFiniteTank(helper, SINK_POS);
        AirtightRegulatorPumpBlockEntity regulator = placePoweredRegulator(helper, REGULATOR_POS, POWER_POS, GasPressure.pascals(16));
        long pressureRiseLimitedTargetPa = Math.min(regulator.getOutletSetPressurePa(), SOURCE_PRESSURE_PA + regulator.getMaxPressureRisePa());

        helper.assertTrue(pressureRiseLimitedTargetPa < regulator.getOutletSetPressurePa(), "Regulator pressure-rise test requires an outlet target above the single-stage pressure-rise limit");
        helper.succeedWhen(() -> {
            assertRegulatorPowered(helper, regulator);
            helper.assertValueEqual(source.getTankInventory().getPressurePa(), SOURCE_PRESSURE_PA, "pressure-rise-limited regulator source pressure");

            long sinkPressurePa = sink.getTankInventory().getPressurePa();
            helper.assertTrue(sinkPressurePa <= pressureRiseLimitedTargetPa, "Regulator pump exceeded its configured maximum pressure rise: sink=" + sinkPressurePa + ", limit=" + pressureRiseLimitedTargetPa + " Pa");
            helper.assertTrue(pressureRiseLimitedTargetPa - sinkPressurePa <= PRESSURE_TOLERANCE_PA, "Regulator pump did not converge to its pressure-rise-limited outlet: sink=" + sinkPressurePa + ", limit=" + pressureRiseLimitedTargetPa + " Pa");
        });
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 120)
    public static void regulatorPumpDoesNotBackflowOrBleedOutletAboveTarget(GameTestHelper helper) {
        CreativeAirtightTankBlockEntity source = placeCreativeBoundary(helper, SOURCE_POS, SOURCE_PRESSURE_PA);
        CreativeAirtightTankBlockEntity sink = placeCreativeBoundary(helper, SINK_POS, GasPressure.pascals(6));
        AirtightRegulatorPumpBlockEntity regulator = placePoweredRegulator(helper, REGULATOR_POS, POWER_POS, DEFAULT_OUTLET_SET_PRESSURE_PA);

        helper.succeedWhen(() -> {
            assertRegulatorPowered(helper, regulator);
            GasTransportBehaviour transport = transport(helper, REGULATOR_POS);
            helper.assertValueEqual(transport.getThroughputFlowRate(), 0L, "regulator pump throughput above target");
            assertNoPositiveFlow(helper, transport.getFlowState(Direction.WEST), "regulator pump inlet above target");
            assertNoPositiveFlow(helper, transport.getFlowState(Direction.EAST), "regulator pump outlet above target");
            helper.assertValueEqual(source.getTankInventory().getPressurePa(), SOURCE_PRESSURE_PA, "regulator pump no-backflow source pressure");
            helper.assertValueEqual(sink.getTankInventory().getPressurePa(), GasPressure.pascals(6), "regulator pump no-bleed outlet pressure");
        });
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 120)
    public static void regulatorPumpRespectsRpmScaledFlowLimit(GameTestHelper helper) {
        CreativeAirtightTankBlockEntity source = placeCreativeBoundary(helper, SOURCE_POS, FLOW_LIMIT_SOURCE_PRESSURE_PA);
        CreativeAirtightTankBlockEntity sink = placeCreativeBoundary(helper, SINK_POS, FLOW_LIMIT_SINK_PRESSURE_PA);
        AirtightRegulatorPumpBlockEntity regulator = placePoweredRegulator(helper, REGULATOR_POS, POWER_POS, OUTLET_SET_PRESSURE_PA);

        helper.assertTrue(source.getTankInventory().getPressurePa() < sink.getTankInventory().getPressurePa(), "Regulator pump flow-limit test did not require active compression against a higher-pressure outlet");
        helper.assertTrue(sink.getTankInventory().getPressurePa() < regulator.getOutletSetPressurePa(), "Regulator pump flow-limit test outlet was not below the configured outlet set pressure");

        helper.succeedWhen(() -> {
            assertRegulatorPowered(helper, regulator);
            long flowRateLimit = regulator.getFlowRateLimit();
            helper.assertTrue(flowRateLimit > 0, "Powered regulator pump reported a zero flow-rate limit");

            GasTransportBehaviour transport = transport(helper, REGULATOR_POS);
            FlowState inletFlow = transport.getFlowState(Direction.WEST);
            FlowState outletFlow = transport.getFlowState(Direction.EAST);
            helper.assertTrue(inletFlow != null, "Regulator pump did not create inlet flow while compressing against the higher-pressure outlet");
            if (inletFlow == null) {
                throw new NullPointerException("Regulator pump did not create inlet flow while compressing against the higher-pressure outlet.");
            }

            helper.assertTrue(outletFlow != null, "Regulator pump did not create outlet flow while compressing against the higher-pressure outlet");
            if (outletFlow == null) {
                throw new NullPointerException("Regulator pump did not create outlet flow while compressing against the higher-pressure outlet.");
            }

            helper.assertTrue(inletFlow.direction() == FlowDirection.INBOUND, "Regulator pump west face was not inbound");
            helper.assertTrue(outletFlow.direction() == FlowDirection.OUTBOUND, "Regulator pump east face was not outbound");
            helper.assertTrue(Math.abs(inletFlow.flowRate() - flowRateLimit) <= 1, "Regulator pump did not saturate at its RPM-scaled flow limit: flow=" + inletFlow.flowRate() + ", limit=" + flowRateLimit + " GU/t");
            helper.assertValueEqual(outletFlow.flowRate(), inletFlow.flowRate(), "regulator pump capped face flow rate");
            helper.assertValueEqual(transport.getThroughputFlowRate(), inletFlow.flowRate(), "regulator pump capped throughput");
        });
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 700)
    public static void regulatorPumpContinuesThroughResistivePipeUntilFiniteTankReachesTarget(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(0, 1, 1);
        BlockPos regulatorPos = new BlockPos(1, 1, 1);
        BlockPos pipePos = new BlockPos(2, 1, 1);
        BlockPos sinkPos = new BlockPos(3, 1, 1);
        BlockPos powerPos = new BlockPos(1, 1, 2);
        long outletSetPressurePa = GasPressure.pascals(8);
        long initialSinkPressurePa = GasPressure.pascals(7);
        long meaningfulPressureDeficitPa = GasPressure.pascals(0.1);

        placeCreativeBoundary(helper, sourcePos, SOURCE_PRESSURE_PA);
        AirtightTankBlockEntity sink = placeFiniteTank(helper, sinkPos);
        long initialSinkAmount = GasPressure.amount(sink.getTankInventory().getVolume(), initialSinkPressurePa);
        sink.getTankInventory().tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), initialSinkAmount)).requireAccepted();
        AirtightRegulatorPumpBlockEntity regulator = placePoweredRegulator(helper, regulatorPos, powerPos, outletSetPressurePa);
        helper.setBlock(pipePos, CCBBlocks.AIRTIGHT_PIPE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X));

        helper.assertTrue(outletSetPressurePa < SOURCE_PRESSURE_PA + regulator.getMaxPressureRisePa(), "Regulator pump resistive-pipe continuation test target must remain below the single-stage pressure-rise ceiling");
        helper.assertValueEqual(sink.getTankInventory().getPressurePa(), initialSinkPressurePa, "regulator pump resistive-pipe initial sink pressure");
        helper.assertTrue(sink.getTankInventory().getMaxPressurePa() >= outletSetPressurePa, "Regulator pump resistive-pipe sink pressure rating was below the configured target");

        boolean[] sawPositiveFlow = new boolean[1];
        int[] stalledTicks = new int[1];
        int[] testTicks = new int[1];

        helper.onEachTick(() -> {
            testTicks[0]++;
            assertRegulatorPowered(helper, regulator);

            long sinkPressurePa = sink.getTankInventory().getPressurePa();
            helper.assertTrue(sinkPressurePa <= outletSetPressurePa + PRESSURE_TOLERANCE_PA, "Regulator pump overshot its target through a resistive airtight pipe: sink=" + sinkPressurePa + ", target=" + outletSetPressurePa + " Pa");
            if (outletSetPressurePa - sinkPressurePa <= PRESSURE_TOLERANCE_PA) {
                helper.succeed();
                return;
            }

            GasTransportBehaviour regulatorTransport = transport(helper, regulatorPos);
            GasTransportBehaviour pipeTransport = transport(helper, pipePos);
            long regulatorFlowRate = regulatorTransport.getThroughputFlowRate();
            long pipeFlowRate = pipeTransport.getThroughputFlowRate();
            if (regulatorFlowRate > 0 || pipeFlowRate > 0) {
                sawPositiveFlow[0] = true;
                stalledTicks[0] = 0;
                return;
            }

            if (!sawPositiveFlow[0]) {
                if (testTicks[0] > 40) {
                    helper.fail("Regulator pump never established positive flow through the resistive airtight pipe");
                }
                return;
            }

            long pressureDeficitPa = outletSetPressurePa - sinkPressurePa;
            if (pressureDeficitPa >= meaningfulPressureDeficitPa) {
                stalledTicks[0]++;
                if (stalledTicks[0] >= 8) {
                    helper.fail("Regulator pump stalled below target through the resistive airtight pipe: sink=" + sinkPressurePa + ", target=" + outletSetPressurePa + ", pipeFlow=" + pipeFlowRate + ", regulatorFlow=" + regulatorFlowRate + " GU/t");
                }
                return;
            }

            stalledTicks[0] = 0;
        });
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 1200)
    public static void regulatorPumpContinuesFromAtmosphereThroughSuctionPipeUntilFiniteTankReachesTarget(GameTestHelper helper) {
        BlockPos inletPipePos = new BlockPos(1, 1, 1);
        BlockPos regulatorPos = new BlockPos(2, 1, 1);
        BlockPos outletPipePos = new BlockPos(3, 1, 1);
        BlockPos sinkPos = new BlockPos(4, 1, 1);
        BlockPos powerPos = new BlockPos(2, 1, 2);
        long outletSetPressurePa = GasPressure.pascals(8);
        long initialSinkPressurePa = GasPressure.pascals(7);
        long meaningfulPressureDeficitPa = GasPressure.pascals(0.1);

        helper.setBlock(inletPipePos, CCBBlocks.AIRTIGHT_PIPE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X));
        AirtightTankBlockEntity sink = placeFiniteTank(helper, sinkPos);
        long initialSinkAmount = GasPressure.amount(sink.getTankInventory().getVolume(), initialSinkPressurePa);
        sink.getTankInventory().tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), initialSinkAmount)).requireAccepted();
        AirtightRegulatorPumpBlockEntity regulator = placePoweredRegulator(helper, regulatorPos, powerPos, outletSetPressurePa);
        helper.setBlock(outletPipePos, CCBBlocks.AIRTIGHT_PIPE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X));
        long atmosphericPressurePa = AtmosphereStateResolver.resolve(helper.getLevel(), helper.absolutePos(inletPipePos)).pressurePa();

        helper.assertTrue(outletSetPressurePa < atmosphericPressurePa + regulator.getMaxPressureRisePa(), "Regulator pump atmospheric-suction continuation test target must remain below the single-stage pressure-rise ceiling");
        helper.assertValueEqual(sink.getTankInventory().getPressurePa(), initialSinkPressurePa, "regulator pump atmospheric-suction initial sink pressure");
        helper.assertTrue(sink.getTankInventory().getMaxPressurePa() >= outletSetPressurePa, "Regulator pump atmospheric-suction sink pressure rating was below the configured target");

        boolean[] sawPositiveFlow = new boolean[1];
        int[] stalledTicks = new int[1];
        int[] stagnantPressureTicks = new int[1];
        int[] testTicks = new int[1];
        long[] lastSinkPressurePa = {initialSinkPressurePa};

        helper.onEachTick(() -> {
            testTicks[0]++;
            assertRegulatorPowered(helper, regulator);

            long sinkPressurePa = sink.getTankInventory().getPressurePa();
            helper.assertTrue(sinkPressurePa <= outletSetPressurePa + PRESSURE_TOLERANCE_PA, "Regulator pump overshot its target with an atmospheric suction pipe: sink=" + sinkPressurePa + ", target=" + outletSetPressurePa + " Pa");
            if (outletSetPressurePa - sinkPressurePa <= PRESSURE_TOLERANCE_PA) {
                helper.succeed();
                return;
            }

            GasTransportBehaviour inletPipeTransport = transport(helper, inletPipePos);
            GasTransportBehaviour regulatorTransport = transport(helper, regulatorPos);
            GasTransportBehaviour outletPipeTransport = transport(helper, outletPipePos);
            long inletPipeFlowRate = inletPipeTransport.getThroughputFlowRate();
            long regulatorFlowRate = regulatorTransport.getThroughputFlowRate();
            long outletPipeFlowRate = outletPipeTransport.getThroughputFlowRate();
            boolean hasPositiveFlow = inletPipeFlowRate > 0 || regulatorFlowRate > 0 || outletPipeFlowRate > 0;
            if (hasPositiveFlow) {
                sawPositiveFlow[0] = true;
                stalledTicks[0] = 0;
            }

            long pressureDeficitPa = outletSetPressurePa - sinkPressurePa;
            if (sinkPressurePa > lastSinkPressurePa[0]) {
                lastSinkPressurePa[0] = sinkPressurePa;
                stagnantPressureTicks[0] = 0;
            }
            else if (sawPositiveFlow[0] && pressureDeficitPa >= meaningfulPressureDeficitPa) {
                stagnantPressureTicks[0]++;
                if (stagnantPressureTicks[0] >= 32) {
                    helper.fail("Regulator pump stopped raising the finite tank pressure below target with an atmospheric suction pipe: sink=" + sinkPressurePa + ", target=" + outletSetPressurePa + ", inletFlow=" + inletPipeFlowRate + ", regulatorFlow=" + regulatorFlowRate + ", outletFlow=" + outletPipeFlowRate + " GU/t");
                }
            }

            if (!sawPositiveFlow[0]) {
                if (testTicks[0] > 40) {
                    helper.fail("Regulator pump never established positive flow from atmosphere through its suction pipe: sink=" + sinkPressurePa + ", target=" + outletSetPressurePa + ", inletFlow=" + inletPipeFlowRate + ", regulatorFlow=" + regulatorFlowRate + ", outletFlow=" + outletPipeFlowRate + " GU/t");
                }
                return;
            }

            if (testTicks[0] >= 1100) {
                helper.fail("Regulator pump remained below target after the extended atmospheric-suction convergence window: sink=" + sinkPressurePa + ", target=" + outletSetPressurePa + ", inletFlow=" + inletPipeFlowRate + ", regulatorFlow=" + regulatorFlowRate + ", outletFlow=" + outletPipeFlowRate + " GU/t");
                return;
            }

            if (hasPositiveFlow) {
                return;
            }

            if (pressureDeficitPa >= meaningfulPressureDeficitPa) {
                stalledTicks[0]++;
                if (stalledTicks[0] >= 8) {
                    helper.fail("Regulator pump stalled below target with an atmospheric suction pipe: sink=" + sinkPressurePa + ", target=" + outletSetPressurePa + ", inletFlow=" + inletPipeFlowRate + ", regulatorFlow=" + regulatorFlowRate + ", outletFlow=" + outletPipeFlowRate + " GU/t");
                }
                return;
            }

            stalledTicks[0] = 0;
        });
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 240)
    public static void regulatorPumpMaintainsContinuousFlowToAtmosphere(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos sourcePos = new BlockPos(0, 1, 1);
        BlockPos regulatorPos = new BlockPos(1, 1, 1);
        BlockPos outletPipePos = new BlockPos(2, 1, 1);
        BlockPos powerPos = new BlockPos(1, 1, 2);
        BlockPos atmosphericOutletPos = outletPipePos.relative(Direction.EAST);
        long outletSetPressurePa = DEFAULT_OUTLET_SET_PRESSURE_PA;

        CreativeAirtightTankBlockEntity source = placeCreativeBoundary(helper, sourcePos, SOURCE_PRESSURE_PA);
        AirtightRegulatorPumpBlockEntity regulator = placePoweredRegulator(helper, regulatorPos, powerPos, outletSetPressurePa);
        helper.setBlock(outletPipePos, CCBBlocks.AIRTIGHT_PIPE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X));

        long atmosphericPressurePa = AtmosphereStateResolver.resolve(level, helper.absolutePos(atmosphericOutletPos)).pressurePa();
        helper.assertValueEqual(source.getTankInventory().getPressurePa(), SOURCE_PRESSURE_PA, "regulator pump atmospheric-outlet source pressure");
        helper.assertTrue(outletSetPressurePa > atmosphericPressurePa, "Regulator pump atmospheric-outlet target was not above ambient pressure: target=" + outletSetPressurePa + ", ambient=" + atmosphericPressurePa + " Pa");

        boolean[] establishedFlow = new boolean[1];
        int[] continuousFlowTicks = new int[1];
        int[] testTicks = new int[1];

        helper.onEachTick(() -> {
            testTicks[0]++;
            assertRegulatorPowered(helper, regulator);
            helper.assertValueEqual(regulator.getOutletSetPressurePa(), outletSetPressurePa, "regulator pump atmospheric-outlet set pressure");
            helper.assertValueEqual(source.getTankInventory().getPressurePa(), SOURCE_PRESSURE_PA, "regulator pump atmospheric-outlet fixed source pressure");

            GasTransportBehaviour regulatorTransport = transport(helper, regulatorPos);
            GasTransportBehaviour outletPipeTransport = transport(helper, outletPipePos);
            boolean regulatorForwardFlow = hasPositiveForwardFlow(regulatorTransport);
            boolean pipeForwardFlow = hasPositiveForwardFlow(outletPipeTransport);
            boolean continuousForwardFlow = regulatorForwardFlow && pipeForwardFlow && regulatorTransport.getThroughputFlowRate() > 0 && outletPipeTransport.getThroughputFlowRate() > 0;

            if (!establishedFlow[0]) {
                if (continuousForwardFlow) {
                    establishedFlow[0] = true;
                    continuousFlowTicks[0] = 1;
                    return;
                }

                if (testTicks[0] > ATMOSPHERIC_OUTLET_FLOW_ESTABLISH_DEADLINE_TICKS) {
                    helper.fail("Regulator pump never established continuous forward flow to atmosphere: target=" + outletSetPressurePa + ", ambient=" + atmosphericPressurePa + ", regulator=" + describeTransport(regulatorTransport) + ", outletPipe=" + describeTransport(outletPipeTransport));
                }
                return;
            }

            if (!continuousForwardFlow) {
                long currentAtmosphericPressurePa = AtmosphereStateResolver.resolve(level, helper.absolutePos(atmosphericOutletPos)).pressurePa();
                helper.fail("Regulator pump flow to atmosphere interrupted after " + continuousFlowTicks[0] + " continuous ticks: target=" + outletSetPressurePa + ", ambient=" + currentAtmosphericPressurePa + ", regulator=" + describeTransport(regulatorTransport) + ", outletPipe=" + describeTransport(outletPipeTransport));
                return;
            }

            long regulatorFlowRate = assertForwardFlow(helper, regulatorTransport, "regulator pump atmospheric-outlet regulator");
            long outletPipeFlowRate = assertForwardFlow(helper, outletPipeTransport, "regulator pump atmospheric-outlet pipe");
            helper.assertTrue(regulatorFlowRate > 0 && outletPipeFlowRate > 0, "Regulator pump atmospheric-outlet server flow state was not positive after flow establishment");

            continuousFlowTicks[0]++;
            if (continuousFlowTicks[0] < ATMOSPHERIC_OUTLET_REQUIRED_CONTINUOUS_FLOW_TICKS) {
                return;
            }

            helper.succeed();
        });
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 120)
    public static void regulatorPumpDoesNotStallAtPressureScaledFlowLimitTransition(GameTestHelper helper) {
        BlockPos chamberPos = new BlockPos(0, 1, 1);
        BlockPos regulatorPos = chamberPos.above();
        BlockPos powerPos = regulatorPos.east();
        BlockPos atmosphericOutletPos = regulatorPos.above();
        long outletSetPressurePa = GasPressure.pascals(16);

        GasStorageHandler source = placeFilledInjectionChamber(helper, chamberPos, PRESSURE_SCALED_FLOW_LIMIT_TRANSITION_SOURCE_PRESSURE_PA);
        AirtightRegulatorPumpBlockEntity regulator = placePoweredRegulator(helper, regulatorPos, powerPos, outletSetPressurePa, Direction.UP);
        long atmosphericPressurePa = AtmosphereStateResolver.resolve(helper.getLevel(), helper.absolutePos(atmosphericOutletPos)).pressurePa();

        helper.assertValueEqual(source.getTankPressurePa(0), PRESSURE_SCALED_FLOW_LIMIT_TRANSITION_SOURCE_PRESSURE_PA, "regulator transition injection chamber source pressure");
        helper.assertTrue(atmosphericPressurePa < PRESSURE_SCALED_FLOW_LIMIT_TRANSITION_SOURCE_PRESSURE_PA, "Regulator transition source pressure was not above ambient pressure");
        helper.succeedWhen(() -> {
            assertRegulatorPowered(helper, regulator);

            GasTransportBehaviour transport = transport(helper, regulatorPos);
            long flowRate = assertForwardFlow(helper, transport, Direction.DOWN, Direction.UP, "regulator pressure-scaled flow-limit transition");
            helper.assertTrue(flowRate > 0, "Regulator pump stalled at the pressure-scaled flow-limit transition: " + describeTransport(transport));
            helper.assertTrue(source.getTankPressurePa(0) < PRESSURE_SCALED_FLOW_LIMIT_TRANSITION_SOURCE_PRESSURE_PA, "Regulator pump reported forward flow without draining the injection chamber source");
        });
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 160)
    public static void regulatorPumpDrainsFiniteSourcePastInventoryAmountActiveSetBoundary(GameTestHelper helper) {
        BlockPos chamberPos = new BlockPos(0, 1, 1);
        BlockPos regulatorPos = chamberPos.above();
        BlockPos powerPos = regulatorPos.east();
        long outletSetPressurePa = GasPressure.pascals(16);

        GasStorageHandler source = placeFilledInjectionChamber(helper, chamberPos, FINITE_SOURCE_ACTIVE_SET_INITIAL_PRESSURE_PA);
        AirtightRegulatorPumpBlockEntity regulator = placePoweredRegulator(helper, regulatorPos, powerPos, outletSetPressurePa, Direction.UP);
        boolean[] establishedFlow = new boolean[1];
        int[] ticks = new int[1];
        long[] previousSourcePressurePa = {FINITE_SOURCE_ACTIVE_SET_INITIAL_PRESSURE_PA};

        helper.onEachTick(() -> {
            ticks[0]++;
            assertRegulatorPowered(helper, regulator);
            long sourcePressurePa = source.getTankPressurePa(0);
            if (sourcePressurePa <= FINITE_SOURCE_ACTIVE_SET_SUCCESS_PRESSURE_PA) {
                helper.succeed();
                return;
            }

            GasTransportBehaviour transport = transport(helper, regulatorPos);
            FlowState inletFlow = transport.getFlowState(Direction.DOWN);
            FlowState outletFlow = transport.getFlowState(Direction.UP);
            boolean forwardFlow = isPositiveFlow(inletFlow, FlowDirection.INBOUND) && isPositiveFlow(outletFlow, FlowDirection.OUTBOUND) && transport.getThroughputFlowRate() > 0;
            if (!establishedFlow[0]) {
                if (forwardFlow) {
                    establishedFlow[0] = true;
                    previousSourcePressurePa[0] = sourcePressurePa;
                    return;
                }

                if (ticks[0] > FINITE_SOURCE_ACTIVE_SET_FLOW_ESTABLISH_DEADLINE_TICKS) {
                    helper.fail("Regulator pump never established finite-source atmospheric flow: source=" + sourcePressurePa + " Pa, amount=" + source.getGasInTank(0).getAmount() + " GU, transport=" + describeTransport(transport));
                }
                return;
            }

            if (!forwardFlow) {
                helper.fail("Regulator pump stalled while draining a finite source across the inventory active-set boundary: source=" + sourcePressurePa + " Pa, amount=" + source.getGasInTank(0).getAmount() + " GU, previousSource=" + previousSourcePressurePa[0] + " Pa, flowLimit=" + regulator.getFlowRateLimit() + " GU/t, transport=" + describeTransport(transport));
                return;
            }

            helper.assertTrue(sourcePressurePa < previousSourcePressurePa[0], "Regulator pump reported forward flow without reducing finite-source pressure: source=" + sourcePressurePa + " Pa, previous=" + previousSourcePressurePa[0] + " Pa, transport=" + describeTransport(transport));
            previousSourcePressurePa[0] = sourcePressurePa;
        });
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 180)
    public static void adjacentEqualTargetRegulatorsMaintainContinuousFlow(GameTestHelper helper) {
        BlockPos firstRegulatorPos = new BlockPos(1, 1, 1);
        BlockPos secondRegulatorPos = new BlockPos(2, 1, 1);
        placeCreativeBoundary(helper, SOURCE_POS, SOURCE_PRESSURE_PA);
        placeCreativeBoundary(helper, new BlockPos(3, 1, 1), SOURCE_PRESSURE_PA);
        AirtightRegulatorPumpBlockEntity firstRegulator = placePoweredRegulator(helper, firstRegulatorPos, new BlockPos(1, 1, 2), DEFAULT_OUTLET_SET_PRESSURE_PA);
        AirtightRegulatorPumpBlockEntity secondRegulator = placePoweredRegulator(helper, secondRegulatorPos, new BlockPos(2, 1, 2), DEFAULT_OUTLET_SET_PRESSURE_PA);
        GasTransportBehaviour firstTransport = transport(helper, firstRegulatorPos);
        GasTransportBehaviour secondTransport = transport(helper, secondRegulatorPos);
        int[] ticks = new int[1];
        int[] consecutiveFlowTicks = new int[1];

        helper.onEachTick(() -> {
            ticks[0]++;
            if (!isRegulatorPowered(firstRegulator) || !isRegulatorPowered(secondRegulator) || !hasPositiveForwardFlow(firstTransport) || !hasPositiveForwardFlow(secondTransport)) {
                consecutiveFlowTicks[0] = 0;
                if (ticks[0] > DYNAMIC_INITIAL_FLOW_DEADLINE_TICKS) {
                    helper.fail("Directly adjacent, east-facing regulator pumps failed to maintain flow: first=" + describeTransport(firstTransport) + ", second=" + describeTransport(secondTransport));
                }
                return;
            }

            long firstFlow = assertForwardFlow(helper, firstTransport, "first adjacent regulator");
            long secondFlow = assertForwardFlow(helper, secondTransport, "second adjacent regulator");
            helper.assertValueEqual(secondFlow, firstFlow, "directly adjacent regulator pump series flow");
            helper.assertTrue(firstFlow <= firstRegulator.getFlowRateLimit(), "First adjacent regulator exceeded its flow limit");
            helper.assertTrue(secondFlow <= secondRegulator.getFlowRateLimit(), "Second adjacent regulator exceeded its flow limit");
            if (++consecutiveFlowTicks[0] >= 40) {
                helper.succeed();
            }
        });
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 520)
    public static void adjacentEqualTargetRegulatorsFillFiniteTank(GameTestHelper helper) {
        BlockPos firstRegulatorPos = new BlockPos(1, 1, 1);
        BlockPos secondRegulatorPos = new BlockPos(2, 1, 1);
        placeCreativeBoundary(helper, SOURCE_POS, SOURCE_PRESSURE_PA);
        AirtightTankBlockEntity sink = placeFiniteTank(helper, new BlockPos(3, 1, 1));
        AirtightRegulatorPumpBlockEntity firstRegulator = placePoweredRegulator(helper, firstRegulatorPos, new BlockPos(1, 1, 2), DEFAULT_OUTLET_SET_PRESSURE_PA);
        AirtightRegulatorPumpBlockEntity secondRegulator = placePoweredRegulator(helper, secondRegulatorPos, new BlockPos(2, 1, 2), DEFAULT_OUTLET_SET_PRESSURE_PA);
        GasTransportBehaviour firstTransport = transport(helper, firstRegulatorPos);
        GasTransportBehaviour secondTransport = transport(helper, secondRegulatorPos);
        int[] ticks = new int[1];
        boolean[] sawFlow = new boolean[1];

        helper.onEachTick(() -> {
            ticks[0]++;
            if (hasPositiveForwardFlow(firstTransport) && hasPositiveForwardFlow(secondTransport)) {
                sawFlow[0] = true;
                assertForwardFlow(helper, firstTransport, "first adjacent regulator filling tank");
                assertForwardFlow(helper, secondTransport, "second adjacent regulator filling tank");
            }
            if (ticks[0] > DYNAMIC_INITIAL_FLOW_DEADLINE_TICKS && !sawFlow[0]) {
                helper.fail("Adjacent regulator pumps did not establish flow into the finite tank");
                return;
            }

            long sinkPressurePa = sink.getTankInventory().getPressurePa();
            helper.assertTrue(sinkPressurePa <= DEFAULT_OUTLET_SET_PRESSURE_PA, "Adjacent regulator pumps stacked their equal outlet pressure targets: sink=" + sinkPressurePa);
            if (sawFlow[0] && DEFAULT_OUTLET_SET_PRESSURE_PA - sinkPressurePa <= PRESSURE_TOLERANCE_PA) {
                assertRegulatorPowered(helper, firstRegulator);
                assertRegulatorPowered(helper, secondRegulator);
                helper.succeed();
            }
        });
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 420)
    public static void adjacentSteppedTargetRegulatorsCrossSupplyLimitTransition(GameTestHelper helper) {
        BlockPos firstRegulatorPos = new BlockPos(1, 1, 1);
        BlockPos secondRegulatorPos = new BlockPos(2, 1, 1);
        BlockPos tankControllerPos = new BlockPos(3, 1, 1);
        long firstTargetPa = GasPressure.pascals(4);
        long secondTargetPa = GasPressure.pascals(16);
        long initialTankPressurePa = GasPressure.pascals(9.8);
        long transitionCrossedPressurePa = GasPressure.pascals(10.5);

        placeCreativeBoundary(helper, SOURCE_POS, SOURCE_PRESSURE_PA);
        AirtightTankBlockEntity sink = placeTwoByTwoByTwoFiniteTank(helper, tankControllerPos);
        AirtightRegulatorPumpBlockEntity firstRegulator = placePoweredRegulator(helper, firstRegulatorPos, new BlockPos(1, 1, 2), firstTargetPa);
        AirtightRegulatorPumpBlockEntity secondRegulator = placePoweredRegulator(helper, secondRegulatorPos, new BlockPos(2, 1, 2), secondTargetPa);
        long initialAmount = GasPressure.amount(sink.getTankInventory().getVolume(), initialTankPressurePa);
        sink.getTankInventory().tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), initialAmount)).requireAccepted();

        GasTransportBehaviour firstTransport = transport(helper, firstRegulatorPos);
        GasTransportBehaviour secondTransport = transport(helper, secondRegulatorPos);
        boolean[] sawFlow = new boolean[1];
        int[] ticks = new int[1];
        int[] stagnantTicks = new int[1];
        long[] lastTankPressurePa = {initialTankPressurePa};

        helper.onEachTick(() -> {
            ticks[0]++;
            if (!isRegulatorPowered(firstRegulator) || !isRegulatorPowered(secondRegulator)) {
                if (ticks[0] > DYNAMIC_INITIAL_FLOW_DEADLINE_TICKS) {
                    helper.fail("Stepped-target adjacent regulators did not reach powered state");
                }
                return;
            }

            long tankPressurePa = sink.getTankInventory().getPressurePa();
            if (tankPressurePa >= transitionCrossedPressurePa) {
                helper.succeed();
                return;
            }

            boolean forwardFlow = hasPositiveForwardFlow(firstTransport) && hasPositiveForwardFlow(secondTransport);
            if (forwardFlow) {
                sawFlow[0] = true;
                long firstFlow = assertForwardFlow(helper, firstTransport, "first stepped-target adjacent regulator");
                long secondFlow = assertForwardFlow(helper, secondTransport, "second stepped-target adjacent regulator");
                helper.assertValueEqual(secondFlow, firstFlow, "stepped-target adjacent regulator series flow");
            }

            if (tankPressurePa > lastTankPressurePa[0]) {
                lastTankPressurePa[0] = tankPressurePa;
                stagnantTicks[0] = 0;
            }
            else if (sawFlow[0]) {
                stagnantTicks[0]++;
            }

            if (!sawFlow[0] && ticks[0] > DYNAMIC_INITIAL_FLOW_DEADLINE_TICKS) {
                helper.fail("Stepped-target adjacent regulators never established flow into the 2x2x2 tank");
                return;
            }

            if (stagnantTicks[0] >= 24) {
                helper.fail("Stepped-target adjacent regulators stalled at the serial supply-limit transition: tank=" + tankPressurePa + " Pa, first=" + describeTransport(firstTransport) + ", second=" + describeTransport(secondTransport));
            }
        });
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 420)
    public static void equalTargetRegulatorPumpsInSeriesDoNotStackPressure(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(0, 1, 1);
        BlockPos firstRegulatorPos = new BlockPos(1, 1, 1);
        BlockPos firstPipePos = new BlockPos(2, 1, 1);
        BlockPos secondPipePos = new BlockPos(3, 1, 1);
        BlockPos secondRegulatorPos = new BlockPos(4, 1, 1);
        BlockPos sinkPos = new BlockPos(5, 1, 1);
        BlockPos firstPowerPos = new BlockPos(1, 1, 2);
        BlockPos secondPowerPos = new BlockPos(4, 1, 2);

        placeCreativeBoundary(helper, sourcePos, SOURCE_PRESSURE_PA);
        AirtightTankBlockEntity sink = placeFiniteTank(helper, sinkPos);
        AirtightRegulatorPumpBlockEntity firstRegulator = placePoweredRegulator(helper, firstRegulatorPos, firstPowerPos, DEFAULT_OUTLET_SET_PRESSURE_PA);
        AirtightRegulatorPumpBlockEntity secondRegulator = placePoweredRegulator(helper, secondRegulatorPos, secondPowerPos, DEFAULT_OUTLET_SET_PRESSURE_PA);
        helper.setBlock(firstPipePos, CCBBlocks.AIRTIGHT_PIPE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X));
        helper.setBlock(secondPipePos, CCBBlocks.AIRTIGHT_PIPE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X));

        helper.succeedWhen(() -> {
            assertRegulatorPowered(helper, firstRegulator);
            assertRegulatorPowered(helper, secondRegulator);
            long sinkPressurePa = sink.getTankInventory().getPressurePa();
            helper.assertTrue(sinkPressurePa <= DEFAULT_OUTLET_SET_PRESSURE_PA, "Series regulator pumps stacked their equal absolute targets: sink=" + sinkPressurePa + ", target=" + DEFAULT_OUTLET_SET_PRESSURE_PA + " Pa");
            helper.assertTrue(DEFAULT_OUTLET_SET_PRESSURE_PA - sinkPressurePa <= PRESSURE_TOLERANCE_PA, "Series regulator pumps had not converged to their shared target: sink=" + sinkPressurePa + ", target=" + DEFAULT_OUTLET_SET_PRESSURE_PA + " Pa");
        });
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 180)
    public static void losingAndRestoringRegulatorPowerStopsAndRestoresActiveTransport(GameTestHelper helper) {
        placeCreativeBoundary(helper, SOURCE_POS, SOURCE_PRESSURE_PA);
        placeCreativeBoundary(helper, SINK_POS, SOURCE_PRESSURE_PA);
        AirtightRegulatorPumpBlockEntity regulator = placePoweredRegulator(helper, REGULATOR_POS, POWER_POS, DEFAULT_OUTLET_SET_PRESSURE_PA);
        CreativeMotorBlockEntity motor = motor(helper);
        GasTransportBehaviour transport = transport(helper, REGULATOR_POS);

        long[] initialFlowRate = new long[1];
        long[] initialFlowRateLimit = new long[1];
        int[] phase = new int[1];
        int[] phaseTicks = new int[1];

        helper.onEachTick(() -> {
            phaseTicks[0]++;
            if (phase[0] == 0) {
                if (!isRegulatorPowered(regulator) || !hasPositiveForwardFlow(transport)) {
                    if (phaseTicks[0] > DYNAMIC_INITIAL_FLOW_DEADLINE_TICKS) {
                        helper.fail("Airtight regulator pump never established its initial powered flow between equal-pressure boundaries");
                    }
                    return;
                }

                initialFlowRateLimit[0] = regulator.getFlowRateLimit();
                initialFlowRate[0] = assertForwardFlow(helper, transport, "powered airtight regulator pump before power loss");
                helper.assertTrue(initialFlowRateLimit[0] > 0, "Powered airtight regulator pump reported a zero initial flow-rate limit");

                motor.generatedSpeed.setValue(0);
                helper.assertValueEqual(motor.generatedSpeed.getValue(), 0, "creative motor commanded speed after regulator power loss");
                phase[0] = 1;
                phaseTicks[0] = 0;
                return;
            }

            if (phase[0] == 1) {
                boolean regulatorStopped = Mth.abs(regulator.getSpeed()) < SpeedLevel.MEDIUM.getSpeedValue() && regulator.getFlowRateLimit() == 0;
                boolean flowCleared = transport.getFlowState(Direction.WEST) == null && transport.getFlowState(Direction.EAST) == null && transport.getThroughputFlowRate() == 0;
                if (!regulatorStopped || !flowCleared) {
                    if (phaseTicks[0] > DYNAMIC_TRANSITION_DEADLINE_TICKS) {
                        helper.fail("Airtight regulator pump did not promptly stop active transport and clear stale server-side flow state after kinetic power was removed");
                    }
                    return;
                }

                helper.assertValueEqual(regulator.getFlowRateLimit(), 0L, "unpowered airtight regulator pump flow-rate limit");
                assertNoPositiveFlow(helper, transport.getFlowState(Direction.WEST), "unpowered regulator pump west-face flow");
                assertNoPositiveFlow(helper, transport.getFlowState(Direction.EAST), "unpowered regulator pump east-face flow");

                motor.generatedSpeed.setValue(CreativeMotorBlockEntity.MAX_SPEED);
                helper.assertValueEqual(motor.generatedSpeed.getValue(), CreativeMotorBlockEntity.MAX_SPEED, "creative motor commanded speed after regulator power restoration");
                phase[0] = 2;
                phaseTicks[0] = 0;
                return;
            }

            if (!isRegulatorPowered(regulator) || !hasPositiveForwardFlow(transport)) {
                if (phaseTicks[0] > DYNAMIC_TRANSITION_DEADLINE_TICKS) {
                    helper.fail("Airtight regulator pump did not promptly restore active transport after kinetic power returned");
                }
                return;
            }

            helper.assertValueEqual(regulator.getFlowRateLimit(), initialFlowRateLimit[0], "restored regulator flow-rate limit versus initial powered limit");
            long restoredFlowRate = assertForwardFlow(helper, transport, "airtight regulator pump after power restoration");
            helper.assertValueEqual(restoredFlowRate, initialFlowRate[0], "restored regulator flow rate versus initial powered flow rate");
            helper.succeed();
        });
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 760)
    public static void changingOutletTargetAtRuntimeReconfiguresRegulationWithoutBackflow(GameTestHelper helper) {
        long initialTargetPa = GasPressure.pascals(4);
        long raisedTargetPa = GasPressure.pascals(6);
        long loweredTargetPa = GasPressure.pascals(3);

        CreativeAirtightTankBlockEntity source = placeCreativeBoundary(helper, SOURCE_POS, SOURCE_PRESSURE_PA);
        AirtightTankBlockEntity sink = placeFiniteTank(helper, SINK_POS);
        AirtightRegulatorPumpBlockEntity regulator = placePoweredRegulator(helper, REGULATOR_POS, POWER_POS, initialTargetPa);
        GasTransportBehaviour transport = transport(helper, REGULATOR_POS);

        int[] phase = new int[1];
        int[] phaseTicks = new int[1];
        long[] pressureAtInitialTarget = new long[1];
        long[] pressureAtRaisedTarget = new long[1];
        long[] pressureAfterLowering = new long[1];
        boolean[] sawPressureRiseAfterRetarget = new boolean[1];

        helper.onEachTick(() -> {
            phaseTicks[0]++;
            assertRegulatorPowered(helper, regulator);
            helper.assertValueEqual(source.getTankInventory().getPressurePa(), SOURCE_PRESSURE_PA, "runtime-retarget regulator source pressure");

            long sinkPressurePa = sink.getTankInventory().getPressurePa();
            if (phase[0] == 0) {
                helper.assertValueEqual(regulator.getOutletSetPressurePa(), initialTargetPa, "initial runtime regulator target");
                helper.assertTrue(sinkPressurePa <= initialTargetPa + PRESSURE_TOLERANCE_PA, "Regulator pump overshot its initial runtime target: sink=" + sinkPressurePa + ", target=" + initialTargetPa + " Pa");
                if (initialTargetPa - sinkPressurePa > PRESSURE_TOLERANCE_PA) {
                    return;
                }

                pressureAtInitialTarget[0] = sinkPressurePa;
                regulator.setOutletSetPressurePa(raisedTargetPa);
                helper.assertValueEqual(regulator.getOutletSetPressurePa(), raisedTargetPa, "raised runtime regulator target immediately after update");
                phase[0] = 1;
                phaseTicks[0] = 0;
                return;
            }

            if (phase[0] == 1) {
                helper.assertValueEqual(regulator.getOutletSetPressurePa(), raisedTargetPa, "raised runtime regulator target");
                helper.assertTrue(sinkPressurePa <= raisedTargetPa + PRESSURE_TOLERANCE_PA, "Regulator pump overshot its raised runtime target: sink=" + sinkPressurePa + ", target=" + raisedTargetPa + " Pa");
                if (sinkPressurePa > pressureAtInitialTarget[0] + GasPressure.pascals(0.05)) {
                    sawPressureRiseAfterRetarget[0] = true;
                }
                if (!sawPressureRiseAfterRetarget[0] && phaseTicks[0] > TARGET_CHANGE_FLOW_DEADLINE_TICKS) {
                    helper.fail("Airtight regulator pump did not resume raising outlet pressure after its target was increased at runtime");
                    return;
                }

                if (raisedTargetPa - sinkPressurePa > PRESSURE_TOLERANCE_PA) {
                    return;
                }

                helper.assertTrue(sawPressureRiseAfterRetarget[0], "Regulator pump reached the raised target without observing any pressure increase after runtime retargeting");
                pressureAtRaisedTarget[0] = sinkPressurePa;
                regulator.setOutletSetPressurePa(loweredTargetPa);
                helper.assertValueEqual(regulator.getOutletSetPressurePa(), loweredTargetPa, "lowered runtime regulator target immediately after update");
                phase[0] = 2;
                phaseTicks[0] = 0;
                return;
            }

            helper.assertValueEqual(regulator.getOutletSetPressurePa(), loweredTargetPa, "lowered runtime regulator target");
            helper.assertTrue(sinkPressurePa >= pressureAtRaisedTarget[0] - PRESSURE_TOLERANCE_PA, "Regulator pump bled pressure from the finite outlet after its runtime target was lowered: sink=" + sinkPressurePa + ", pressureBeforeLowering=" + pressureAtRaisedTarget[0] + " Pa");
            FlowState westFlow = transport.getFlowState(Direction.WEST);
            FlowState eastFlow = transport.getFlowState(Direction.EAST);
            helper.assertTrue(!isPositiveFlow(westFlow, FlowDirection.OUTBOUND), "Runtime-lowered regulator produced reverse outbound flow on its west inlet");
            helper.assertTrue(!isPositiveFlow(eastFlow, FlowDirection.INBOUND), "Runtime-lowered regulator produced reverse inbound flow on its east outlet");

            boolean flowCleared = westFlow == null && eastFlow == null && transport.getThroughputFlowRate() == 0;
            if (phase[0] == 2) {
                if (!flowCleared) {
                    if (phaseTicks[0] > DYNAMIC_TRANSITION_DEADLINE_TICKS) {
                        helper.fail("Airtight regulator pump retained stale forward flow after its runtime target was lowered below the current outlet pressure");
                    }
                    return;
                }

                pressureAfterLowering[0] = sinkPressurePa;
                phase[0] = 3;
                phaseTicks[0] = 0;
                return;
            }

            helper.assertTrue(flowCleared, "Airtight regulator pump resumed flow while its lowered runtime target remained below the finite outlet pressure");
            helper.assertValueEqual(sinkPressurePa, pressureAfterLowering[0], "stable finite outlet pressure after lowering regulator target below the current pressure");
            if (phaseTicks[0] < POST_LOWER_TARGET_STABILITY_TICKS) {
                return;
            }

            helper.succeed();
        });
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 70)
    public static void destroyingRegulatorWithPendingTransferReturnsCustodyExactlyOnce(GameTestHelper helper) {
        AirtightTankBlockEntity sourceTank = placeFiniteTank(helper, SOURCE_POS);
        helper.setBlock(REGULATOR_POS, CCBBlocks.AIRTIGHT_REGULATOR_PUMP_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST));

        GasTank tank = sourceTank.getTankInventory();
        GasHandler sourceHandler = gasHandler(helper);
        long insertedAmount = sourceHandler.fill(new GasStack(CCBGases.NATURAL_AIR.get(), PENDING_REMOVAL_INITIAL_SOURCE_AMOUNT), GasAction.EXECUTE);
        helper.assertValueEqual(insertedAmount, PENDING_REMOVAL_INITIAL_SOURCE_AMOUNT, "initial Natural Air inserted before regulator pending-transfer removal");
        assertTankAmount(helper, tank, PENDING_REMOVAL_INITIAL_SOURCE_AMOUNT, "initial regulator-removal source tank");

        GasTransportBehaviour transport = transport(helper, REGULATOR_POS);
        GasPipeConnection westConnection = transport.getConnection(Direction.WEST);
        helper.assertTrue(westConnection != null, "Airtight regulator pump did not create its west connection to the airtight tank recovery endpoint");
        if (westConnection == null) {
            throw new NullPointerException("Airtight regulator pump did not create its west connection to the airtight tank recovery endpoint.");
        }

        GasStack drainedForCustody = sourceHandler.drain(PENDING_REMOVAL_TRANSFER_AMOUNT, GasAction.EXECUTE);
        helper.assertTrue(drainedForCustody.is(CCBGases.NATURAL_AIR.get()), "Regulator removal custody checkpoint drained the wrong gas");
        helper.assertValueEqual(drainedForCustody.getAmount(), PENDING_REMOVAL_TRANSFER_AMOUNT, "Natural Air amount moved into regulator pending custody");
        helper.assertTrue(westConnection.retainPendingTransfer(drainedForCustody), "Regulator west connection rejected the pending Natural Air custody transfer");
        helper.assertTrue(westConnection.hasPendingTransfer(), "Regulator west connection did not retain pending Natural Air custody");

        long sourceAfterDrain = PENDING_REMOVAL_INITIAL_SOURCE_AMOUNT - PENDING_REMOVAL_TRANSFER_AMOUNT;
        assertTankAmount(helper, tank, sourceAfterDrain, "regulator-removal source tank after pending custody was established");
        helper.assertTrue(helper.getLevel().getCapability(GasCapabilities.BLOCK, helper.absolutePos(SOURCE_POS), Direction.EAST) != null, "Regulator removal recovery endpoint was unavailable before destruction");

        BlockPos absoluteRegulatorPos = helper.absolutePos(REGULATOR_POS);
        boolean destroyed = helper.getLevel().destroyBlock(absoluteRegulatorPos, false);
        helper.assertTrue(destroyed, "Runtime destruction did not remove the airtight regulator pump holding pending custody");
        helper.assertTrue(helper.getLevel().getBlockState(absoluteRegulatorPos).isAir(), "Destroyed airtight regulator pump position was not air");
        helper.assertTrue(helper.getLevel().getBlockEntity(absoluteRegulatorPos) == null, "Destroyed airtight regulator pump block entity remained in the level");

        int[] phase = new int[1];
        int[] phaseTicks = new int[1];
        helper.onEachTick(() -> {
            phaseTicks[0]++;
            helper.assertTrue(helper.getLevel().getBlockState(absoluteRegulatorPos).isAir(), "Destroyed airtight regulator pump unexpectedly reappeared during pending-transfer recovery");
            helper.assertTrue(helper.getLevel().getBlockEntity(absoluteRegulatorPos) == null, "Destroyed airtight regulator pump block entity unexpectedly reappeared during pending-transfer recovery");
            helper.assertTrue(helper.getLevel().getCapability(GasCapabilities.BLOCK, helper.absolutePos(SOURCE_POS), Direction.EAST) != null, "Regulator removal recovery endpoint disappeared after destruction");

            long sourceAmount = tank.getStoredAmount();
            helper.assertTrue(sourceAmount <= PENDING_REMOVAL_INITIAL_SOURCE_AMOUNT, "Destroying the airtight regulator pump duplicated pending gas into the source tank");
            if (phase[0] == 0) {
                if (sourceAmount == PENDING_REMOVAL_INITIAL_SOURCE_AMOUNT) {
                    phase[0] = 1;
                    phaseTicks[0] = 0;
                    return;
                }

                helper.assertValueEqual(sourceAmount, sourceAfterDrain, "source amount while waiting for regulator-removal pending recovery");
                if (phaseTicks[0] > PENDING_REMOVAL_RECOVERY_DEADLINE_TICKS) {
                    helper.fail("Destroying an airtight regulator pump discarded its active pending transfer instead of returning custody to the available source endpoint");
                }
                return;
            }

            helper.assertValueEqual(sourceAmount, PENDING_REMOVAL_INITIAL_SOURCE_AMOUNT, "exactly-once source amount after regulator pending-transfer recovery");
            if (phaseTicks[0] < PENDING_REMOVAL_STABILITY_TICKS) {
                return;
            }

            helper.succeed();
        });
    }

    @GameTest(template = "gametest/empty_8x3x7")
    public static void separatedRegulatorsWithUnequalLimitsMaintainFlow(GameTestHelper helper) {
        assertSeriesMaintainsFlow(helper, 2);
    }

    @GameTest(template = "gametest/empty_8x3x7")
    public static void threeSeparatedRegulatorsMaintainFlow(GameTestHelper helper) {
        assertSeriesMaintainsFlow(helper, 3);
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 80)
    public static void quantizedRegulatorTransferRespectsRemainingBudget(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        placeCreativeBoundary(helper, SOURCE_POS, SOURCE_PRESSURE_PA);
        placeCreativeBoundary(helper, SINK_POS, DEFAULT_OUTLET_SET_PRESSURE_PA);
        AirtightRegulatorPumpBlockEntity regulator = placePoweredRegulator(helper, REGULATOR_POS, POWER_POS, GasPressure.pascals(16));
        helper.succeedWhen(() -> {
            assertRegulatorPowered(helper, regulator);
            GasStack gas = new GasStack(CCBGases.NATURAL_AIR.get(), 1);
            GasTank source = new GasTank(8000000, GasPressure.pascals(16));
            GasTank sink = new GasTank(8000000, GasPressure.pascals(16));
            long initialSource = 8000004;
            long initialSink = GasPressure.amount(sink.getVolume(), SOURCE_PRESSURE_PA + regulator.getMaxPressureRisePa());
            source.tryReplaceContents(gas.copyWithAmount(initialSource)).requireAccepted();
            sink.tryReplaceContents(gas.copyWithAmount(initialSink)).requireAccepted();
            BlockPos pumpPos = helper.absolutePos(REGULATOR_POS);
            Snapshot topology = GasNetworkTopology.get(level, pumpPos);
            PreparedGraph graph = GasPressureGraphSolver.prepare(level, topology, gas);
            List<GasNetworkPressureEndpoint> endpoints = List.of(quantizedEndpoint(source, pumpPos, Direction.WEST), quantizedEndpoint(sink, pumpPos, Direction.EAST));
            GasTransportFlowBudget budget = new GasTransportFlowBudget();
            budget.remaining(pumpPos, regulator.getFlowRateLimit());
            budget.consume(pumpPos, regulator.getFlowRateLimit() - 1);
            GasPressureGraphSolution solution = graph.solve(endpoints, budget);
            helper.assertValueEqual(solution.transferComponents().size(), 1, "near-equilibrium component count");
            TransferComponent component = solution.transferComponents().getFirst();
            double fraction = GasEndpointTransferPlanner.calculateSubstepFraction(solution, 1, budget);
            GasTransferPlan normalPlan = GasEndpointTransferPlanner.plan(level, gas, component, fraction, 0);
            helper.assertTrue(normalPlan.isEmpty(), "Fixture did not require quantized transfer");
            GasTransferPlan plan = GasQuantizedEquilibriumPlanner.plan(level, graph, gas, endpoints, solution, component, budget, 0);
            helper.assertValueEqual(plan.totalAmount(), 1L, "quantized amount with one GU remaining");
            PooledTransferExecutionResult execution = GasTransferExecutor.executePooledTransfer(gas, plan.executorDrains(), plan.executorFills());
            helper.assertValueEqual(execution.filledAmounts()[0], 1L, "executed quantized amount");
            helper.assertValueEqual(source.getStoredAmount() + sink.getStoredAmount(), initialSource + initialSink, "quantized gas conservation");
            budget.consumeFlowRates(component.budgetedFlowRates(), plan.totalAmount() / component.referenceFlowRate());
            helper.assertValueEqual(budget.remainingKnown(pumpPos), 0L, "exhausted pump budget");
            helper.assertTrue(GasQuantizedEquilibriumPlanner.plan(level, graph, gas, endpoints, solution, component, budget, 1).isEmpty(), "Exhausted budget allowed another quantized transfer");
        });
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void transportBudgetChecksEveryPumpWithConsumptionRounding(GameTestHelper helper) {
        GasTransportFlowBudget budget = new GasTransportFlowBudget();
        budget.remaining(SOURCE_POS, 100);
        budget.remaining(SINK_POS, 1);
        Map<BlockPos, Double> rates = Map.of(SOURCE_POS, 10.0, SINK_POS, 0.01);
        helper.assertTrue(budget.canConsumeFlowRates(rates, 1), "One-GU remainder rejected a rounded sub-unit flow");
        budget.consumeFlowRates(rates, 1);
        helper.assertValueEqual(budget.remainingKnown(SOURCE_POS), 90L, "first pump remainder");
        helper.assertValueEqual(budget.remainingKnown(SINK_POS), 0L, "sub-unit flow rounds up to one GU");
        helper.assertTrue(!budget.canConsumeFlowRates(rates, 1), "An exhausted downstream pump was ignored");
        helper.succeed();
    }

    private static void assertSeriesMaintainsFlow(GameTestHelper helper, int count) {
        placeCreativeBoundary(helper, SOURCE_POS, GasPressure.REFERENCE_PRESSURE_PA);
        placeCreativeBoundary(helper, new BlockPos(2 * count, 1, 1), GasPressure.pascals(3));
        AirtightRegulatorPumpBlockEntity[] regulators = new AirtightRegulatorPumpBlockEntity[count];
        GasTransportBehaviour[] transports = new GasTransportBehaviour[count];
        for (int index = 0; index < count; index++) {
            BlockPos pos = new BlockPos(1 + 2 * index, 1, 1);
            BlockPos motorPos = pos.south();
            regulators[index] = placePoweredRegulator(helper, pos, motorPos, GasPressure.pascals(4));
            CreativeMotorBlockEntity motor = (CreativeMotorBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(motorPos));
            if (motor == null) {
                throw new NullPointerException("Creative motor block entity is missing at " + motorPos + '.');
            }

            motor.generatedSpeed.setValue(index == 0 ? 64 : 128);
            transports[index] = transport(helper, pos);
            if (!(index + 1 < count)) {
                continue;
            }

            helper.setBlock(pos.east(), CCBBlocks.AIRTIGHT_PIPE_BLOCK.getDefaultState().setValue(RotatedPillarBlock.AXIS, Axis.X));
        }
        int[] stableTicks = {0};
        helper.succeedWhen(() -> {
            for (int index = 0; index < count; index++) {
                assertRegulatorPowered(helper, regulators[index]);
                if (!hasPositiveForwardFlow(transports[index])) {
                    stableTicks[0] = 0;
                }
                long flow = assertForwardFlow(helper, transports[index], "separated regulator " + index);
                helper.assertTrue(flow <= regulators[index].getFlowRateLimit(), "Series exceeded pump flow limit");
                helper.assertValueEqual(flow, transports[0].getThroughputFlowRate(), "conserved series flow");
            }
            helper.assertTrue(++stableTicks[0] >= 20, "Series flow has not remained stable for 20 ticks");
        });
    }

    private static GasNetworkPressureEndpoint quantizedEndpoint(GasTank tank, BlockPos pumpPos, Direction face) {
        List<BlockFace> faces = List.of(new BlockFace(pumpPos, face));
        long amount = tank.getStoredAmount();
        double pressure = (double) amount * GasPressure.REFERENCE_PRESSURE_PA / tank.getVolume();
        return new GasNetworkPressureEndpoint(new TransferAccess(tank, tank, faces, faces), new PressureState(pressure, false, amount, tank.getVolume(), tank.getMaxPressurePa(), tank.getMaxAmount()), new TransferLimits(amount, tank.getMaxAmount() - amount), new Recovery(List.of(tank), null, null, false));
    }

    private static AirtightRegulatorPumpBlockEntity placePoweredRegulator(GameTestHelper helper, BlockPos regulatorPos, BlockPos powerPos, long outletSetPressurePa) {
        return placePoweredRegulator(helper, regulatorPos, powerPos, outletSetPressurePa, Direction.EAST);
    }

    private static AirtightRegulatorPumpBlockEntity placePoweredRegulator(GameTestHelper helper, BlockPos regulatorPos, BlockPos powerPos, long outletSetPressurePa, Direction outputDirection) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(regulatorPos, CCBBlocks.AIRTIGHT_REGULATOR_PUMP_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.FACING, outputDirection));
        Direction motorFacing;
        if (regulatorPos.getX() != powerPos.getX()) {
            motorFacing = regulatorPos.getX() > powerPos.getX() ? Direction.EAST : Direction.WEST;
        }
        else if (regulatorPos.getY() != powerPos.getY()) {
            motorFacing = regulatorPos.getY() > powerPos.getY() ? Direction.UP : Direction.DOWN;
        }
        else {
            motorFacing = regulatorPos.getZ() > powerPos.getZ() ? Direction.SOUTH : Direction.NORTH;
        }
        helper.setBlock(powerPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, motorFacing));

        BlockEntity regulatorBlockEntity = level.getBlockEntity(helper.absolutePos(regulatorPos));
        helper.assertTrue(regulatorBlockEntity instanceof AirtightRegulatorPumpBlockEntity, "Airtight regulator pump was not initialized at " + regulatorPos);
        if (!(regulatorBlockEntity instanceof AirtightRegulatorPumpBlockEntity regulator)) {
            throw new IllegalStateException("Airtight regulator pump was not initialized at " + regulatorPos + '.');
        }

        regulator.setOutletSetPressurePa(outletSetPressurePa);
        BlockEntity motorBlockEntity = level.getBlockEntity(helper.absolutePos(powerPos));
        helper.assertTrue(motorBlockEntity instanceof CreativeMotorBlockEntity, "Creative motor was not initialized at " + powerPos);
        if (!(motorBlockEntity instanceof CreativeMotorBlockEntity motor)) {
            throw new IllegalStateException("Creative motor was not initialized at " + powerPos + '.');
        }

        motor.generatedSpeed.setValue(CreativeMotorBlockEntity.MAX_SPEED);
        return regulator;
    }

    private static CreativeAirtightTankBlockEntity placeCreativeBoundary(GameTestHelper helper, BlockPos pos, long pressurePa) {
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

    private static GasStorageHandler placeFilledInjectionChamber(GameTestHelper helper, BlockPos pos, long pressurePa) {
        helper.setBlock(pos, CCBBlocks.GAS_INJECTION_CHAMBER_BLOCK.get().defaultBlockState());
        GasHandler handler = helper.getLevel().getCapability(GasCapabilities.BLOCK, helper.absolutePos(pos), Direction.UP);
        helper.assertTrue(handler instanceof GasStorageHandler, "Gas injection chamber did not expose pressure-aware gas storage on its top face");
        if (!(handler instanceof GasStorageHandler storage)) {
            throw new IllegalStateException("Gas injection chamber did not expose pressure-aware gas storage on its top face.");
        }

        long amount = GasPressure.amount(storage.getTankVolume(0), pressurePa);
        long filled = storage.fill(new GasStack(CCBGases.NATURAL_AIR.get(), amount), GasAction.EXECUTE);
        helper.assertValueEqual(filled, amount, "gas injection chamber source fill amount");
        helper.assertValueEqual(storage.getTankPressurePa(0), pressurePa, "gas injection chamber source pressure");
        return storage;
    }

    private static AirtightTankBlockEntity placeTwoByTwoByTwoFiniteTank(GameTestHelper helper, BlockPos controllerPos) {
        AirtightTankBlockEntity controller = null;
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 2; x++) {
                for (int z = 0; z < 2; z++) {
                    AirtightTankBlockEntity tank = placeFiniteTank(helper, controllerPos.offset(x, y, z));
                    if (!(x == 0 && y == 0 && z == 0)) {
                        continue;
                    }

                    controller = tank;
                }
            }
        }

        GasTankMultiblockConnectivity.formMultiblock(controller, helper.getLevel());
        helper.assertTrue(controller.isController(), "Expected lower northwest 2x2x2 airtight tank block to be the controller");
        helper.assertValueEqual(controller.getWidth(), 2, "2x2x2 airtight tank width");
        helper.assertValueEqual(controller.getHeight(), 2, "2x2x2 airtight tank height");
        helper.assertValueEqual(controller.getTotalTankSize(), 8, "2x2x2 airtight tank block count");
        return controller;
    }

    private static AirtightTankBlockEntity placeFiniteTank(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, CCBBlocks.AIRTIGHT_TANK_BLOCK.get().defaultBlockState());
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        helper.assertTrue(blockEntity instanceof AirtightTankBlockEntity, "Airtight tank was not initialized at " + pos);
        if (!(blockEntity instanceof AirtightTankBlockEntity tank)) {
            throw new IllegalStateException("Airtight tank was not initialized at " + pos + '.');
        }

        return tank;
    }

    private static CreativeMotorBlockEntity motor(GameTestHelper helper) {
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(POWER_POS));
        helper.assertTrue(blockEntity instanceof CreativeMotorBlockEntity, "Creative motor was not initialized at " + POWER_POS);
        if (!(blockEntity instanceof CreativeMotorBlockEntity motor)) {
            throw new IllegalStateException("Creative motor was not initialized at " + POWER_POS + '.');
        }

        return motor;
    }

    private static boolean isRegulatorPowered(AirtightRegulatorPumpBlockEntity regulator) {
        return Mth.abs(regulator.getSpeed()) >= SpeedLevel.MEDIUM.getSpeedValue() && regulator.getFlowRateLimit() > 0;
    }

    private static boolean hasPositiveForwardFlow(GasTransportBehaviour transport) {
        return isPositiveFlow(transport.getFlowState(Direction.WEST), FlowDirection.INBOUND) && isPositiveFlow(transport.getFlowState(Direction.EAST), FlowDirection.OUTBOUND);
    }

    private static boolean isPositiveFlow(@Nullable FlowState flowState, FlowDirection expectedDirection) {
        return flowState != null && flowState.direction() == expectedDirection && flowState.flowRate() > 0;
    }

    private static String describeTransport(GasTransportBehaviour transport) {
        FlowState west = transport.getFlowState(Direction.WEST);
        FlowState east = transport.getFlowState(Direction.EAST);
        return "throughput=" + transport.getThroughputFlowRate() + ", west=" + describeFlow(west) + ", east=" + describeFlow(east);
    }

    private static String describeFlow(@Nullable FlowState flowState) {
        if (flowState == null) {
            return "none";
        }

        return String.valueOf(flowState.direction()) + ':' + flowState.flowRate() + " GU/t";
    }

    private static long assertForwardFlow(GameTestHelper helper, GasTransportBehaviour transport, String stage) {
        return assertForwardFlow(helper, transport, Direction.WEST, Direction.EAST, stage);
    }

    private static long assertForwardFlow(GameTestHelper helper, GasTransportBehaviour transport, Direction inputFace, Direction outputFace, String stage) {
        FlowState inputFlow = transport.getFlowState(inputFace);
        FlowState outputFlow = transport.getFlowState(outputFace);
        helper.assertTrue(inputFlow != null, stage + " did not have " + inputFace.getName() + "-face flow");
        if (inputFlow == null) {
            throw new NullPointerException("Required test object is missing: " + stage + " did not have " + inputFace.getName() + "-face flow" + '.');
        }

        helper.assertTrue(outputFlow != null, stage + " did not have " + outputFace.getName() + "-face flow");
        if (outputFlow == null) {
            throw new NullPointerException("Required test object is missing: " + stage + " did not have " + outputFace.getName() + "-face flow" + '.');
        }

        helper.assertTrue(inputFlow.direction() == FlowDirection.INBOUND, stage + ' ' + inputFace.getName() + " face was not inbound");
        helper.assertTrue(outputFlow.direction() == FlowDirection.OUTBOUND, stage + ' ' + outputFace.getName() + " face was not outbound");
        helper.assertTrue(inputFlow.flowRate() > 0, stage + " flow rate was not positive");
        helper.assertValueEqual(outputFlow.flowRate(), inputFlow.flowRate(), stage + " face flow rate");
        helper.assertValueEqual(transport.getThroughputFlowRate(), inputFlow.flowRate(), stage + " throughput");
        helper.assertTrue(inputFlow.gas().is(CCBGases.NATURAL_AIR.get()), stage + ' ' + inputFace.getName() + "-face gas was not Natural Air");
        helper.assertTrue(outputFlow.gas().is(CCBGases.NATURAL_AIR.get()), stage + ' ' + outputFace.getName() + "-face gas was not Natural Air");
        return inputFlow.flowRate();
    }

    private static GasHandler gasHandler(GameTestHelper helper) {
        GasHandler handler = helper.getLevel().getCapability(GasCapabilities.BLOCK, helper.absolutePos(SOURCE_POS), Direction.EAST);
        helper.assertTrue(handler != null, "airtight tank recovery endpoint for regulator removal was not initialized on " + Direction.EAST.getName());
        if (handler == null) {
            throw new NullPointerException("Airtight tank recovery endpoint for regulator removal was not initialized on " + Direction.EAST.getName() + '.');
        }

        return handler;
    }

    private static void assertTankAmount(GameTestHelper helper, GasTank tank, long expectedAmount, String description) {
        GasStack contents = tank.getGasStack();
        if (expectedAmount == 0) {
            helper.assertTrue(contents.isEmpty(), description + " was not empty");
            helper.assertValueEqual(tank.getStoredAmount(), 0L, description + " stored amount");
            return;
        }

        helper.assertTrue(contents.is(CCBGases.NATURAL_AIR.get()), description + " did not contain Natural Air");
        helper.assertValueEqual(contents.getAmount(), expectedAmount, description + " Natural Air amount");
    }

    private static GasTransportBehaviour transport(GameTestHelper helper, BlockPos pos) {
        GasTransportBehaviour transport = BlockEntityBehaviour.get(helper.getLevel(), helper.absolutePos(pos), GasTransportBehaviour.TYPE);
        helper.assertTrue(transport != null, "GasTransportBehaviour was not initialized at " + pos);
        if (transport == null) {
            throw new NullPointerException("GasTransportBehaviour was not initialized at " + pos + '.');
        }

        return transport;
    }

    private static void assertRegulatorPowered(GameTestHelper helper, AirtightRegulatorPumpBlockEntity regulator) {
        helper.assertTrue(Mth.abs(regulator.getSpeed()) >= SpeedLevel.MEDIUM.getSpeedValue(), "Airtight regulator pump did not receive sustained medium-or-faster kinetic power");
    }

    private static void assertNoPositiveFlow(GameTestHelper helper, @Nullable FlowState flowState, String description) {
        if (flowState == null) {
            return;
        }

        helper.assertValueEqual(flowState.flowRate(), 0L, description);
    }
}
