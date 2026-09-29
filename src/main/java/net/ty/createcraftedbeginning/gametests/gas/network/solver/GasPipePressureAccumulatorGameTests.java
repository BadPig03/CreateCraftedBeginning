package net.ty.createcraftedbeginning.gametests.gas.network.solver;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtightmeters.AirtightManometerBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankBlockEntity;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology.Snapshot;
import net.ty.createcraftedbeginning.gas.network.solver.GasPipePressureAccumulator;
import net.ty.createcraftedbeginning.gas.network.solver.GasPipeTelemetryTargets;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasEndpointPlanner;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolution.FacePressureSamples;
import net.ty.createcraftedbeginning.gas.network.solver.graph.GasPressureGraphSolver;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransportFlowBudget;
import net.ty.createcraftedbeginning.gas.storage.CreativeGasReservoir;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasPipePressureAccumulatorGameTests {
    private GasPipePressureAccumulatorGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void emptyAndFailedSamplesDoNotCrashOrReplacePressureLayout(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos meterPos = new BlockPos(1, 1, 1);
        GasStack gas = new GasStack(CCBGases.NATURAL_AIR.get(), 1);
        for (BlockPos tankPos : List.of(new BlockPos(0, 1, 1), new BlockPos(2, 1, 1))) {
            helper.setBlock(tankPos, CCBBlocks.CREATIVE_AIRTIGHT_TANK_BLOCK.getDefaultState());
            BlockEntity tankEntity = level.getBlockEntity(helper.absolutePos(tankPos));
            if (tankEntity == null) {
                throw new NullPointerException("Pressure telemetry test tank block entity is missing at " + tankPos + '.');
            }

            if (!(tankEntity instanceof CreativeAirtightTankBlockEntity tank)) {
                throw new IllegalStateException("Expected a creative airtight tank block entity at " + tankPos + '.');
            }

            CreativeGasReservoir inventory = tank.getTankInventory();
            inventory.setFixedPressurePa(GasPressure.REFERENCE_PRESSURE_PA);
            inventory.setContainedGas(gas.copy());
        }
        helper.setBlock(meterPos, CCBBlocks.AIRTIGHT_MANOMETER_BLOCK.getDefaultState().setValue(RotatedPillarBlock.AXIS, Axis.X));
        BlockPos absoluteMeterPos = helper.absolutePos(meterPos);
        BlockEntity meterEntity = level.getBlockEntity(absoluteMeterPos);
        if (meterEntity == null) {
            throw new NullPointerException("Pressure telemetry test manometer block entity is missing at " + meterPos + '.');
        }

        if (!(meterEntity instanceof AirtightManometerBlockEntity meter)) {
            throw new IllegalStateException("Expected an airtight manometer block entity at " + meterPos + '.');
        }

        Snapshot topology = GasNetworkTopology.get(level, absoluteMeterPos);
        GasPipeTelemetryTargets targets = GasPipeTelemetryTargets.get(level, topology);
        GasPipePressureAccumulator accumulator = new GasPipePressureAccumulator(targets);
        meter.acceptPressureTelemetry(GasPressure.REFERENCE_PRESSURE_PA, GasPressure.REFERENCE_PRESSURE_PA);
        helper.assertValueEqual(accumulator.record(GasPressureGraphSolution.FAILED.facePressures()), 0, "failed solve sample count");
        helper.assertValueEqual(accumulator.record(GasPressureGraphSolution.EMPTY.facePressures()), 0, "empty solve sample count");
        helper.assertValueEqual(accumulator.record(FacePressureSamples.EMPTY), 0, "compact empty sample count");
        accumulator.finishGas();
        accumulator.apply(level);
        helper.assertTrue(!meter.hasPressureReading(), "A tick without pressure samples retained stale telemetry");

        GasEndpointPlanner endpoints = GasEndpointPlanner.prepare(level, topology);
        GasPressureGraphSolution solution = GasPressureGraphSolver.prepare(level, topology, gas).solve(endpoints.planPressureEndpoints(level, gas), new GasTransportFlowBudget());
        helper.assertTrue(solution.converged(), "Equal-pressure boundary fixture failed to converge");
        helper.assertTrue(solution.isEmpty(), "Equal-pressure boundaries unexpectedly produced transfer components");
        helper.assertTrue(accumulator.record(solution.facePressures()) > 0, "A successful zero-flow solve produced no pressure samples");
        helper.assertValueEqual(accumulator.record(FacePressureSamples.EMPTY), 0, "compact empty samples after a valid layout");
        helper.assertValueEqual(accumulator.record(GasPressureGraphSolution.FAILED.facePressures()), 0, "failed samples after a valid layout");
        accumulator.finishGas();
        accumulator.apply(level);
        helper.assertTrue(meter.hasPressureReading(), "Empty samples prevented pressure telemetry from recovering");
        helper.assertValueEqual(meter.getMinPressurePa(), GasPressure.REFERENCE_PRESSURE_PA, "recovered minimum pressure");
        helper.assertValueEqual(meter.getMaxPressurePa(), GasPressure.REFERENCE_PRESSURE_PA, "recovered maximum pressure");

        GasPipePressureAccumulator nextTick = new GasPipePressureAccumulator(targets);
        nextTick.record(GasPressureGraphSolution.FAILED.facePressures());
        nextTick.finishGas();
        nextTick.apply(level);
        helper.assertTrue(!meter.hasPressureReading(), "A failed solve on the next tick retained the previous pressure reading");
        helper.succeed();
    }
}
