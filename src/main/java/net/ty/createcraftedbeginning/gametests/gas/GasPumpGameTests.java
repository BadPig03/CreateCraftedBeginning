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
public final class GasPumpGameTests {
    private static final BlockPos SOURCE_TANK_POS = new BlockPos(0, 1, 1);
    private static final BlockPos PUMP_POS = new BlockPos(1, 1, 1);
    private static final BlockPos SINK_TANK_POS = new BlockPos(2, 1, 1);
    private static final BlockPos COG_POS = new BlockPos(1, 1, 2);
    private static final BlockPos MOTOR_POS = new BlockPos(2, 1, 2);
    private static final long BOUNDARY_PRESSURE_PA = GasPressure.REFERENCE_PRESSURE_PA;
    private static final long MAXIMUM_TEST_PRESSURE_PA = GasPressure.pascals(16);

    private GasPumpGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 120)
    public static void airtightPumpProvidesBasicTransport(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        CreativeAirtightTankBlockEntity sourceTank = placeBoundaryTank(helper, SOURCE_TANK_POS);
        CreativeAirtightTankBlockEntity sinkTank = placeBoundaryTank(helper, SINK_TANK_POS);
        helper.setBlock(PUMP_POS, CCBBlocks.AIRTIGHT_PUMP_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST));
        helper.setBlock(COG_POS, AllBlocks.COGWHEEL.getDefaultState().setValue(CogWheelBlock.AXIS, Axis.X));
        helper.setBlock(MOTOR_POS, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.WEST));

        BlockEntity pumpBlockEntity = level.getBlockEntity(helper.absolutePos(PUMP_POS));
        helper.assertTrue(pumpBlockEntity instanceof AirtightPumpBlockEntity, "Airtight pump was not initialized");
        if (!(pumpBlockEntity instanceof AirtightPumpBlockEntity pump)) {
            return;
        }

        BlockEntity motorBlockEntity = level.getBlockEntity(helper.absolutePos(MOTOR_POS));
        helper.assertTrue(motorBlockEntity instanceof CreativeMotorBlockEntity, "Creative motor was not initialized");
        if (!(motorBlockEntity instanceof CreativeMotorBlockEntity motor)) {
            return;
        }

        motor.generatedSpeed.setValue(CreativeMotorBlockEntity.MAX_SPEED);

        helper.assertValueEqual(sourceTank.getTankInventory().getPressurePa(), BOUNDARY_PRESSURE_PA, "pump source pressure");
        helper.assertValueEqual(sinkTank.getTankInventory().getPressurePa(), BOUNDARY_PRESSURE_PA, "pump sink pressure");

        helper.succeedWhen(() -> {
            helper.assertTrue(Mth.abs(pump.getSpeed()) >= SpeedLevel.MEDIUM.getSpeedValue(), "Airtight pump did not receive sustained medium-or-faster kinetic power");
            helper.assertTrue(pump.getPumpMaxPressureBoostPa() > GasPressure.VACUUM_PA, "Powered airtight pump reported no pressure boost at speed " + pump.getSpeed());
            helper.assertTrue(pump.getPumpFlowRateLimit() > 0, "Powered airtight pump reported a zero flow-rate limit at speed " + pump.getSpeed());

            GasTransportBehaviour transport = BlockEntityBehaviour.get(level, helper.absolutePos(PUMP_POS), GasTransportBehaviour.TYPE);
            helper.assertTrue(transport != null, "Airtight pump transport behaviour was not initialized");
            if (transport == null) {
                throw new NullPointerException("Airtight pump transport behaviour was not initialized.");
            }

            BlockState pumpState = level.getBlockState(helper.absolutePos(PUMP_POS));
            helper.assertTrue(transport.allowsInboundFlow(pumpState, Direction.WEST), "East-facing airtight pump did not accept gas on its west inlet");
            helper.assertTrue(!transport.allowsOutboundFlow(pumpState, Direction.WEST), "East-facing airtight pump allowed outbound gas on its west inlet");
            helper.assertTrue(!transport.allowsInboundFlow(pumpState, Direction.EAST), "East-facing airtight pump allowed inbound gas on its east outlet");
            helper.assertTrue(transport.allowsOutboundFlow(pumpState, Direction.EAST), "East-facing airtight pump did not allow gas on its east outlet");

            FlowState inletFlow = transport.getFlowState(Direction.WEST);
            FlowState outletFlow = transport.getFlowState(Direction.EAST);
            helper.assertTrue(inletFlow != null, "Powered airtight pump did not create inlet flow between equal-pressure boundaries");
            if (inletFlow == null) {
                throw new NullPointerException("Powered airtight pump did not create inlet flow between equal-pressure boundaries.");
            }

            helper.assertTrue(outletFlow != null, "Powered airtight pump did not create outlet flow between equal-pressure boundaries");
            if (outletFlow == null) {
                throw new NullPointerException("Powered airtight pump did not create outlet flow between equal-pressure boundaries.");
            }

            helper.assertTrue(inletFlow.direction() == FlowDirection.INBOUND, "Airtight pump west face was not inbound");
            helper.assertTrue(outletFlow.direction() == FlowDirection.OUTBOUND, "Airtight pump east face was not outbound");
            helper.assertTrue(inletFlow.flowRate() > 0, "Airtight pump flow rate was not positive");
            helper.assertValueEqual(outletFlow.flowRate(), inletFlow.flowRate(), "airtight pump face flow rate");
            helper.assertValueEqual(transport.getThroughputFlowRate(), inletFlow.flowRate(), "airtight pump throughput");
            helper.assertTrue(inletFlow.gas().is(CCBGases.NATURAL_AIR.get()), "Airtight pump inlet gas was not Natural Air");
            helper.assertTrue(outletFlow.gas().is(CCBGases.NATURAL_AIR.get()), "Airtight pump outlet gas was not Natural Air");
        });
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 120)
    public static void airtightPumpCapsFlowAtMaximumRate(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        CreativeAirtightTankBlockEntity sourceTank = placeBoundaryTank(helper, SOURCE_TANK_POS);
        CreativeAirtightTankBlockEntity sinkTank = placeBoundaryTank(helper, SINK_TANK_POS);
        sourceTank.getTankInventory().setFixedPressurePa(MAXIMUM_TEST_PRESSURE_PA);
        helper.setBlock(PUMP_POS, CCBBlocks.AIRTIGHT_PUMP_BLOCK.get().defaultBlockState().setValue(BlockStateProperties.FACING, Direction.EAST));
        helper.setBlock(COG_POS, AllBlocks.COGWHEEL.getDefaultState().setValue(CogWheelBlock.AXIS, Axis.X));
        helper.setBlock(MOTOR_POS, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(CreativeMotorBlock.FACING, Direction.WEST));

        BlockEntity pumpBlockEntity = level.getBlockEntity(helper.absolutePos(PUMP_POS));
        helper.assertTrue(pumpBlockEntity instanceof AirtightPumpBlockEntity, "Airtight pump was not initialized");
        if (!(pumpBlockEntity instanceof AirtightPumpBlockEntity pump)) {
            return;
        }

        BlockEntity motorBlockEntity = level.getBlockEntity(helper.absolutePos(MOTOR_POS));
        helper.assertTrue(motorBlockEntity instanceof CreativeMotorBlockEntity, "Creative motor was not initialized");
        if (!(motorBlockEntity instanceof CreativeMotorBlockEntity motor)) {
            return;
        }

        motor.generatedSpeed.setValue(CreativeMotorBlockEntity.MAX_SPEED);

        helper.assertValueEqual(sourceTank.getTankInventory().getPressurePa(), MAXIMUM_TEST_PRESSURE_PA, "pump high-pressure source");
        helper.assertValueEqual(sinkTank.getTankInventory().getPressurePa(), BOUNDARY_PRESSURE_PA, "pump low-pressure sink");
        helper.assertTrue(sourceTank.getTankInventory().getPressurePa() > sinkTank.getTankInventory().getPressurePa(), "Pump flow-limit test did not create a favorable source-to-sink pressure difference");

        helper.succeedWhen(() -> {
            helper.assertTrue(Mth.abs(pump.getSpeed()) >= SpeedLevel.MEDIUM.getSpeedValue(), "Airtight pump did not receive sustained medium-or-faster kinetic power");
            long flowRateLimit = pump.getPumpFlowRateLimit();
            helper.assertTrue(flowRateLimit > 0, "Powered airtight pump reported a zero flow-rate limit at speed " + pump.getSpeed());

            GasTransportBehaviour transport = BlockEntityBehaviour.get(level, helper.absolutePos(PUMP_POS), GasTransportBehaviour.TYPE);
            helper.assertTrue(transport != null, "Airtight pump transport behaviour was not initialized");
            if (transport == null) {
                throw new NullPointerException("Airtight pump transport behaviour was not initialized.");
            }

            FlowState inletFlow = transport.getFlowState(Direction.WEST);
            FlowState outletFlow = transport.getFlowState(Direction.EAST);
            helper.assertTrue(inletFlow != null, "Flow-limited airtight pump did not create inlet flow");
            if (inletFlow == null) {
                throw new NullPointerException("Flow-limited airtight pump did not create inlet flow.");
            }

            helper.assertTrue(outletFlow != null, "Flow-limited airtight pump did not create outlet flow");
            if (outletFlow == null) {
                throw new NullPointerException("Flow-limited airtight pump did not create outlet flow.");
            }

            helper.assertTrue(inletFlow.direction() == FlowDirection.INBOUND, "Flow-limited airtight pump west face was not inbound");
            helper.assertTrue(outletFlow.direction() == FlowDirection.OUTBOUND, "Flow-limited airtight pump east face was not outbound");
            helper.assertTrue(inletFlow.flowRate() > 0, "Flow-limited airtight pump flow rate was not positive");
            helper.assertTrue(inletFlow.flowRate() <= flowRateLimit, "Airtight pump inlet flow exceeded its maximum flow-rate limit: " + inletFlow.flowRate() + " > " + flowRateLimit);
            helper.assertTrue(outletFlow.flowRate() <= flowRateLimit, "Airtight pump outlet flow exceeded its maximum flow-rate limit: " + outletFlow.flowRate() + " > " + flowRateLimit);
            helper.assertValueEqual(inletFlow.flowRate(), flowRateLimit, "airtight pump capped inlet flow rate");
            helper.assertValueEqual(outletFlow.flowRate(), flowRateLimit, "airtight pump capped outlet flow rate");
            helper.assertValueEqual(transport.getThroughputFlowRate(), flowRateLimit, "airtight pump capped throughput");
            helper.assertTrue(inletFlow.gas().is(CCBGases.NATURAL_AIR.get()), "Flow-limited airtight pump inlet gas was not Natural Air");
            helper.assertTrue(outletFlow.gas().is(CCBGases.NATURAL_AIR.get()), "Flow-limited airtight pump outlet gas was not Natural Air");
        });
    }

    private static CreativeAirtightTankBlockEntity placeBoundaryTank(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, CCBBlocks.CREATIVE_AIRTIGHT_TANK_BLOCK.get().defaultBlockState());
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        helper.assertTrue(blockEntity instanceof CreativeAirtightTankBlockEntity, "Creative airtight tank was not initialized at " + pos);
        if (!(blockEntity instanceof CreativeAirtightTankBlockEntity tank)) {
            throw new IllegalStateException("Creative airtight tank was not initialized at " + pos + '.');
        }

        tank.getTankInventory().setFixedPressurePa(BOUNDARY_PRESSURE_PA);
        tank.getTankInventory().setContainedGas(new GasStack(CCBGases.NATURAL_AIR.get(), 1));
        return tank;
    }
}
