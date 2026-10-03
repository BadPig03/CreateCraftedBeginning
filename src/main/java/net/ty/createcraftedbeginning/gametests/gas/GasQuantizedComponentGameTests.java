package net.ty.createcraftedbeginning.gametests.gas;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtightencasedpipe.AirtightEncasedPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.content.airtights.airtightvalve.AirtightValveBlock;
import net.ty.createcraftedbeginning.content.airtights.creativeairtighttank.CreativeAirtightTankBlockEntity;
import net.ty.createcraftedbeginning.gas.atmosphere.AtmosphereStateResolver;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.gas.network.GasConnectionResolver;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkSimulator;
import net.ty.createcraftedbeginning.gas.network.solver.GasNetworkTopology;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasQuantizedComponentGameTests {
    private static final int REQUIRED_STABLE_TICKS = 12;

    private GasQuantizedComponentGameTests() {
    }

    @GameTest(template = "gametest/empty_8x3x7")
    public static void quantizedExcessReachesBalanceBesideContinuousFlow(GameTestHelper helper) {
        assertIndependentQuantizedBalance(helper, 1);
    }

    @GameTest(template = "gametest/empty_8x3x7")
    public static void quantizedDeficitReachesBalanceBesideContinuousFlow(GameTestHelper helper) {
        assertIndependentQuantizedBalance(helper, -1);
    }

    @GameTest(template = "gametest/empty_8x3x7")
    public static void quantizedComponentsTakeTurnsWithoutStoppingContinuousFlow(GameTestHelper helper) {
        assertIndependentQuantizedBalance(helper, 1, -1);
    }

    private static void assertIndependentQuantizedBalance(GameTestHelper helper, long... initialOffsets) {
        ServerLevel level = helper.getLevel();
        BlockPos flowingPipePos = new BlockPos(4, 1, 3);
        BlockPos absoluteFlowingPipePos = helper.absolutePos(flowingPipePos);
        placeBoundaryTank(helper, flowingPipePos.west(), GasPressure.pascals(2));
        placeBoundaryTank(helper, flowingPipePos.east(), GasPressure.REFERENCE_PRESSURE_PA);
        Direction[] flowingFaces = initialOffsets.length == 1 ? new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH} : new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH};
        helper.setBlock(flowingPipePos, encasedPipe(flowingFaces));

        AirtightTankBlockEntity[] tanks = new AirtightTankBlockEntity[initialOffsets.length];
        BlockPos[] pipePositions = new BlockPos[initialOffsets.length];
        long[] targetAmounts = new long[initialOffsets.length];
        for (int index = 0; index < initialOffsets.length; index++) {
            Direction branchDirection = index == 0 ? Direction.NORTH : Direction.SOUTH;
            BlockPos pipePos = flowingPipePos.relative(branchDirection, 2);
            BlockPos tankPos = pipePos.east();
            helper.setBlock(tankPos, CCBBlocks.AIRTIGHT_TANK_BLOCK.getDefaultState());
            BlockEntity entity = level.getBlockEntity(helper.absolutePos(tankPos));
            if (entity == null) {
                throw new NullPointerException("Quantized component tank was not initialized at " + tankPos + '.');
            }

            AirtightTankBlockEntity tank = (AirtightTankBlockEntity) entity;
            long atmospherePressurePa = AtmosphereStateResolver.resolve(level, helper.absolutePos(pipePos.west())).pressurePa();
            long targetAmount = GasPressure.minimumAmountForPressure(tank.getTankInventory().getVolume(), atmospherePressurePa);
            tank.getTankInventory().tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), targetAmount + initialOffsets[index])).requireAccepted();
            tanks[index] = tank;
            pipePositions[index] = helper.absolutePos(pipePos);
            targetAmounts[index] = targetAmount;
            helper.setBlock(pipePos, encasedPipe(Direction.WEST, Direction.EAST, branchDirection.getOpposite()));
            helper.setBlock(flowingPipePos.relative(branchDirection), CCBBlocks.AIRTIGHT_VALVE_BLOCK.getDefaultState().setValue(AirtightValveBlock.AXIS, Axis.Z).setValue(AirtightValveBlock.OPEN, false));
        }

        GasTransportBehaviour flowingTransport = GasConnectionResolver.getTransportBehaviour(level, absoluteFlowingPipePos);
        if (flowingTransport == null) {
            throw new NullPointerException("Continuous-flow transport was not initialized at " + absoluteFlowingPipePos + '.');
        }

        helper.onEachTick(() -> {
            GasNetworkSimulator.simulateTick(level, absoluteFlowingPipePos);
            helper.assertTrue(flowingTransport.getThroughputFlowRate() > 0, "Quantized work stopped the continuously flowing component");
            for (int index = 0; index < tanks.length; index++) {
                helper.assertTrue(GasNetworkTopology.get(level, absoluteFlowingPipePos).pipePositions().contains(pipePositions[index]), "Closed valve separated the physical topology for component " + index);
                long amount = tanks[index].getTankInventory().getStoredAmount();
                long targetAmount = targetAmounts[index];
                helper.assertTrue(amount == targetAmount || amount == targetAmount + initialOffsets[index], "Quantized component crossed its integer equilibrium: component=" + index + ", amount=" + amount + ", target=" + targetAmount);
            }
        });

        int[] stableTicks = {0};
        helper.succeedWhen(() -> {
            for (int index = 0; index < tanks.length; index++) {
                long amount = tanks[index].getTankInventory().getStoredAmount();
                if (amount == targetAmounts[index]) {
                    continue;
                }

                stableTicks[0] = 0;
                helper.assertValueEqual(amount, targetAmounts[index], "Quantized balance beside continuous flow for component " + index);
            }
            stableTicks[0]++;
            helper.assertTrue(stableTicks[0] >= REQUIRED_STABLE_TICKS, "Quantized component balance has remained stable for only " + stableTicks[0] + '/' + REQUIRED_STABLE_TICKS + " ticks");
        });
    }

    private static BlockState encasedPipe(Direction... faces) {
        BlockState state = CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK.getDefaultState();
        List<Direction> connectedFaces = List.of(faces);
        for (Direction direction : Iterate.directions) {
            state = state.setValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(direction), connectedFaces.contains(direction));
        }
        return state;
    }

    private static void placeBoundaryTank(GameTestHelper helper, BlockPos pos, long pressurePa) {
        helper.setBlock(pos, CCBBlocks.CREATIVE_AIRTIGHT_TANK_BLOCK.getDefaultState());
        BlockEntity entity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        if (entity == null) {
            throw new NullPointerException("Continuous-flow creative tank was not initialized at " + pos + '.');
        }

        CreativeAirtightTankBlockEntity tank = (CreativeAirtightTankBlockEntity) entity;
        tank.getTankInventory().setFixedPressurePa(pressurePa);
        tank.getTankInventory().setContainedGas(new GasStack(CCBGases.NATURAL_AIR.get(), 1));
    }
}
