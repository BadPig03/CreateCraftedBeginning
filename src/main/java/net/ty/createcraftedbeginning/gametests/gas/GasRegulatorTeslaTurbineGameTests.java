package net.ty.createcraftedbeginning.gametests.gas;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtightregulatorpump.AirtightRegulatorPumpBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.teslaturbinenozzle.TeslaTurbineNozzleBlock;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasConnectionResolver;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology.Snapshot;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasEndpointPlanner;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolver;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolver.PreparedGraph;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransportFlowBudget;
import net.ty.createcraftedbeginning.gas.storage.CreativeGasReservoir;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasRegulatorTeslaTurbineGameTests {
    private static final int REGULATOR_SPEED_RPM = 256;
    private static final long SOURCE_PRESSURE_PA = GasPressure.pascals(16);
    private static final long TARGET_PRESSURE_STEP_PA = GasPressure.pascals(0.5);
    private static final int STRUCTURE_SETUP_TICK = 12;
    private static final int FIRST_FLOW_CHECK_TICK = 40;
    private static final int LAST_FLOW_CHECK_TICK = 64;
    private static final int TARGET_CHANGE_INTERVAL_TICKS = 25;
    private static final int TARGET_CHANGE_COUNT = 6;
    private static final BlockPos TURBINE_POS = new BlockPos(3, 1, 3);
    private static final BlockPos NOZZLE_POS = new BlockPos(5, 1, 4);
    private static final BlockPos TRANSPORT_POS = new BlockPos(6, 1, 4);
    private static final BlockPos SOURCE_POS = new BlockPos(7, 1, 4);
    private static final BlockPos MOTOR_POS = new BlockPos(6, 1, 5);

    private GasRegulatorTeslaTurbineGameTests() {
    }

    @GameTest(template = "gametest/empty_8x3x7")
    public static void equalSourceAndRegulatorTargetSupplyTeslaNozzle(GameTestHelper helper) {
        verifySupply(helper, true, false, GasPressure.pascals(16), false);
    }

    @GameTest(template = "gametest/empty_8x3x7")
    public static void passivePipeSuppliesSameTeslaNozzle(GameTestHelper helper) {
        verifySupply(helper, false, false, GasPressure.pascals(16), false);
    }

    @GameTest(template = "gametest/empty_20x12x20")
    public static void thirteenAtmRegulatorSuppliesNozzleThroughInletAndOutletPipes(GameTestHelper helper) {
        verifySupply(helper, true, true, GasPressure.pascals(13), false);
    }

    @GameTest(template = "gametest/empty_20x12x20")
    public static void thirteenAndHalfAtmRegulatorSuppliesNozzleThroughInletAndOutletPipes(GameTestHelper helper) {
        verifySupply(helper, true, true, GasPressure.pascals(13.5), false);
    }

    @GameTest(template = "gametest/empty_20x12x20")
    public static void sixteenAtmRegulatorSuppliesNozzleThroughInletAndOutletPipes(GameTestHelper helper) {
        verifySupply(helper, true, true, GasPressure.pascals(16), false);
    }

    @GameTest(template = "gametest/empty_20x12x20")
    public static void fourteenAtmRegulatorSuppliesNozzleThroughInletAndOutletPipes(GameTestHelper helper) {
        verifySupply(helper, true, true, GasPressure.pascals(14), false);
    }

    @GameTest(template = "gametest/empty_20x12x20")
    public static void fourteenAndHalfAtmRegulatorSuppliesNozzleThroughInletAndOutletPipes(GameTestHelper helper) {
        verifySupply(helper, true, true, GasPressure.pascals(14.5), false);
    }

    @GameTest(template = "gametest/empty_20x12x20")
    public static void fifteenAtmRegulatorSuppliesNozzleThroughInletAndOutletPipes(GameTestHelper helper) {
        verifySupply(helper, true, true, GasPressure.pascals(15), false);
    }

    @GameTest(template = "gametest/empty_20x12x20")
    public static void fifteenAndHalfAtmRegulatorSuppliesNozzleThroughInletAndOutletPipes(GameTestHelper helper) {
        verifySupply(helper, true, true, GasPressure.pascals(15.5), false);
    }

    @GameTest(template = "gametest/empty_20x12x20", timeoutTicks = 260)
    public static void increasingTargetWithInletAndOutletPipesKeepsSupplyingNozzle(GameTestHelper helper) {
        verifySupply(helper, true, true, GasPressure.pascals(13), true);
    }

    private static void verifySupply(GameTestHelper helper, boolean regulated, boolean extraPipes, long targetPressurePa, boolean increaseTarget) {
        BlockPos sourcePos = extraPipes ? SOURCE_POS.east(2) : SOURCE_POS;
        BlockPos regulatorPos = extraPipes ? TRANSPORT_POS.east() : TRANSPORT_POS;
        BlockPos motorPos = extraPipes ? MOTOR_POS.east() : MOTOR_POS;
        ServerLevel level = helper.getLevel();
        GasStack gas = new GasStack(CCBGases.NATURAL_AIR.get(), 1);
        helper.setBlock(TURBINE_POS, CCBBlocks.TESLA_TURBINE_BLOCK.getDefaultState().setValue(RotatedPillarBlock.AXIS, Axis.Y));
        helper.setBlock(sourcePos, CCBBlocks.CREATIVE_AIRTIGHT_TANK_BLOCK.getDefaultState());
        BlockEntity sourceEntity = level.getBlockEntity(helper.absolutePos(sourcePos));
        if (sourceEntity == null) {
            throw new NullPointerException("Tesla supply source block entity is missing at " + sourcePos + '.');
        }

        if (!(sourceEntity instanceof CreativeAirtightTankBlockEntity source)) {
            throw new IllegalStateException("Expected a creative airtight tank at " + sourcePos + '.');
        }

        CreativeGasReservoir inventory = source.getTankInventory();
        inventory.setFixedPressurePa(SOURCE_PRESSURE_PA);
        inventory.setContainedGas(gas);
        helper.runAtTickTime(STRUCTURE_SETUP_TICK, () -> {
            helper.setBlock(NOZZLE_POS, CCBBlocks.TESLA_TURBINE_NOZZLE_BLOCK.getDefaultState().setValue(TeslaTurbineNozzleBlock.FACING, Direction.EAST).setValue(TeslaTurbineNozzleBlock.CLOCKWISE, true));
            BlockState pipeState = CCBBlocks.AIRTIGHT_PIPE_BLOCK.getDefaultState().setValue(RotatedPillarBlock.AXIS, Axis.X);
            if (!regulated) {
                helper.setBlock(TRANSPORT_POS, pipeState);
                return;
            }

            if (extraPipes) {
                helper.setBlock(TRANSPORT_POS, pipeState);
                helper.setBlock(regulatorPos.east(), pipeState);
            }
            helper.setBlock(regulatorPos, CCBBlocks.AIRTIGHT_REGULATOR_PUMP_BLOCK.getDefaultState().setValue(BlockStateProperties.FACING, Direction.WEST));
            AirtightRegulatorPumpBlockEntity regulator = helper.getBlockEntity(regulatorPos);
            regulator.setOutletSetPressurePa(targetPressurePa);
            helper.setBlock(motorPos, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.NORTH));
            CreativeMotorBlockEntity motor = helper.getBlockEntity(motorPos);
            motor.generatedSpeed.setValue(REGULATOR_SPEED_RPM);
        });
        int lastCheckTick = increaseTarget ? LAST_FLOW_CHECK_TICK + TARGET_CHANGE_COUNT * TARGET_CHANGE_INTERVAL_TICKS : LAST_FLOW_CHECK_TICK;
        for (int tick = FIRST_FLOW_CHECK_TICK; tick <= lastCheckTick; tick++) {
            int checkTick = tick;
            helper.runAtTickTime(checkTick, () -> {
                BlockPos transportPos = helper.absolutePos(regulatorPos);
                GasTransportBehaviour transport = GasConnectionResolver.getTransportBehaviour(level, transportPos);
                if (transport == null) {
                    throw new NullPointerException("Tesla supply transport behaviour is missing at " + transportPos + '.');
                }

                long currentTargetPressurePa = targetPressurePa;
                if (regulated) {
                    AirtightRegulatorPumpBlockEntity regulator = helper.getBlockEntity(regulatorPos);
                    int targetChangeTick = checkTick - LAST_FLOW_CHECK_TICK - 1;
                    if (increaseTarget && targetChangeTick >= 0 && targetChangeTick % TARGET_CHANGE_INTERVAL_TICKS == 0) {
                        int increments = targetChangeTick / TARGET_CHANGE_INTERVAL_TICKS + 1;
                        regulator.setOutletSetPressurePa(targetPressurePa + increments * TARGET_PRESSURE_STEP_PA);
                    }
                    currentTargetPressurePa = regulator.getOutletSetPressurePa();
                    helper.assertTrue(Math.abs(regulator.getSpeed()) == REGULATOR_SPEED_RPM, "Regulator must have independent " + REGULATOR_SPEED_RPM + " RPM power");
                    helper.assertTrue(regulator.getFlowRateLimit() > 0, "Powered regulator flow limit is zero");
                }
                Snapshot topology = GasNetworkTopology.get(level, transportPos);
                GasEndpointPlanner planner = GasEndpointPlanner.prepare(level, topology);
                PreparedGraph prepared = GasPressureGraphSolver.prepare(level, topology, gas);
                GasPressureGraphSolution solution = prepared.solveHypothetical(planner.planPressureEndpoints(level, gas), new GasTransportFlowBudget());
                long throughput = transport.getThroughputFlowRate();
                helper.assertTrue(solution.converged(), "Tesla supply pressure graph failed to converge");
                if (extraPipes) {
                    helper.assertTrue(throughput > 0, "Regulator with inlet and outlet pipes has zero flow at target " + currentTargetPressurePa + " Pa");
                }
                else {
                    helper.assertValueEqual(throughput, regulated ? 530L : 517L, "Tesla supply transport throughput");
                }
                if (checkTick < lastCheckTick) {
                    return;
                }

                helper.succeed();
            });
        }
    }
}
