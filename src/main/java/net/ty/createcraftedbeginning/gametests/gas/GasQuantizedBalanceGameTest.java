package net.ty.createcraftedbeginning.gametests.gas;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtightencasedpipe.AirtightEncasedPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.airtighttank.AirtightTankBlockEntity;
import net.ty.createcraftedbeginning.gas.atmosphere.AtmosphereStateResolver;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasQuantizedBalanceGameTest {
    private static final BlockPos MANIFOLD_POS = new BlockPos(1, 1, 1);
    private static final BlockPos WEST_TANK_POS = MANIFOLD_POS.relative(Direction.WEST);
    private static final BlockPos EAST_TANK_POS = MANIFOLD_POS.relative(Direction.EAST);
    private static final BlockPos SOUTH_TANK_POS = MANIFOLD_POS.relative(Direction.SOUTH);

    private static final long INITIAL_WEST_EXCESS_GU = 5;
    private static final long INITIAL_PARALLEL_ATMOSPHERE_EXCESS_GU = 8;
    private static final int REQUIRED_STABLE_TICKS = 12;

    private GasQuantizedBalanceGameTest() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 80)
    public static void oneGuBetweenOtherwiseEmptyTanksDoesNotOscillate(GameTestHelper helper) {
        assertSingleGuImbalanceStaysPut(helper, false);
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 80)
    public static void oneGuOfHeadroomBetweenFullTanksDoesNotOscillate(GameTestHelper helper) {
        assertSingleGuImbalanceStaysPut(helper, true);
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 160)
    public static void quantizedFiniteTankBalanceDoesNotOscillate(GameTestHelper helper) {
        AirtightTankBlockEntity westTank = placeFiniteTank(helper, WEST_TANK_POS);
        AirtightTankBlockEntity eastTank = placeFiniteTank(helper, EAST_TANK_POS);
        AirtightTankBlockEntity southTank = placeFiniteTank(helper, SOUTH_TANK_POS);
        helper.setBlock(MANIFOLD_POS, threeWayEncasedPipeState());

        long volume = westTank.getTankInventory().getVolume();
        helper.assertTrue(volume > 0, "Quantized-balance finite tank reported zero physical volume");
        helper.assertValueEqual(eastTank.getTankInventory().getVolume(), volume, "quantized-balance east tank volume");
        helper.assertValueEqual(southTank.getTankInventory().getVolume(), volume, "quantized-balance south tank volume");
        helper.assertTrue(westTank.getTankInventory().getMaxPressurePa() > GasPressure.REFERENCE_PRESSURE_PA, "Quantized-balance finite tank pressure rating was too low for the near-atmospheric setup");

        long initialWestAmount = volume + INITIAL_WEST_EXCESS_GU;
        long initialTotalAmount = initialWestAmount + volume * 2;
        helper.assertValueEqual(initialTotalAmount % 3, 2L, "quantized-balance total remainder");

        westTank.getTankInventory().tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), initialWestAmount)).requireAccepted();
        eastTank.getTankInventory().tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), volume)).requireAccepted();
        southTank.getTankInventory().tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), volume)).requireAccepted();

        helper.assertValueEqual(westTank.getTankInventory().getStoredAmount(), initialWestAmount, "initial quantized-balance west amount");
        helper.assertValueEqual(eastTank.getTankInventory().getStoredAmount(), volume, "initial quantized-balance east amount");
        helper.assertValueEqual(southTank.getTankInventory().getStoredAmount(), volume, "initial quantized-balance south amount");

        GasTransportBehaviour manifold = transport(helper);
        long[] stableSnapshot = {Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE};
        int[] stableTicks = {0};
        boolean[] transferObserved = {false};

        helper.succeedWhen(() -> {
            long westAmount = westTank.getTankInventory().getStoredAmount();
            long eastAmount = eastTank.getTankInventory().getStoredAmount();
            long southAmount = southTank.getTankInventory().getStoredAmount();

            helper.assertValueEqual(westAmount + eastAmount + southAmount, initialTotalAmount, "quantized-balance total stored gas conservation");
            helper.assertTrue(westTank.getTankInventory().getGasStack().is(CCBGases.NATURAL_AIR.get()), "Quantized-balance west tank gas changed away from Natural Air");
            helper.assertTrue(eastTank.getTankInventory().getGasStack().is(CCBGases.NATURAL_AIR.get()), "Quantized-balance east tank gas changed away from Natural Air");
            helper.assertTrue(southTank.getTankInventory().getGasStack().is(CCBGases.NATURAL_AIR.get()), "Quantized-balance south tank gas changed away from Natural Air");

            if (westAmount != initialWestAmount || eastAmount != volume || southAmount != volume) {
                transferObserved[0] = true;
            }

            long minimumAmount = Math.min(westAmount, Math.min(eastAmount, southAmount));
            long maximumAmount = Math.max(westAmount, Math.max(eastAmount, southAmount));
            long amountSpread = maximumAmount - minimumAmount;
            if (amountSpread > 1) {
                stableSnapshot[0] = Long.MIN_VALUE;
                stableSnapshot[1] = Long.MIN_VALUE;
                stableSnapshot[2] = Long.MIN_VALUE;
                stableTicks[0] = 0;
                helper.assertTrue(false, "Finite tanks have not yet reached whole-GU quantized balance: west=" + westAmount + ", east=" + eastAmount + ", south=" + southAmount + " GU");
                return;
            }

            helper.assertTrue(transferObserved[0], "Quantized-balance network reached its terminal condition without transferring gas");
            helper.assertValueEqual(amountSpread, 1L, "quantized-balance terminal amount spread");
            helper.assertTrue(westAmount < initialWestAmount, "Quantized-balance source tank did not lose gas");
            helper.assertTrue(eastAmount > volume, "Quantized-balance east tank did not receive gas");
            helper.assertTrue(southAmount > volume, "Quantized-balance south tank did not receive gas");

            boolean snapshotChanged = stableSnapshot[0] != westAmount || stableSnapshot[1] != eastAmount || stableSnapshot[2] != southAmount;
            if (snapshotChanged) {
                stableSnapshot[0] = westAmount;
                stableSnapshot[1] = eastAmount;
                stableSnapshot[2] = southAmount;
                stableTicks[0] = 0;
                helper.assertTrue(false, "Whole-GU balance was reached but has not remained stable yet");
                return;
            }

            helper.assertTrue(manifold.getFlowState(Direction.WEST) == null, "Quantized equilibrium kept moving gas through the west face");
            helper.assertTrue(manifold.getFlowState(Direction.EAST) == null, "Quantized equilibrium kept moving gas through the east face");
            helper.assertTrue(manifold.getFlowState(Direction.SOUTH) == null, "Quantized equilibrium kept moving gas through the south face");
            helper.assertValueEqual(manifold.getThroughputFlowRate(), 0L, "quantized-equilibrium manifold throughput");

            stableTicks[0]++;
            helper.assertTrue(stableTicks[0] >= REQUIRED_STABLE_TICKS, "Whole-GU balance has remained stable for only " + stableTicks[0] + '/' + REQUIRED_STABLE_TICKS + " ticks");
        });
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 240)
    public static void parallelFiniteTanksReachExactAtmosphericQuantizedBalance(GameTestHelper helper) {
        Direction atmosphereFace = Direction.EAST;
        Direction[] tankFaces = {Direction.WEST, Direction.NORTH, Direction.SOUTH, Direction.UP, Direction.DOWN};
        AirtightTankBlockEntity[] tanks = new AirtightTankBlockEntity[tankFaces.length];
        for (int index = 0; index < tankFaces.length; index++) {
            tanks[index] = placeFiniteTank(helper, MANIFOLD_POS.relative(tankFaces[index]));
        }
        helper.setBlock(MANIFOLD_POS, allWayEncasedPipeState());

        long volume = tanks[0].getTankInventory().getVolume();
        long atmosphericPressurePa = AtmosphereStateResolver.resolve(helper.getLevel(), helper.absolutePos(MANIFOLD_POS.relative(atmosphereFace))).pressurePa();
        long targetAmount = GasPressure.minimumAmountForPressure(volume, atmosphericPressurePa);
        helper.assertTrue(targetAmount > 0, "Parallel-atmosphere quantized target amount was not positive");

        long initialAmount = targetAmount + INITIAL_PARALLEL_ATMOSPHERE_EXCESS_GU;
        for (AirtightTankBlockEntity tank : tanks) {
            helper.assertValueEqual(tank.getTankInventory().getVolume(), volume, "parallel-atmosphere tank volume");
            tank.getTankInventory().tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), initialAmount)).requireAccepted();
        }

        long[] stableSnapshot = new long[tanks.length];
        Arrays.fill(stableSnapshot, Long.MIN_VALUE);
        int[] stableTicks = {0};
        helper.succeedWhen(() -> {
            for (int index = 0; index < tanks.length; index++) {
                long amount = tanks[index].getTankInventory().getStoredAmount();
                helper.assertTrue(amount >= targetAmount, "Parallel finite tank crossed below atmospheric equilibrium: tank=" + index + ", amount=" + amount + ", target=" + targetAmount + " GU");
                if (amount == targetAmount) {
                    continue;
                }

                stableTicks[0] = 0;
                helper.assertTrue(false, "Parallel finite tanks have not reached exact atmospheric balance: tank=" + index + ", amount=" + amount + ", target=" + targetAmount + " GU");
                return;
            }

            boolean changed = false;
            for (int index = 0; index < tanks.length; index++) {
                long amount = tanks[index].getTankInventory().getStoredAmount();
                if (stableSnapshot[index] == amount) {
                    continue;
                }

                stableSnapshot[index] = amount;
                changed = true;
            }
            if (changed) {
                stableTicks[0] = 0;
                helper.assertTrue(false, "Exact atmospheric balance was reached but has not remained stable yet");
                return;
            }

            GasTransportBehaviour manifold = transport(helper);
            helper.assertTrue(manifold.getFlowState(atmosphereFace) == null, "Atmospheric outlet kept flowing after exact quantized balance");
            helper.assertValueEqual(manifold.getThroughputFlowRate(), 0L, "parallel-atmosphere terminal manifold throughput");
            stableTicks[0]++;
            helper.assertTrue(stableTicks[0] >= REQUIRED_STABLE_TICKS, "Exact atmospheric balance has remained stable for only " + stableTicks[0] + '/' + REQUIRED_STABLE_TICKS + " ticks");
        });
    }

    private static void assertSingleGuImbalanceStaysPut(GameTestHelper helper, boolean nearlyFull) {
        AirtightTankBlockEntity west = placeFiniteTank(helper, WEST_TANK_POS);
        AirtightTankBlockEntity east = placeFiniteTank(helper, EAST_TANK_POS);
        helper.setBlock(MANIFOLD_POS, CCBBlocks.AIRTIGHT_PIPE_BLOCK.getDefaultState().setValue(RotatedPillarBlock.AXIS, Axis.X));
        long westAmount = nearlyFull ? west.getTankInventory().getMaxAmount() : 1;
        long eastAmount = westAmount - 1;
        west.getTankInventory().tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), westAmount)).requireAccepted();
        east.getTankInventory().tryReplaceContents(new GasStack(CCBGases.NATURAL_AIR.get(), eastAmount)).requireAccepted();
        int[] ticks = {0};
        long[] previousWest = {westAmount};
        int[] transfers = {0};
        helper.onEachTick(() -> {
            long currentWest = west.getTankInventory().getStoredAmount();
            long currentEast = east.getTankInventory().getStoredAmount();
            helper.assertValueEqual(currentWest + currentEast, westAmount + eastAmount, "indivisible equilibrium gas conservation");
            helper.assertTrue(currentWest == westAmount || currentWest == eastAmount, "Tank left the two nearest integer equilibrium states");
            if (currentWest != previousWest[0]) {
                previousWest[0] = currentWest;
                ticks[0] = 0;
                transfers[0]++;
            }
            helper.assertTrue(transfers[0] <= (nearlyFull ? 1 : 0), "Gas oscillated between the nearest integer equilibrium states");
            if (++ticks[0] >= 30) {
                helper.assertValueEqual(transport(helper).getThroughputFlowRate(), 0L, "indivisible equilibrium throughput");
                helper.succeed();
            }
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

    private static BlockState threeWayEncasedPipeState() {
        BlockState state = CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK.get().defaultBlockState();
        for (Direction face : new Direction[]{Direction.WEST, Direction.EAST, Direction.SOUTH}) {
            state = state.setValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(face), true);
        }
        return state;
    }

    private static BlockState allWayEncasedPipeState() {
        BlockState state = CCBBlocks.AIRTIGHT_ENCASED_PIPE_BLOCK.get().defaultBlockState();
        for (Direction face : Iterate.directions) {
            state = state.setValue(AirtightEncasedPipeBlock.PROPERTY_BY_DIRECTION.get(face), true);
        }
        return state;
    }

    private static GasTransportBehaviour transport(GameTestHelper helper) {
        GasTransportBehaviour transport = BlockEntityBehaviour.get(helper.getLevel(), helper.absolutePos(MANIFOLD_POS), GasTransportBehaviour.TYPE);
        helper.assertTrue(transport != null, "GasTransportBehaviour was not initialized at " + MANIFOLD_POS);
        if (transport == null) {
            throw new NullPointerException("GasTransportBehaviour was not initialized at " + MANIFOLD_POS + '.');
        }

        return transport;
    }
}
