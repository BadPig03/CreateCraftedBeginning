package net.ty.createcraftedbeginning.gametests.gas;

import com.simibubi.create.content.logistics.box.PackageEntity;
import com.simibubi.create.content.logistics.box.PackageItem;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonItem;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonPackingLimits;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonStyles;
import net.ty.createcraftedbeginning.content.airtights.balloon.BalloonWorldPhysics;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BalloonWorldPhysicsGameTests {
    private static final double EPSILON = 1.0E-6;

    private BalloonWorldPhysicsGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void localStandardBalloonsShareWorldPhysicsAcrossPressure(GameTestHelper helper) {
        long referenceAmount = BalloonPackingLimits.getLocalPackingLimit(GasPressure.REFERENCE_PRESSURE_PA);
        long lowPressure = GasPressure.pascals(0.4);
        long lowPressureAmount = BalloonPackingLimits.getLocalPackingLimit(lowPressure);
        helper.assertTrue(referenceAmount > 0 && lowPressureAmount > 0, "balloon packing limits must be positive");

        BalloonWorldPhysics reference = BalloonWorldPhysics.of(referenceAmount, GasPressure.REFERENCE_PRESSURE_PA);
        BalloonWorldPhysics low = BalloonWorldPhysics.of(lowPressureAmount, lowPressure);

        assertClose(helper, reference.volumeRatio(), 1, "1 atm standard volume ratio");
        assertClose(helper, low.volumeRatio(), 1, "0.4 atm standard volume ratio");
        assertClose(helper, reference.linearScale(), low.linearScale(), "standard linear scale across pressure");
        assertClose(helper, reference.gasLiftFactor(), low.gasLiftFactor(), "standard gas lift factor across pressure");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void lowPressureExpandsTransferredReferenceBalloon(GameTestHelper helper) {
        long referenceAmount = BalloonPackingLimits.getLocalPackingLimit(GasPressure.REFERENCE_PRESSURE_PA);
        long lowPressure = GasPressure.pascals(0.4);
        long lowPressureAmount = BalloonPackingLimits.getLocalPackingLimit(lowPressure);
        helper.assertTrue(referenceAmount > 0 && lowPressureAmount > 0, "balloon packing limits must be positive");

        BalloonWorldPhysics physics = BalloonWorldPhysics.of(referenceAmount, lowPressure);
        double expectedVolumeRatio = (double) referenceAmount / lowPressureAmount;

        assertClose(helper, physics.volumeRatio(), expectedVolumeRatio, "cross-pressure volume ratio");
        assertClose(helper, physics.effectiveVolumeRatio(), Math.min(expectedVolumeRatio, BalloonWorldPhysics.MAX_EFFECTIVE_VOLUME_RATIO), "cross-pressure effective volume ratio");
        assertClose(helper, physics.linearScale(), Math.cbrt(physics.effectiveVolumeRatio()), "cross-pressure linear scale");
        helper.assertTrue(physics.volumeRatio() > 1, "a 1 atm standard balloon must expand at 0.4 atm");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void underfilledBalloonUsesFractionalVolume(GameTestHelper helper) {
        long lowPressure = GasPressure.pascals(0.4);
        long localStandardAmount = BalloonPackingLimits.getLocalPackingLimit(lowPressure);
        helper.assertTrue(localStandardAmount >= 2, "local standard amount must support a half-full test");

        long halfAmount = localStandardAmount / 2;
        BalloonWorldPhysics physics = BalloonWorldPhysics.of(halfAmount, lowPressure);
        double expectedRatio = (double) halfAmount / localStandardAmount;

        assertClose(helper, physics.volumeRatio(), expectedRatio, "underfilled volume ratio");
        assertClose(helper, physics.linearScale(), Math.cbrt(expectedRatio), "underfilled linear scale");
        assertClose(helper, physics.gasLiftFactor(), Math.sqrt(expectedRatio), "underfilled gas lift factor");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void highPressureStandardBalloonStillHasUnitVolume(GameTestHelper helper) {
        long highPressure = GasPressure.pascals(2);
        long localStandardAmount = BalloonPackingLimits.getLocalPackingLimit(highPressure);
        helper.assertTrue(localStandardAmount > 0, "2 atm packing limit must be positive");

        BalloonWorldPhysics physics = BalloonWorldPhysics.of(localStandardAmount, highPressure);

        assertClose(helper, physics.volumeRatio(), 1, "2 atm standard volume ratio");
        assertClose(helper, physics.linearScale(), 1, "2 atm standard linear scale");
        assertClose(helper, physics.gasLiftFactor(), 1, "2 atm standard gas lift factor");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void vacuumKeepsWorldEffectsFinite(GameTestHelper helper) {
        long gasAmount = BalloonPackingLimits.getBaseAmount();
        helper.assertTrue(gasAmount > 0, "balloon base amount must be positive");

        BalloonWorldPhysics physics = BalloonWorldPhysics.of(gasAmount, GasPressure.VACUUM_PA);

        helper.assertTrue(physics.vacuum(), "vacuum state was not preserved");
        helper.assertValueEqual(physics.localStandardAmount(), 0L, "vacuum local standard amount");
        helper.assertTrue(Double.isInfinite(physics.volumeRatio()), "vacuum raw volume ratio must have no finite equilibrium");
        helper.assertTrue(Double.isFinite(physics.effectiveVolumeRatio()), "vacuum effective volume ratio must stay finite");
        helper.assertTrue(Float.isFinite(physics.linearScale()), "vacuum render scale must stay finite");
        helper.assertTrue(Double.isFinite(physics.gasLiftFactor()), "vacuum lift factor must stay finite");
        assertClose(helper, physics.effectiveVolumeRatio(), BalloonWorldPhysics.MAX_EFFECTIVE_VOLUME_RATIO, "vacuum effective volume cap");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void packageEntityHitboxTracksWorldScale(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos localPos = new BlockPos(1, 1, 1);
        BlockPos worldPos = helper.absolutePos(localPos);
        long localStandardAmount = BalloonPackingLimits.getLocalPackingLimit(level, worldPos);
        helper.assertTrue(localStandardAmount > 0, "local standard amount must be positive for entity-dimension test");

        ItemStack standardBalloon = BalloonStyles.createDefaultBalloon();
        BalloonItem.setGas(standardBalloon, new GasStack(CCBGases.NATURAL_AIR.get(), localStandardAmount));
        ItemStack underfilledBalloon = standardBalloon.copy();
        BalloonItem.setGas(underfilledBalloon, new GasStack(CCBGases.NATURAL_AIR.get(), Math.max(1, localStandardAmount / 8)));
        ItemStack expandedBalloon = standardBalloon.copy();
        BalloonItem.setGas(expandedBalloon, new GasStack(CCBGases.NATURAL_AIR.get(), localStandardAmount * 2));

        Vec3 spawn = Vec3.atBottomCenterOf(worldPos);
        PackageEntity standardEntity = PackageEntity.fromItemStack(level, spawn, standardBalloon);
        PackageEntity underfilledEntity = PackageEntity.fromItemStack(level, spawn, underfilledBalloon);
        PackageEntity expandedEntity = PackageEntity.fromItemStack(level, spawn, expandedBalloon);
        float baseWidth = PackageItem.getWidth(standardBalloon);
        float baseHeight = PackageItem.getHeight(standardBalloon);
        float standardScale = BalloonWorldPhysics.of(standardBalloon, level, worldPos).linearScale();
        float underfilledScale = BalloonWorldPhysics.of(underfilledBalloon, level, worldPos).linearScale();
        float expandedScale = BalloonWorldPhysics.of(expandedBalloon, level, worldPos).linearScale();

        assertClose(helper, standardEntity.getBbWidth(), baseWidth * standardScale, "standard balloon entity width");
        assertClose(helper, standardEntity.getBbHeight(), baseHeight * standardScale, "standard balloon entity height");
        assertClose(helper, underfilledEntity.getBbWidth(), baseWidth * underfilledScale, "underfilled balloon entity width");
        assertClose(helper, underfilledEntity.getBbHeight(), baseHeight * underfilledScale, "underfilled balloon entity height");
        assertClose(helper, expandedEntity.getBbWidth(), baseWidth * expandedScale, "expanded balloon entity width");
        assertClose(helper, expandedEntity.getBbHeight(), baseHeight * expandedScale, "expanded balloon entity height");
        helper.assertTrue(underfilledEntity.getBbWidth() < standardEntity.getBbWidth(), "underfilled balloon entity width must shrink with world scale");
        helper.assertTrue(underfilledEntity.getBbHeight() < standardEntity.getBbHeight(), "underfilled balloon entity height must shrink with world scale");
        helper.assertTrue(expandedEntity.getBbWidth() > standardEntity.getBbWidth(), "expanded balloon entity width must grow with world scale");
        helper.assertTrue(expandedEntity.getBbHeight() > standardEntity.getBbHeight(), "expanded balloon entity height must grow with world scale");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void buoyancyUsesMediumSpecificLiftAndSharedSpeedCaps(GameTestHelper helper) {
        long standardAmount = BalloonPackingLimits.getLocalPackingLimit(GasPressure.REFERENCE_PRESSURE_PA);
        helper.assertTrue(standardAmount > 0, "standard amount must be positive for buoyancy test");

        BalloonWorldPhysics physics = BalloonWorldPhysics.of(standardAmount, GasPressure.REFERENCE_PRESSURE_PA);
        double airAcceleration = physics.buoyancyAcceleration(false);
        double waterAcceleration = physics.buoyancyAcceleration(true);
        double airLimit = physics.buoyancyRiseSpeedLimit(false);
        double waterLimit = physics.buoyancyRiseSpeedLimit(true);

        helper.assertTrue(airAcceleration > 0.08, "air buoyancy must overcome vanilla living-entity gravity");
        helper.assertTrue(waterAcceleration > 0, "water buoyancy must remain positive");
        helper.assertTrue(airLimit > 0 && waterLimit > 0, "buoyancy rise-speed limits must be positive");
        assertClose(helper, physics.nextBuoyantVerticalSpeed(airLimit - airAcceleration / 2, false), airLimit, "air buoyancy speed cap");
        assertClose(helper, physics.nextBuoyantVerticalSpeed(waterLimit - waterAcceleration / 2, true), waterLimit, "water buoyancy speed cap");
        assertClose(helper, physics.nextBuoyantVerticalSpeed(airLimit + 0.25, false), airLimit + 0.25, "existing upward throw speed remains untouched");

        BalloonWorldPhysics empty = BalloonWorldPhysics.of(0, GasPressure.REFERENCE_PRESSURE_PA);
        assertClose(helper, empty.buoyancyAcceleration(false), 0, "empty balloon air buoyancy");
        helper.assertTrue(empty.buoyancyAcceleration(true) > 0, "empty balloon keeps the existing water buoyancy");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void worldScaleAndLiftIncreaseMonotonicallyWithVolume(GameTestHelper helper) {
        long pressure = GasPressure.REFERENCE_PRESSURE_PA;
        long standardAmount = BalloonPackingLimits.getLocalPackingLimit(pressure);
        helper.assertTrue(standardAmount >= 4, "standard amount must support monotonic volume samples");

        BalloonWorldPhysics quarter = BalloonWorldPhysics.of(standardAmount / 4, pressure);
        BalloonWorldPhysics standard = BalloonWorldPhysics.of(standardAmount, pressure);
        BalloonWorldPhysics expanded = BalloonWorldPhysics.of(standardAmount * 2, pressure);

        helper.assertTrue(quarter.linearScale() < standard.linearScale(), "linear scale must increase from partial to standard volume");
        helper.assertTrue(standard.linearScale() < expanded.linearScale(), "linear scale must increase beyond standard volume");
        helper.assertTrue(quarter.gasLiftFactor() < standard.gasLiftFactor(), "lift factor must increase from partial to standard volume");
        helper.assertTrue(standard.gasLiftFactor() < expanded.gasLiftFactor(), "lift factor must increase beyond standard volume");
        helper.succeed();
    }

    private static void assertClose(GameTestHelper helper, double actual, double expected, String label) {
        helper.assertTrue(Math.abs(actual - expected) <= EPSILON, label + ": expected " + expected + ", got " + actual);
    }
}
