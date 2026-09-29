package net.ty.createcraftedbeginning.gametests.gas;

import net.createmod.catnip.math.BlockFace;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint.PressureState;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint.Recovery;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint.TransferAccess;
import net.ty.createcraftedbeginning.gas.network.solver.endpoint.GasNetworkPressureEndpoint.TransferLimits;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasAmountDistribution;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferExecutor;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferExecutor.PlannedDrain;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferExecutor.PlannedFill;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferExecutor.PooledTransferExecutionResult;
import net.ty.createcraftedbeginning.gas.storage.GasPressureCompartmentView;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.math.BigInteger;
import java.util.List;
import java.util.Random;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasNetworkTransferSafetyGameTests {
    private GasNetworkTransferSafetyGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void largeDistributionsPreserveExactTotals(GameTestHelper helper) {
        long[] proportional = GasAmountDistribution.allocateProportionally(100, new long[]{Long.MAX_VALUE, Long.MAX_VALUE});
        helper.assertValueEqual(proportional[0], 50L, "first large-capacity proportional share");
        helper.assertValueEqual(proportional[1], 50L, "second large-capacity proportional share");
        long[] weighted = GasAmountDistribution.allocateByWeight(Long.MAX_VALUE, new long[]{Long.MAX_VALUE, Long.MAX_VALUE}, new long[]{1, 1}, 1);
        helper.assertValueEqual(weighted[0], Long.MAX_VALUE / 2, "exact first half");
        helper.assertValueEqual(weighted[1], 4611686018427387904L, "cursor receives indivisible remainder");

        Random random = new Random(0x6A5);
        for (int sample = 0; sample < 500; sample++) {
            long[] limits = new long[1 + random.nextInt(8)];
            long[] weights = new long[limits.length];
            long[] weightedLimits = new long[limits.length];
            for (int index = 0; index < limits.length; index++) {
                limits[index] = sample % 2 == 0 ? random.nextLong() & Long.MAX_VALUE : random.nextInt(100);
                weights[index] = random.nextInt(5) == 0 ? 0 : random.nextLong() & Long.MAX_VALUE;
                weightedLimits[index] = weights[index] == 0 ? 0 : limits[index];
            }
            long total = sample % 3 == 0 ? Long.MAX_VALUE : random.nextLong() & Long.MAX_VALUE;
            assertAllocation(helper, total, limits, GasAmountDistribution.allocateProportionally(total, limits));
            assertAllocation(helper, total, weightedLimits, GasAmountDistribution.allocateByWeight(total, limits, weights, random.nextInt()));
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void oversizedFillPlansCannotCreateGas(GameTestHelper helper) {
        GasTank source = tank(100, 100);
        GasTank first = tank(Long.MAX_VALUE, 0);
        GasTank second = tank(Long.MAX_VALUE, 0);
        PooledTransferExecutionResult result = GasTransferExecutor.executePooledTransfer(gas(1), List.of(new PlannedDrain(source, 100)), List.of(new PlannedFill(first, Long.MAX_VALUE, -1), new PlannedFill(second, Long.MAX_VALUE, -1)));
        helper.assertValueEqual(first.getStoredAmount(), 50L, "first actual fill");
        helper.assertValueEqual(second.getStoredAmount(), 50L, "second actual fill");
        helper.assertValueEqual(source.getStoredAmount(), 0L, "drained source");
        assertExecutionConserved(helper, result);
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void overflowingDrainPlansLeaveExcessGasInItsSource(GameTestHelper helper) {
        GasTank first = tank(Long.MAX_VALUE, Long.MAX_VALUE);
        GasTank second = tank(Long.MAX_VALUE, Long.MAX_VALUE);
        GasTank target = tank(Long.MAX_VALUE, 0);
        PooledTransferExecutionResult result = GasTransferExecutor.executePooledTransfer(gas(1), List.of(new PlannedDrain(first, Long.MAX_VALUE), new PlannedDrain(second, Long.MAX_VALUE)), List.of(new PlannedFill(target, Long.MAX_VALUE, -1)));
        helper.assertValueEqual(first.getStoredAmount(), 0L, "first source drained");
        helper.assertValueEqual(second.getStoredAmount(), Long.MAX_VALUE, "unrepresentable pooled excess stays in its source");
        helper.assertValueEqual(target.getStoredAmount(), Long.MAX_VALUE, "bounded pooled fill");
        assertExecutionConserved(helper, result);
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void largeUnderfilledPlansKeepAnExactRecoverableRemainder(GameTestHelper helper) {
        GasTank source = tank(Long.MAX_VALUE, Long.MAX_VALUE);
        GasTank target = tank(7, 0);
        PooledTransferExecutionResult result = GasTransferExecutor.executePooledTransfer(gas(1), List.of(new PlannedDrain(source, Long.MAX_VALUE)), List.of(new PlannedFill(target, Long.MAX_VALUE, -1)));
        helper.assertValueEqual(target.getStoredAmount(), 7L, "underfilled target");
        helper.assertValueEqual(result.remainingGas().getAmount(), Long.MAX_VALUE - 7, "recoverable remainder");
        assertExecutionConserved(helper, result);
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void hypotheticalEndpointsRebuildEmptyAndFullTankAccess(GameTestHelper helper) {
        GasTank tank = tank(100, 0);
        List<BlockFace> faces = List.of(new BlockFace(BlockPos.ZERO, Direction.WEST));
        GasNetworkPressureEndpoint endpoint = new GasNetworkPressureEndpoint(new TransferAccess(null, tank, List.of(), faces), new PressureState(0, false, 0, 100, GasPressure.REFERENCE_PRESSURE_PA, 100), new TransferLimits(0, 100), Recovery.NONE);
        GasNetworkPressureEndpoint full = endpoint.withAmountDelta(gas(1), 100);
        helper.assertTrue(full != null, "Filled tank did not acquire its drain access");
        if (full == null) {
            throw new NullPointerException("Required full is missing.");
        }

        helper.assertTrue(full.transferLimits().drainLimit() == 100 && full.access().drainFaces().equals(faces), "Filled tank did not acquire its drain access");
        helper.assertTrue(full.transferLimits().fillLimit() == 0 && full.access().fillFaces().isEmpty(), "Full tank retained a fill edge");
        GasNetworkPressureEndpoint partial = full.withAmountDelta(gas(1), -1);
        helper.assertTrue(partial != null, "Draining a full tank did not restore fill access");
        if (partial == null) {
            throw new NullPointerException("Required partial is missing.");
        }

        helper.assertTrue(partial.transferLimits().fillLimit() == 1 && partial.access().fillFaces().equals(faces), "Draining a full tank did not restore fill access");
        helper.assertValueEqual(tank.getStoredAmount(), 0L, "hypothetical prediction leaves real storage untouched");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3")
    public static void unknownDirectionalAccessIsNotInventedDuringPrediction(GameTestHelper helper) {
        GasPressureCompartmentView restricted = new GasPressureCompartmentView(tank(100, 0)) {
            @Override
            public GasStack drain(GasStack resource, GasAction action) {
                return GasStack.EMPTY;
            }

            @Override
            public GasStack drain(long amount, GasAction action) {
                return GasStack.EMPTY;
            }
        };
        List<BlockFace> faces = List.of(new BlockFace(BlockPos.ZERO, Direction.WEST));
        GasNetworkPressureEndpoint endpoint = new GasNetworkPressureEndpoint(new TransferAccess(null, restricted, List.of(), faces), new PressureState(0, false, 0, 100, GasPressure.REFERENCE_PRESSURE_PA, 100), new TransferLimits(0, 100), Recovery.NONE);
        helper.assertTrue(endpoint.withAmountDelta(gas(1), 1) == null, "Unknown permissions were treated as a bidirectional tank");
        helper.succeed();
    }

    private static void assertAllocation(GameTestHelper helper, long total, long[] limits, long[] allocation) {
        BigInteger capacity = BigInteger.ZERO;
        BigInteger allocated = BigInteger.ZERO;
        for (int index = 0; index < limits.length; index++) {
            helper.assertTrue(allocation[index] >= 0 && allocation[index] <= limits[index], "Allocation exceeded an endpoint limit");
            capacity = capacity.add(BigInteger.valueOf(limits[index]));
            allocated = allocated.add(BigInteger.valueOf(allocation[index]));
        }
        helper.assertTrue(allocated.equals(capacity.min(BigInteger.valueOf(total))), "Allocation violated exact conservation: " + allocated + " / " + total);
    }

    private static void assertExecutionConserved(GameTestHelper helper, PooledTransferExecutionResult result) {
        BigInteger drained = BigInteger.ZERO;
        BigInteger filled = BigInteger.valueOf(result.remainingGas().getAmount());
        for (long amount : result.drainedAmounts()) {
            drained = drained.add(BigInteger.valueOf(amount));
        }
        for (long amount : result.filledAmounts()) {
            filled = filled.add(BigInteger.valueOf(amount));
        }
        helper.assertTrue(drained.equals(filled), "Filled gas plus recovery did not equal drained gas");
    }

    private static GasTank tank(long capacity, long amount) {
        GasTank tank = new GasTank(capacity, GasPressure.REFERENCE_PRESSURE_PA);
        tank.tryReplaceContents(gas(amount)).requireAccepted();
        return tank;
    }

    private static GasStack gas(long amount) {
        return new GasStack(CCBGases.NATURAL_AIR.get(), amount);
    }
}
