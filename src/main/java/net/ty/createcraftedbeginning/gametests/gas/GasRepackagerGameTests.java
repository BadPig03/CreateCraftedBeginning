package net.ty.createcraftedbeginning.gametests.gas;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonFactory;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonItem;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonPackingLimits;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerOutputs;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerPlanner;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerScan.Candidate;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasRepackagerGameTests {
    private static final long LOW_PRESSURE = GasPressure.pascals(0.4);

    private GasRepackagerGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void oneAtmPackageSplitsAtLowPressureAndConservesGas(GameTestHelper helper) {
        long inputAmount = BalloonPackingLimits.getBaseAmount();
        long localLimit = BalloonPackingLimits.getLocalPackingLimit(LOW_PRESSURE);
        List<BigItemStack> output = GasRepackagerOutputs.createBalloons(naturalAir(inputAmount), "End Depot", LOW_PRESSURE);

        helper.assertTrue(localLimit > 0 && localLimit < inputAmount, "0.4 atm local packing limit was not below the 1 atm base amount");
        helper.assertValueEqual(output.size(), expectedPackageCount(inputAmount, localLimit), "low-pressure repack package count");
        assertGasPackages(helper, output, CCBGases.NATURAL_AIR.get(), "End Depot", localLimit, inputAmount);
        assertCanonicalChunkSizes(helper, output, inputAmount, localLimit);
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void lowPressurePackagesMergeAtOneAtmosphereAndConserveGas(GameTestHelper helper) {
        long totalAmount = BalloonPackingLimits.getBaseAmount();
        long lowPressureLimit = BalloonPackingLimits.getLocalPackingLimit(LOW_PRESSURE);
        List<BigItemStack> lowPressurePackages = GasRepackagerOutputs.createBalloons(naturalAir(totalAmount), "Overworld Depot", LOW_PRESSURE);
        long collectedAmount = 0;
        for (BigItemStack output : lowPressurePackages) {
            collectedAmount += BalloonItem.getGas(output.stack).getAmount();
        }

        List<BigItemStack> merged = GasRepackagerOutputs.createBalloons(naturalAir(collectedAmount), "Overworld Depot", GasPressure.REFERENCE_PRESSURE_PA);

        helper.assertTrue(lowPressurePackages.size() > 1, "0.4 atm source did not produce multiple packages");
        helper.assertValueEqual(collectedAmount, totalAmount, "gas amount before 0.4 atm to 1 atm merge");
        helper.assertValueEqual(merged.size(), 1, "1 atm repack did not consolidate the low-pressure packages");
        assertGasPackages(helper, merged, CCBGases.NATURAL_AIR.get(), "Overworld Depot", BalloonPackingLimits.getLocalPackingLimit(GasPressure.REFERENCE_PRESSURE_PA), totalAmount);
        helper.assertTrue(lowPressureLimit > 0, "0.4 atm local packing limit must be positive");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void vacuumProducesNoReplacementBalloons(GameTestHelper helper) {
        List<BigItemStack> output = GasRepackagerOutputs.createBalloons(naturalAir(BalloonPackingLimits.getBaseAmount()), "Vacuum", GasPressure.VACUUM_PA);
        helper.assertTrue(output.isEmpty(), "repackager created replacement balloons in vacuum");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void orderedBalloonRoundTripKeepsAddressAndValidOrderSequence(GameTestHelper helper) {
        long inputAmount = BalloonPackingLimits.getBaseAmount();
        int orderId = 24680;
        String address = "Assembly Hall";
        PackageOrderWithCrafts orderContext = PackageOrderWithCrafts.simple(List.of(new BigItemStack(Items.IRON_INGOT.getDefaultInstance(), 2)));
        ItemStack input = BalloonFactory.createOrdered(naturalAir(inputAmount), address, orderId, 0, true, 0, true, orderContext);

        List<Candidate> initialOrder = List.of(new Candidate(0, input, BalloonItem.getGas(input)));
        List<BigItemStack> lowPressureOutput = GasRepackagerOutputs.createMixedOrderOutput(orderId, initialOrder, LOW_PRESSURE, RandomSource.create(1));
        long lowPressureLimit = BalloonPackingLimits.getLocalPackingLimit(LOW_PRESSURE);
        helper.assertValueEqual(lowPressureOutput.size(), expectedPackageCount(inputAmount, lowPressureLimit), "ordered low-pressure split count");
        assertOrderedSequence(helper, lowPressureOutput, orderId, address, lowPressureLimit, inputAmount);

        List<Candidate> lowPressureCandidates = toCandidates(lowPressureOutput);
        helper.assertTrue(GasRepackagerPlanner.isOrderComplete(lowPressureCandidates), "low-pressure repack output was not a complete Create order");

        List<BigItemStack> oneAtmosphereOutput = GasRepackagerOutputs.createMixedOrderOutput(orderId, lowPressureCandidates, GasPressure.REFERENCE_PRESSURE_PA, RandomSource.create(2));
        helper.assertValueEqual(oneAtmosphereOutput.size(), 1, "ordered 0.4 atm packages did not merge back to one 1 atm balloon");
        assertOrderedSequence(helper, oneAtmosphereOutput, orderId, address, BalloonPackingLimits.getLocalPackingLimit(GasPressure.REFERENCE_PRESSURE_PA), inputAmount);
        helper.assertTrue(GasRepackagerPlanner.isOrderComplete(toCandidates(oneAtmosphereOutput)), "1 atm merged output was not a complete Create order");
        helper.succeed();
    }

    private static void assertGasPackages(GameTestHelper helper, List<BigItemStack> packages, Gas expectedGas, String expectedAddress, long localLimit, long expectedTotal) {
        long total = 0;
        for (BigItemStack output : packages) {
            GasStack gas = BalloonItem.getGas(output.stack);
            helper.assertTrue(!gas.isEmpty() && gas.is(expectedGas), "repackager changed the gas identity");
            helper.assertTrue(gas.getAmount() > 0 && gas.getAmount() <= localLimit, "repacked balloon exceeded the local packing limit");
            helper.assertValueEqual(PackageItem.getAddress(output.stack), expectedAddress, "repacked balloon address");
            total += gas.getAmount();
        }
        helper.assertValueEqual(total, expectedTotal, "repacked gas total");
    }

    private static void assertOrderedSequence(GameTestHelper helper, List<BigItemStack> packages, int orderId, String address, long localLimit, long expectedTotal) {
        long total = 0;
        for (int index = 0; index < packages.size(); index++) {
            ItemStack stack = packages.get(index).stack;
            GasStack gas = BalloonItem.getGas(stack);
            boolean finalPackage = index == packages.size() - 1;

            helper.assertTrue(!gas.isEmpty() && gas.is(CCBGases.NATURAL_AIR.get()), "ordered repack changed gas identity");
            helper.assertTrue(gas.getAmount() > 0 && gas.getAmount() <= localLimit, "ordered repack exceeded local packing limit");
            helper.assertValueEqual(PackageItem.getAddress(stack), address, "ordered repack address");
            helper.assertValueEqual(PackageItem.getOrderId(stack), orderId, "ordered repack orderId");
            helper.assertValueEqual(PackageItem.getLinkIndex(stack), 0, "ordered repack linkIndex");
            helper.assertTrue(PackageItem.isFinalLink(stack), "ordered repack finalLink");
            helper.assertValueEqual(PackageItem.getIndex(stack), index, "ordered repack packageIndex");
            helper.assertTrue(PackageItem.isFinal(stack) == finalPackage, "ordered repack finalPackage");
            if (finalPackage) {
                helper.assertTrue(PackageItem.getOrderContext(stack) != null, "final ordered repack package lost order context");
            }
            else {
                helper.assertTrue(PackageItem.getOrderContext(stack) == null, "non-final ordered repack package retained order context");
            }
            total += gas.getAmount();
        }
        helper.assertValueEqual(total, expectedTotal, "ordered repack gas total");
    }

    private static void assertCanonicalChunkSizes(GameTestHelper helper, List<BigItemStack> packages, long totalAmount, long localLimit) {
        long remainingAmount = totalAmount;
        for (BigItemStack output : packages) {
            long expectedAmount = Math.min(remainingAmount, localLimit);
            helper.assertValueEqual(BalloonItem.getGas(output.stack).getAmount(), expectedAmount, "canonical repack amount");
            remainingAmount -= expectedAmount;
        }
        helper.assertValueEqual(remainingAmount, 0L, "canonical repack remainder");
    }

    private static List<Candidate> toCandidates(List<BigItemStack> packages) {
        List<Candidate> candidates = new ArrayList<>(packages.size());
        for (int index = 0; index < packages.size(); index++) {
            ItemStack stack = packages.get(index).stack;
            candidates.add(new Candidate(index, stack, BalloonItem.getGas(stack)));
        }
        return List.copyOf(candidates);
    }

    private static int expectedPackageCount(long amount, long limit) {
        if (limit <= 0) {
            return 0;
        }

        if (amount % limit == 0) {
            return Math.toIntExact(amount / limit);
        }

        return Math.toIntExact(amount / limit + 1);
    }

    private static GasStack naturalAir(long amount) {
        return new GasStack(CCBGases.NATURAL_AIR.get(), amount);
    }
}
