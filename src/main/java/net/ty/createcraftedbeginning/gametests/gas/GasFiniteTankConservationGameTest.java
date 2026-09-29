package net.ty.createcraftedbeginning.gametests.gas;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
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
public final class GasFiniteTankConservationGameTest {
    private static final BlockPos SOURCE_TANK_POS = new BlockPos(0, 1, 1);
    private static final BlockPos PIPE_POS = new BlockPos(1, 1, 1);
    private static final BlockPos SINK_TANK_POS = new BlockPos(2, 1, 1);

    private static final long SOURCE_PRESSURE_PA = GasPressure.pascals(2);
    private static final long SINK_PRESSURE_PA = GasPressure.REFERENCE_PRESSURE_PA;

    private GasFiniteTankConservationGameTest() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 120)
    public static void finiteAirtightTanksConserveGasDuringPressureEqualization(GameTestHelper helper) {
        AirtightTankBlockEntity sourceTank = placeFiniteTank(helper, SOURCE_TANK_POS);
        AirtightTankBlockEntity sinkTank = placeFiniteTank(helper, SINK_TANK_POS);
        helper.setBlock(PIPE_POS, CCBBlocks.AIRTIGHT_PIPE_BLOCK.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis.X));

        long sourceVolume = sourceTank.getTankInventory().getVolume();
        long sinkVolume = sinkTank.getTankInventory().getVolume();
        helper.assertTrue(sourceVolume > 0, "Finite source tank reported zero physical volume");
        helper.assertTrue(sinkVolume > 0, "Finite sink tank reported zero physical volume");
        helper.assertTrue(sourceTank.getTankInventory().getMaxPressurePa() >= SOURCE_PRESSURE_PA, "Finite source tank pressure rating was below the 2 atm test pressure");
        helper.assertTrue(sinkTank.getTankInventory().getMaxPressurePa() >= SINK_PRESSURE_PA, "Finite sink tank pressure rating was below the 1 atm test pressure");

        long initialSourceAmount = GasPressure.amount(sourceVolume, SOURCE_PRESSURE_PA);
        long initialSinkAmount = GasPressure.amount(sinkVolume, SINK_PRESSURE_PA);
        long initialTotalAmount = initialSourceAmount + initialSinkAmount;
        sourceTank.getTankInventory().tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), initialSourceAmount)).requireAccepted();
        sinkTank.getTankInventory().tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), initialSinkAmount)).requireAccepted();

        helper.assertValueEqual(sourceTank.getTankInventory().getStoredAmount(), initialSourceAmount, "initial finite source amount");
        helper.assertValueEqual(sinkTank.getTankInventory().getStoredAmount(), initialSinkAmount, "initial finite sink amount");
        helper.assertValueEqual(sourceTank.getTankInventory().getPressurePa(), SOURCE_PRESSURE_PA, "initial finite source pressure");
        helper.assertValueEqual(sinkTank.getTankInventory().getPressurePa(), SINK_PRESSURE_PA, "initial finite sink pressure");

        helper.succeedWhen(() -> {
            long sourceAmount = sourceTank.getTankInventory().getStoredAmount();
            long sinkAmount = sinkTank.getTankInventory().getStoredAmount();
            long sourceLoss = initialSourceAmount - sourceAmount;
            long sinkGain = sinkAmount - initialSinkAmount;

            helper.assertTrue(sourceLoss > 0, "Finite source tank did not lose gas toward the lower-pressure sink");
            helper.assertTrue(sinkGain > 0, "Finite sink tank did not gain gas from the higher-pressure source");
            helper.assertValueEqual(sourceLoss, sinkGain, "finite tank transferred amount conservation");
            helper.assertValueEqual(sourceAmount + sinkAmount, initialTotalAmount, "finite tank total stored gas conservation");

            helper.assertTrue(sourceTank.getTankInventory().getPressurePa() < SOURCE_PRESSURE_PA, "Finite source pressure did not decrease after transferring gas");
            helper.assertTrue(sinkTank.getTankInventory().getPressurePa() > SINK_PRESSURE_PA, "Finite sink pressure did not increase after receiving gas");
            helper.assertTrue(sourceTank.getTankInventory().getGasStack().is(CCBGases.NATURAL_AIR.get()), "Finite source tank gas changed away from Natural Air");
            helper.assertTrue(sinkTank.getTankInventory().getGasStack().is(CCBGases.NATURAL_AIR.get()), "Finite sink tank gas changed away from Natural Air");

            GasTransportBehaviour transport = BlockEntityBehaviour.get(helper.getLevel(), helper.absolutePos(PIPE_POS), GasTransportBehaviour.TYPE);
            helper.assertTrue(transport != null, "Finite-tank conservation pipe transport behaviour was not initialized");
            if (transport == null) {
                throw new NullPointerException("Finite-tank conservation pipe transport behaviour was not initialized.");
            }

            FlowState inletFlow = transport.getFlowState(Direction.WEST);
            FlowState outletFlow = transport.getFlowState(Direction.EAST);
            helper.assertTrue(inletFlow != null, "Finite source did not create pipe inlet flow");
            if (inletFlow == null) {
                throw new NullPointerException("Finite source did not create pipe inlet flow.");
            }

            helper.assertTrue(outletFlow != null, "Finite sink did not receive pipe outlet flow");
            if (outletFlow == null) {
                throw new NullPointerException("Finite sink did not receive pipe outlet flow.");
            }

            helper.assertTrue(inletFlow.direction() == FlowDirection.INBOUND, "Finite-tank pipe west face was not inbound");
            helper.assertTrue(outletFlow.direction() == FlowDirection.OUTBOUND, "Finite-tank pipe east face was not outbound");
            helper.assertTrue(inletFlow.flowRate() > 0, "Finite-tank pressure equalization flow was not positive");
            helper.assertValueEqual(outletFlow.flowRate(), inletFlow.flowRate(), "finite-tank straight pipe face flow rate");
            helper.assertValueEqual(transport.getThroughputFlowRate(), inletFlow.flowRate(), "finite-tank pipe throughput");
            helper.assertTrue(inletFlow.gas().is(CCBGases.NATURAL_AIR.get()), "Finite-tank inlet flow gas was not Natural Air");
            helper.assertTrue(outletFlow.gas().is(CCBGases.NATURAL_AIR.get()), "Finite-tank outlet flow gas was not Natural Air");
        });
    }

    private static AirtightTankBlockEntity placeFiniteTank(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, CCBBlocks.AIRTIGHT_TANK_BLOCK.get().defaultBlockState());
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        helper.assertTrue(blockEntity instanceof AirtightTankBlockEntity, "Airtight tank block entity was not initialized at " + pos);
        if (!(blockEntity instanceof AirtightTankBlockEntity tank)) {
            throw new IllegalStateException("Airtight tank block entity missing at " + pos + '.');
        }

        return tank;
    }
}
