package net.ty.createcraftedbeginning.gametests.gas;

import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonFactory;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonItem;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonPackingLimits;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BalloonPackingGameTests {
    private BalloonPackingGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void localPackingLimitTracksAmbientPressure(GameTestHelper helper) {
        long baseAmount = BalloonPackingLimits.getBaseAmount();
        helper.assertTrue(baseAmount > 0, "balloon base amount must be positive");
        helper.assertValueEqual(BalloonPackingLimits.getLocalPackingLimit(GasPressure.REFERENCE_PRESSURE_PA), baseAmount, "1 atm packing limit");
        helper.assertValueEqual(BalloonPackingLimits.getLocalPackingLimit(GasPressure.pascals(0.4)), GasPressure.amount(baseAmount, GasPressure.pascals(0.4)), "0.4 atm packing limit");
        helper.assertValueEqual(BalloonPackingLimits.getLocalPackingLimit(GasPressure.VACUUM_PA), 0L, "vacuum packing limit");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void factoryWritesGasAddressAndOrderMetadata(GameTestHelper helper) {
        PackageOrderWithCrafts orderContext = PackageOrderWithCrafts.simple(List.of(new BigItemStack(Items.IRON_INGOT.getDefaultInstance(), 3)));
        ItemStack balloon = BalloonFactory.createOrdered(naturalAir(2500), "Factory A", 37, 2, false, 5, true, orderContext);

        helper.assertTrue(!balloon.isEmpty(), "BalloonFactory returned an empty ordered balloon");
        helper.assertTrue(GasStack.matches(BalloonItem.getGas(balloon), naturalAir(2500)), "BalloonFactory did not write the requested gas");
        assertLogisticsMetadata(helper, balloon, "Factory A", 37, 2, false, 5, true);
        helper.assertTrue(PackageItem.getOrderContext(balloon) != null, "BalloonFactory dropped order context");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void changingRemainderAmountKeepsLogisticsMetadata(GameTestHelper helper) {
        PackageOrderWithCrafts orderContext = PackageOrderWithCrafts.simple(List.of(new BigItemStack(Items.COPPER_INGOT.getDefaultInstance(), 4)));
        ItemStack balloon = BalloonFactory.createOrdered(naturalAir(10000), "Endpoint", 81, 1, true, 3, false, orderContext);
        ItemStack remainder = balloon.copyWithCount(1);

        BalloonItem.setGas(remainder, naturalAir(6000));

        helper.assertTrue(GasStack.matches(BalloonItem.getGas(remainder), naturalAir(6000)), "partial remainder did not keep the remaining gas");
        assertLogisticsMetadata(helper, remainder, "Endpoint", 81, 1, true, 3, false);
        helper.assertTrue(PackageItem.getOrderContext(remainder) != null, "partial remainder dropped order context");
        helper.succeed();
    }

    private static void assertLogisticsMetadata(GameTestHelper helper, ItemStack balloon, String address, int orderId, int linkIndex, boolean finalLink, int packageIndex, boolean finalPackage) {
        helper.assertValueEqual(PackageItem.getAddress(balloon), address, "package address");
        helper.assertValueEqual(PackageItem.getOrderId(balloon), orderId, "package orderId");
        helper.assertValueEqual(PackageItem.getLinkIndex(balloon), linkIndex, "package linkIndex");
        helper.assertTrue(PackageItem.isFinalLink(balloon) == finalLink, "package finalLink");
        helper.assertValueEqual(PackageItem.getIndex(balloon), packageIndex, "packageIndex");
        helper.assertTrue(PackageItem.isFinal(balloon) == finalPackage, "package finalPackage");
    }

    private static GasStack naturalAir(long amount) {
        return new GasStack(CCBGases.NATURAL_AIR.get(), amount);
    }
}
