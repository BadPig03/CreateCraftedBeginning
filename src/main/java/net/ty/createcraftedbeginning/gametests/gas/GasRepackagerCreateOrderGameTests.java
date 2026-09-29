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
import net.neoforged.neoforge.items.ItemStackHandler;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonFactory;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonItem;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonPackingLimits;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerOutputs;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerPlanner;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerScan.Candidate;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasRepackagerCreateOrderGameTests {
    private static final long LOW_PRESSURE = GasPressure.pascals(0.4);

    private GasRepackagerCreateOrderGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void multiLinkMixedOrderRepackagesToFinalAddressAndSingleFinalLink(GameTestHelper helper) {
        int orderId = 61234;
        String firstAddress = "Warehouse A";
        String finalAddress = "Assembly Hall";
        long halfLocalPackage = BalloonPackingLimits.getLocalPackingLimit(LOW_PRESSURE) / 2;
        PackageOrderWithCrafts orderContext = PackageOrderWithCrafts.simple(List.of(new BigItemStack(Items.IRON_INGOT.getDefaultInstance(), 3), new BigItemStack(Items.GOLD_INGOT.getDefaultInstance(), 2)));

        ItemStack itemPackage = PackageItem.containing(List.of(Items.IRON_INGOT.getDefaultInstance().copyWithCount(3)));
        PackageItem.addAddress(itemPackage, firstAddress);
        PackageItem.setOrder(itemPackage, orderId, 0, false, 0, false, null);
        ItemStack firstGas = BalloonFactory.createOrdered(naturalAir(halfLocalPackage), firstAddress, orderId, 0, false, 1, true, null);
        ItemStack finalGas = BalloonFactory.createOrdered(naturalAir(halfLocalPackage), finalAddress, orderId, 1, true, 0, true, orderContext);

        List<Candidate> input = List.of(candidate(7, finalGas), candidate(2, itemPackage), candidate(5, firstGas));
        helper.assertTrue(GasRepackagerPlanner.isOrderComplete(input), "multi-link mixed input was not recognized as a complete Create order");

        List<BigItemStack> output = GasRepackagerOutputs.createMixedOrderOutput(orderId, input, LOW_PRESSURE, RandomSource.create(61234));
        helper.assertValueEqual(output.size(), 2, "mixed order output package count");
        assertOuterOrder(helper, output, orderId, finalAddress, orderContext);
        helper.assertTrue(GasRepackagerPlanner.isOrderComplete(toCandidates(output)), "mixed order output was not a complete flattened Create order");

        ItemStack itemOutput = output.getFirst().stack;
        ItemStack gasOutput = output.getLast().stack;
        helper.assertTrue(BalloonItem.getGas(itemOutput).isEmpty(), "item fragment was converted into a gas balloon");
        helper.assertValueEqual(countItem(itemOutput, Items.IRON_INGOT.getDefaultInstance()), 3, "mixed order item contents");
        GasStack outputGas = BalloonItem.getGas(gasOutput);
        helper.assertTrue(outputGas.is(CCBGases.NATURAL_AIR.get()), "mixed order changed gas identity");
        helper.assertValueEqual(outputGas.getAmount(), halfLocalPackage * 2, "gas from different links was not merged at the final link");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void multiLinkOrderKeepsGasIdentitiesSeparateAtFinalAddress(GameTestHelper helper) {
        int orderId = 62345;
        String firstAddress = "Gas Depot";
        String finalAddress = "Research Lab";
        long localLimit = BalloonPackingLimits.getLocalPackingLimit(LOW_PRESSURE);
        long naturalPerLink = localLimit / 2;
        long steamAmount = Math.max(1, localLimit / 3);
        PackageOrderWithCrafts orderContext = PackageOrderWithCrafts.simple(List.of(new BigItemStack(Items.COPPER_INGOT.getDefaultInstance(), 1)));

        ItemStack firstNatural = BalloonFactory.createOrdered(naturalAir(naturalPerLink), firstAddress, orderId, 0, false, 0, true, null);
        ItemStack steam = BalloonFactory.createOrdered(new GasStack(CCBGases.STEAM.get(), steamAmount), finalAddress, orderId, 1, true, 0, false, null);
        ItemStack finalNatural = BalloonFactory.createOrdered(naturalAir(naturalPerLink), finalAddress, orderId, 1, true, 1, true, orderContext);
        List<Candidate> input = List.of(candidate(4, finalNatural), candidate(1, firstNatural), candidate(3, steam));

        helper.assertTrue(GasRepackagerPlanner.isOrderComplete(input), "multi-link multi-gas input was not recognized as complete");
        List<BigItemStack> output = GasRepackagerOutputs.createMixedOrderOutput(orderId, input, LOW_PRESSURE, RandomSource.create(62345));
        helper.assertValueEqual(output.size(), 2, "multi-gas order output package count");
        assertOuterOrder(helper, output, orderId, finalAddress, orderContext);
        helper.assertTrue(GasRepackagerPlanner.isOrderComplete(toCandidates(output)), "multi-gas output was not a complete flattened Create order");

        long naturalAmount = 0;
        long repackedSteamAmount = 0;
        for (BigItemStack packageStack : output) {
            GasStack gas = BalloonItem.getGas(packageStack.stack);
            if (gas.is(CCBGases.NATURAL_AIR.get())) {
                naturalAmount += gas.getAmount();
                continue;
            }

            if (gas.is(CCBGases.STEAM.get())) {
                repackedSteamAmount += gas.getAmount();
                continue;
            }

            helper.fail("multi-gas order produced an unexpected gas identity");
            return;
        }

        helper.assertValueEqual(naturalAmount, naturalPerLink * 2, "natural air total after multi-link repack");
        helper.assertValueEqual(repackedSteamAmount, steamAmount, "steam total after multi-link repack");
        helper.succeed();
    }

    private static void assertOuterOrder(GameTestHelper helper, List<BigItemStack> packages, int orderId, String finalAddress, PackageOrderWithCrafts expectedContext) {
        for (int index = 0; index < packages.size(); index++) {
            ItemStack stack = packages.get(index).stack;
            boolean finalPackage = index == packages.size() - 1;
            helper.assertValueEqual(PackageItem.getAddress(stack), finalAddress, "flattened order address");
            helper.assertValueEqual(PackageItem.getOrderId(stack), orderId, "flattened order orderId");
            helper.assertValueEqual(PackageItem.getLinkIndex(stack), 0, "flattened order linkIndex");
            helper.assertTrue(PackageItem.isFinalLink(stack), "flattened order finalLink");
            helper.assertValueEqual(PackageItem.getIndex(stack), index, "flattened order packageIndex");
            helper.assertTrue(PackageItem.isFinal(stack) == finalPackage, "flattened order finalPackage");
            PackageOrderWithCrafts actualContext = PackageItem.getOrderContext(stack);
            if (finalPackage) {
                assertOrderContext(helper, actualContext, expectedContext);
                continue;
            }

            helper.assertTrue(actualContext == null, "non-final flattened package retained order context");
        }
    }

    private static void assertOrderContext(GameTestHelper helper, @Nullable PackageOrderWithCrafts actual, PackageOrderWithCrafts expected) {
        helper.assertTrue(actual != null, "final flattened package lost order context");
        if (actual == null) {
            throw new NullPointerException("Final flattened package lost order context.");
        }

        helper.assertValueEqual(actual.stacks().size(), expected.stacks().size(), "order context stack count");
        for (int index = 0; index < expected.stacks().size(); index++) {
            BigItemStack actualStack = actual.stacks().get(index);
            BigItemStack expectedStack = expected.stacks().get(index);
            helper.assertTrue(ItemStack.isSameItemSameComponents(actualStack.stack, expectedStack.stack), "order context item identity changed");
            helper.assertValueEqual(actualStack.count, expectedStack.count, "order context item count");
        }
    }

    private static int countItem(ItemStack packageStack, ItemStack expected) {
        ItemStackHandler contents = PackageItem.getContents(packageStack);
        int count = 0;
        for (int slot = 0; slot < contents.getSlots(); slot++) {
            ItemStack stack = contents.getStackInSlot(slot);
            if (!ItemStack.isSameItemSameComponents(stack, expected)) {
                continue;
            }

            count += stack.getCount();
        }
        return count;
    }

    private static Candidate candidate(int slot, ItemStack stack) {
        return new Candidate(slot, stack, BalloonItem.getGas(stack));
    }

    private static List<Candidate> toCandidates(List<BigItemStack> packages) {
        List<Candidate> candidates = new ArrayList<>(packages.size());
        for (int index = 0; index < packages.size(); index++) {
            candidates.add(candidate(index, packages.get(index).stack));
        }
        return List.copyOf(candidates);
    }

    private static GasStack naturalAir(long amount) {
        return new GasStack(CCBGases.NATURAL_AIR.get(), amount);
    }
}
