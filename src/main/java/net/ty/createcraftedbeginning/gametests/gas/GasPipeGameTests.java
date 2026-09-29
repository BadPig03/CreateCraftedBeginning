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
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
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
public final class GasPipeGameTests {
    private static final BlockPos SOURCE_TANK_POS = new BlockPos(0, 1, 1);
    private static final BlockPos PIPE_POS = new BlockPos(1, 1, 1);
    private static final long HIGH_PRESSURE_PA = 2 * GasPressure.REFERENCE_PRESSURE_PA;

    private GasPipeGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 40)
    public static void equalAtmosphericPressureProducesNoFlow(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(PIPE_POS, CCBBlocks.AIRTIGHT_PIPE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X));

        helper.succeedWhen(() -> {
            GasTransportBehaviour transport = BlockEntityBehaviour.get(level, helper.absolutePos(PIPE_POS), GasTransportBehaviour.TYPE);
            helper.assertTrue(transport != null, "Airtight pipe transport behaviour was not initialized");
            if (transport == null) {
                throw new NullPointerException("Airtight pipe transport behaviour was not initialized.");
            }

            long atmosphericPressurePa = AtmosphereStateResolver.resolve(level, helper.absolutePos(PIPE_POS.relative(Direction.EAST))).pressurePa();
            helper.assertTrue(atmosphericPressurePa >= GasPressure.VACUUM_PA, "Atmospheric endpoint returned an invalid pressure");
            helper.assertValueEqual(transport.getThroughputFlowRate(), 0L, "pipe throughput at equal pressure");
        });
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 60)
    public static void highPressureSourceFlowsTowardAtmosphere(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.setBlock(SOURCE_TANK_POS, CCBBlocks.CREATIVE_AIRTIGHT_TANK_BLOCK.get().defaultBlockState());
        helper.setBlock(PIPE_POS, CCBBlocks.AIRTIGHT_PIPE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X));

        BlockEntity sourceBlockEntity = level.getBlockEntity(helper.absolutePos(SOURCE_TANK_POS));
        helper.assertTrue(sourceBlockEntity instanceof CreativeAirtightTankBlockEntity, "Creative airtight tank block entity was not initialized");
        if (!(sourceBlockEntity instanceof CreativeAirtightTankBlockEntity sourceTank)) {
            return;
        }

        sourceTank.getTankInventory().setFixedPressurePa(HIGH_PRESSURE_PA);
        sourceTank.getTankInventory().setContainedGas(new GasStack(CCBGases.NATURAL_AIR.get(), 1));
        helper.assertValueEqual(sourceTank.getTankInventory().getPressurePa(), HIGH_PRESSURE_PA, "source tank pressure");

        helper.succeedWhen(() -> {
            GasTransportBehaviour transport = BlockEntityBehaviour.get(level, helper.absolutePos(PIPE_POS), GasTransportBehaviour.TYPE);
            helper.assertTrue(transport != null, "Airtight pipe transport behaviour was not initialized");
            if (transport == null) {
                throw new NullPointerException("Airtight pipe transport behaviour was not initialized.");
            }

            FlowState inletFlow = transport.getFlowState(Direction.WEST);
            FlowState outletFlow = transport.getFlowState(Direction.EAST);
            helper.assertTrue(inletFlow != null, "High-pressure source did not produce inlet flow");
            if (inletFlow == null) {
                throw new NullPointerException("High-pressure source did not produce inlet flow.");
            }

            helper.assertTrue(outletFlow != null, "Pipe did not discharge toward atmosphere");
            if (outletFlow == null) {
                throw new NullPointerException("Pipe did not discharge toward atmosphere.");
            }

            helper.assertTrue(inletFlow.direction() == FlowDirection.INBOUND, "West face flow was not inbound");
            helper.assertTrue(outletFlow.direction() == FlowDirection.OUTBOUND, "East face flow was not outbound");
            helper.assertTrue(inletFlow.flowRate() > 0, "High-to-low pressure flow rate was not positive");
            helper.assertValueEqual(outletFlow.flowRate(), inletFlow.flowRate(), "straight pipe face flow rate");
            helper.assertValueEqual(transport.getThroughputFlowRate(), inletFlow.flowRate(), "pipe throughput");
            helper.assertTrue(inletFlow.gas().is(CCBGases.NATURAL_AIR.get()), "Inlet flow gas was not Natural Air");
            helper.assertTrue(outletFlow.gas().is(CCBGases.NATURAL_AIR.get()), "Outlet flow gas was not Natural Air");

            long atmosphericPressurePa = AtmosphereStateResolver.resolve(level, helper.absolutePos(PIPE_POS.relative(Direction.EAST))).pressurePa();
            helper.assertTrue(atmosphericPressurePa < HIGH_PRESSURE_PA, "High-pressure flow test did not create a source-to-atmosphere pressure gradient");
        });
    }
}
