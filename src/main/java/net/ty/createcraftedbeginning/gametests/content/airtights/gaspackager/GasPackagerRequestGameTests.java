package net.ty.createcraftedbeginning.gametests.content.airtights.gaspackager;

import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonItem;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.VirtualGasItems;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasPackagerRequestPlanner;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasPackagerRequestPlanner.GasRequestPlan;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasPackagerRequestProcessor;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasPackagerRequestProcessor.Deduction;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasPackagerRequestProcessor.Result;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;
import org.apache.commons.lang3.mutable.MutableBoolean;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasPackagerRequestGameTests {
    private GasPackagerRequestGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void planningStopsAtAddressBoundaryWithoutChangingRequests(GameTestHelper helper) {
        ItemStack token = VirtualGasItems.createVirtualItem(new GasStack(CCBGases.NATURAL_AIR.get(), 1));
        PackagingRequest first = PackagingRequest.create(token, 200, "first", 0, new MutableBoolean(true), 3, 10, null);
        PackagingRequest second = PackagingRequest.create(token, 200, "second", 0, new MutableBoolean(true), 0, 10, null);
        List<PackagingRequest> queue = new ArrayList<>(List.of(first, second));

        GasRequestPlan plan = GasPackagerRequestPlanner.planGasRequestBatch(queue, 350);
        helper.assertValueEqual(plan.requests().size(), 1, "Requests selected across address boundary");
        helper.assertValueEqual(plan.requests().getFirst().amount(), 200L, "Planned first request amount");
        helper.assertValueEqual(first.getCount(), 200, "Request quantity after planning");
        helper.assertValueEqual(first.packageCounter().intValue(), 3, "Package counter after planning");
        helper.assertValueEqual(queue.size(), 2, "Queue size after planning");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void partialPackagingCommitsOnlyTransferredGas(GameTestHelper helper) {
        GasStack gas = new GasStack(CCBGases.NATURAL_AIR.get(), 500);
        GasTank source = new GasTank(1000);
        source.tryReplaceContents(gas);
        ItemStack token = VirtualGasItems.createVirtualItem(gas);
        PackagingRequest first = PackagingRequest.create(token, 200, "destination", 0, new MutableBoolean(true), 0, 10, null);
        PackagingRequest second = PackagingRequest.create(token, 400, "destination", 0, new MutableBoolean(true), 0, 10, null);
        List<PackagingRequest> queue = new ArrayList<>(List.of(first, second));

        Result result = new GasPackagerRequestProcessor(queue, source).process(350);
        helper.assertTrue(result != null, "Partial request did not produce a package");
        if (result == null) {
            throw new NullPointerException("Partial request did not produce a package.");
        }

        ItemStack balloon = result.balloon();
        helper.assertValueEqual(BalloonItem.getGas(balloon).getAmount(), 350L, "Packaged gas");
        helper.assertValueEqual(source.getStoredAmount(), 150L, "Source gas after packaging");
        helper.assertValueEqual(queue.size(), 1, "Remaining request count");
        helper.assertValueEqual(second.getCount(), 250, "Remaining gas request");
        helper.assertValueEqual(second.packageCounter().intValue(), 1, "Next package index");
        helper.assertTrue(!PackageItem.isFinal(balloon), "Partial link was marked complete");
        helper.assertValueEqual(result.deductions().stream().mapToInt(Deduction::amount).sum(), 350, "Deducted network stock");
        helper.succeed();
    }
}
