package net.ty.createcraftedbeginning.gametests.gas;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
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
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtightcheckvalve.AirtightCheckValveBlock;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankBlockEntity;
import net.ty.createcraftedbeginning.gas.atmosphere.AtmosphereStateResolver;
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
public final class GasCheckValveGameTests {
    private static final BlockPos VALVE_POS = new BlockPos(1, 1, 1);
    private static final BlockPos WEST_POS = VALVE_POS.relative(Direction.WEST);
    private static final BlockPos EAST_POS = VALVE_POS.relative(Direction.EAST);
    private static final long HIGH_PRESSURE_PA = 2 * GasPressure.REFERENCE_PRESSURE_PA;

    private GasCheckValveGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 80)
    public static void checkValveAllowsForwardFlow(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        placeSource(helper, EAST_POS);
        helper.setBlock(VALVE_POS, forwardValveState());

        helper.succeedWhen(() -> {
            GasTransportBehaviour valve = transport(helper);
            BlockState state = level.getBlockState(helper.absolutePos(VALVE_POS));
            assertForwardDirectionality(helper, valve, state);

            FlowState inletFlow = valve.getFlowState(Direction.EAST);
            FlowState outletFlow = valve.getFlowState(Direction.WEST);
            helper.assertTrue(inletFlow != null, "Forward check-valve inlet had no flow");
            if (inletFlow == null) {
                throw new NullPointerException("Forward check-valve inlet had no flow.");
            }

            helper.assertTrue(outletFlow != null, "Forward check-valve outlet had no flow");
            if (outletFlow == null) {
                throw new NullPointerException("Forward check-valve outlet had no flow.");
            }

            helper.assertTrue(inletFlow.direction() == FlowDirection.INBOUND, "East check-valve face was not inbound during forward flow");
            helper.assertTrue(outletFlow.direction() == FlowDirection.OUTBOUND, "West check-valve face was not outbound during forward flow");
            helper.assertTrue(inletFlow.flowRate() > 0, "Forward check-valve flow rate was not positive");
            helper.assertValueEqual(outletFlow.flowRate(), inletFlow.flowRate(), "forward check-valve face flow rate");
            helper.assertValueEqual(valve.getThroughputFlowRate(), inletFlow.flowRate(), "forward check-valve throughput");
            helper.assertTrue(inletFlow.gas().is(CCBGases.NATURAL_AIR.get()), "Forward check-valve inlet gas was not Natural Air");
            helper.assertTrue(outletFlow.gas().is(CCBGases.NATURAL_AIR.get()), "Forward check-valve outlet gas was not Natural Air");

            long atmosphericPressurePa = AtmosphereStateResolver.resolve(level, helper.absolutePos(WEST_POS)).pressurePa();
            helper.assertTrue(atmosphericPressurePa < HIGH_PRESSURE_PA, "Forward-flow test did not create a source-to-atmosphere pressure gradient");
        });
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 80)
    public static void checkValveBlocksReverseFlow(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        placeSource(helper, WEST_POS);
        helper.setBlock(VALVE_POS, forwardValveState());

        helper.succeedWhen(() -> {
            GasTransportBehaviour valve = transport(helper);
            BlockState state = level.getBlockState(helper.absolutePos(VALVE_POS));
            assertForwardDirectionality(helper, valve, state);

            long atmosphericPressurePa = AtmosphereStateResolver.resolve(level, helper.absolutePos(EAST_POS)).pressurePa();
            helper.assertTrue(atmosphericPressurePa < HIGH_PRESSURE_PA, "Reverse-flow test requires the west source pressure to exceed atmospheric pressure");
            helper.assertValueEqual(valve.getThroughputFlowRate(), 0L, "reverse check-valve throughput");
            helper.assertTrue(valve.getFlowState(Direction.WEST) == null, "Reverse-biased check valve reported flow on its west output face");
            helper.assertTrue(valve.getFlowState(Direction.EAST) == null, "Reverse-biased check valve reported flow on its east input face");
        });
    }

    private static BlockState forwardValveState() {
        return CCBBlocks.AIRTIGHT_CHECK_VALVE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X).setValue(AirtightCheckValveBlock.INVERTED, false);
    }

    private static void assertForwardDirectionality(GameTestHelper helper, GasTransportBehaviour valve, BlockState state) {
        helper.assertTrue(valve.allowsInboundFlow(state, Direction.EAST), "Non-inverted X-axis check valve did not accept gas on its east input face");
        helper.assertTrue(!valve.allowsOutboundFlow(state, Direction.EAST), "Non-inverted X-axis check valve allowed outbound flow on its east input face");
        helper.assertTrue(!valve.allowsInboundFlow(state, Direction.WEST), "Non-inverted X-axis check valve allowed inbound flow on its west output face");
        helper.assertTrue(valve.allowsOutboundFlow(state, Direction.WEST), "Non-inverted X-axis check valve did not allow outbound flow on its west output face");
    }

    private static void placeSource(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, CCBBlocks.CREATIVE_AIRTIGHT_TANK_BLOCK.get().defaultBlockState());
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        helper.assertTrue(blockEntity instanceof CreativeAirtightTankBlockEntity, "Creative airtight tank block entity was not initialized at " + pos);
        if (!(blockEntity instanceof CreativeAirtightTankBlockEntity sourceTank)) {
            return;
        }

        sourceTank.getTankInventory().setFixedPressurePa(HIGH_PRESSURE_PA);
        sourceTank.getTankInventory().setContainedGas(new GasStack(CCBGases.NATURAL_AIR.get(), 1));
        helper.assertValueEqual(sourceTank.getTankInventory().getPressurePa(), HIGH_PRESSURE_PA, "source tank pressure at " + pos);
    }

    private static GasTransportBehaviour transport(GameTestHelper helper) {
        GasTransportBehaviour transport = BlockEntityBehaviour.get(helper.getLevel(), helper.absolutePos(VALVE_POS), GasTransportBehaviour.TYPE);
        helper.assertTrue(transport != null, "GasTransportBehaviour was not initialized at " + VALVE_POS);
        if (transport == null) {
            throw new NullPointerException("GasTransportBehaviour was not initialized at " + VALVE_POS + '.');
        }

        return transport;
    }
}
