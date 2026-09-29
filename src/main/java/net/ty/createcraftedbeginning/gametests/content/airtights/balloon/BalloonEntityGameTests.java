package net.ty.createcraftedbeginning.gametests.content.airtights.balloon;

import com.simibubi.create.content.logistics.box.PackageEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonEntityBehaviour;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonItem;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonPackingLimits;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonStyles;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonWorldPhysics;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BalloonEntityGameTests {
    private BalloonEntityGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void gasDataCopiesAndClearsWithoutTouchingOrdinaryItems(GameTestHelper helper) {
        ItemStack balloon = BalloonStyles.createDefaultBalloon();
        GasStack gas = new GasStack(CCBGases.NATURAL_AIR.get(), 100);
        BalloonItem.setGas(balloon, gas);
        gas.setAmount(200);
        GasStack copy = BalloonItem.getGas(balloon);
        helper.assertValueEqual(copy.getAmount(), 100L, "Stored gas aliases caller data");
        copy.setAmount(300);
        helper.assertValueEqual(BalloonItem.getGas(balloon).getAmount(), 100L, "Returned gas aliases stored data");
        BalloonItem.setGas(balloon, GasStack.EMPTY);
        helper.assertTrue(!BalloonItem.containsGas(balloon), "Empty gas did not clear the component");
        ItemStack ordinary = new ItemStack(Items.STONE);
        BalloonItem.setGas(ordinary, gas);
        helper.assertTrue(BalloonItem.getGas(ordinary).isEmpty(), "Ordinary item gained balloon gas");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void buoyancyPreservesThrownSpeedAndSkipsPassengers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        PackageEntity balloon = balloon(helper);
        BalloonWorldPhysics physics = BalloonWorldPhysics.of(balloon.getBox(), level, balloon.blockPosition());
        balloon.setDeltaMovement(0.2, 0, 0.3);
        BalloonEntityBehaviour.tick(balloon, physics);
        helper.assertTrue(balloon.getDeltaMovement().y > 0, "Filled balloon did not rise");
        helper.assertValueEqual(balloon.getDeltaMovement().x, 0.2, "Air horizontal motion");
        balloon.setDeltaMovement(0, 0.8, 0);
        BalloonEntityBehaviour.tick(balloon, physics);
        helper.assertValueEqual(balloon.getDeltaMovement().y, 0.8, "Thrown upward speed was clamped");
        Boat boat = new Boat(level, balloon.getX(), balloon.getY(), balloon.getZ());
        helper.assertTrue(balloon.startRiding(boat, true), "Could not create passenger fixture");
        balloon.setDeltaMovement(Vec3.ZERO);
        BalloonEntityBehaviour.tick(balloon, physics);
        helper.assertTrue(balloon.getDeltaMovement().equals(Vec3.ZERO), "Passenger received buoyancy");
        balloon.stopRiding();
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void buildHeightBurstStopsMotionAndRunsDestroyHook(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        PackageEntity balloon = balloon(helper);
        balloon.setPos(balloon.getX(), level.getMaxBuildHeight(), balloon.getZ());
        balloon.setDeltaMovement(Vec3.ZERO);
        BalloonWorldPhysics physics = BalloonWorldPhysics.of(balloon.getBox(), level, balloon.blockPosition());
        BalloonEntityBehaviour.tick(balloon, physics);
        helper.assertTrue(!balloon.isAlive(), "Filled balloon survived build height");
        helper.assertTrue(balloon.isInvulnerable(), "Real destroy hook did not protect the burst source");
        helper.assertTrue(balloon.getDeltaMovement().equals(Vec3.ZERO), "Destroyed balloon continued buoyancy");
        PackageEntity empty = balloon(helper);
        BalloonItem.setGas(empty.getBox(), GasStack.EMPTY);
        empty.setPos(empty.getX(), level.getMaxBuildHeight(), empty.getZ());
        BalloonEntityBehaviour.tick(empty, BalloonWorldPhysics.of(empty.getBox(), level, empty.blockPosition()));
        helper.assertTrue(empty.isAlive(), "Empty balloon burst at build height");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void realTickRefreshesDimensionsAndDrowningDoesNotDestroy(GameTestHelper helper) {
        PackageEntity balloon = balloon(helper);
        float oldWidth = balloon.getBbWidth();
        GasStack gas = BalloonItem.getGas(balloon.getBox());
        gas.setAmount(Math.max(1, gas.getAmount() / 8));
        BalloonItem.setGas(balloon.getBox(), gas);
        balloon.setNoGravity(true);
        balloon.tick();
        helper.assertTrue(balloon.getBbWidth() < oldWidth, "Tick did not refresh changed balloon size");
        helper.assertTrue(!balloon.hurt(balloon.damageSources().drown(), Float.MAX_VALUE) && balloon.isAlive(), "Drowning destroyed balloon");
        helper.succeed();
    }

    private static PackageEntity balloon(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(1, 1, 1));
        ItemStack item = BalloonStyles.createDefaultBalloon();
        long amount = BalloonPackingLimits.getLocalPackingLimit(level, BlockPos.containing(pos));
        BalloonItem.setGas(item, new GasStack(CCBGases.NATURAL_AIR.get(), amount));
        return PackageEntity.fromItemStack(level, pos, item);
    }
}
