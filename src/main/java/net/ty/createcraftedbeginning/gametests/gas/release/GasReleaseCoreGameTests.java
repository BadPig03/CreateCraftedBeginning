package net.ty.createcraftedbeginning.gametests.gas.release;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseCause;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseContext;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseMode;
import net.ty.createcraftedbeginning.gas.atmosphere.AtmosphereStateResolver;
import net.ty.createcraftedbeginning.gas.release.GasReleaseFeedback;
import net.ty.createcraftedbeginning.gas.release.GasReleaseRequest;
import net.ty.createcraftedbeginning.gas.release.GasReleaseResult;
import net.ty.createcraftedbeginning.gas.release.GasReleaseService;
import net.ty.createcraftedbeginning.gas.release.GasReleaseState;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;
import java.util.OptionalLong;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GasReleaseCoreGameTests {
    private static final BlockPos SOURCE_POS = new BlockPos(1, 1, 1);
    private static final float EPSILON = 0.0001F;

    private GasReleaseCoreGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void continuousReleaseKeepsCadenceAndProgressBounded(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos sourcePos = helper.absolutePos(SOURCE_POS);
        GasReleaseState state = new GasReleaseState();
        long gameTime = level.getGameTime();

        GasReleaseResult first = GasReleaseService.release(level, GasReleaseRequest.radial(new GasStack(CCBGases.STEAM.get(), 250), sourcePos, GasReleaseCause.MANUAL_VENT), state);
        helper.assertTrue(first.released(), "First continuous gas release was not reported as released");
        helper.assertTrue(first.feedbackDue(), "First continuous gas release did not schedule feedback");
        helper.assertTrue(!first.effectDue(), "250 GU continuous gas release unexpectedly scheduled an effect");
        helper.assertValueEqual(state.getEffectProgress(), 250L, "continuous effect progress after 250 GU");
        helper.assertValueEqual(state.getLastFeedbackTick(), gameTime, "continuous feedback tick after first release");

        GasReleaseResult second = GasReleaseService.release(level, GasReleaseRequest.radial(new GasStack(CCBGases.STEAM.get(), 750), sourcePos, GasReleaseCause.MANUAL_VENT), state);
        helper.assertTrue(second.released(), "Second continuous gas release was not reported as released");
        helper.assertTrue(!second.feedbackDue(), "Same-tick continuous gas release scheduled duplicate feedback");
        helper.assertTrue(second.effectDue(), "250 + 750 GU continuous gas release did not cross the effect interval");
        helper.assertValueEqual(state.getEffectProgress(), 0L, "continuous effect progress after exactly 1000 GU");
        helper.assertValueEqual(state.getLastEffectTick(), gameTime, "continuous effect tick after crossing the interval");

        GasReleaseResult third = GasReleaseService.release(level, GasReleaseRequest.radial(new GasStack(CCBGases.STEAM.get(), GasReleaseService.EFFECT_INTERVAL), sourcePos, GasReleaseCause.MANUAL_VENT), state);
        helper.assertTrue(!third.feedbackDue(), "Same-tick high-flow release scheduled duplicate feedback");
        helper.assertTrue(!third.effectDue(), "Same-tick high-flow release bypassed the single-effect cadence cap");
        helper.assertValueEqual(state.getEffectProgress(), 0L, "continuous effect progress after same-tick 1000 GU release");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void continuousReleaseProgressResetsWhenPressureProfileChanges(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos sourcePos = helper.absolutePos(SOURCE_POS);
        GasReleaseState state = new GasReleaseState();
        GasStack partialRelease = new GasStack(CCBGases.NATURAL_AIR.get(), GasReleaseService.EFFECT_INTERVAL / 2);

        GasReleaseResult normal = GasReleaseService.release(level, GasReleaseRequest.radial(partialRelease, sourcePos, GasReleaseCause.MANUAL_VENT, GameplayPressureProfiles.HIGH_PRESSURE.minimumPressurePa() - 1), state);
        helper.assertTrue(!normal.effectDue(), "Half-interval normal-pressure release unexpectedly triggered an effect");
        helper.assertValueEqual(state.getEffectProgress(), GasReleaseService.EFFECT_INTERVAL / 2, "Normal-pressure release progress");
        helper.assertTrue(state.getEffectPressureProfile().equals(GameplayPressureProfiles.NORMAL), "Normal-pressure release stored the wrong gameplay pressure profile");

        GasReleaseResult highPressure = GasReleaseService.release(level, GasReleaseRequest.radial(partialRelease, sourcePos, GasReleaseCause.MANUAL_VENT, GameplayPressureProfiles.HIGH_PRESSURE.minimumPressurePa()), state);
        helper.assertTrue(!highPressure.effectDue(), "Normal and high-pressure release progress leaked across pressure tiers");
        helper.assertValueEqual(state.getEffectProgress(), GasReleaseService.EFFECT_INTERVAL / 2, "High-pressure release progress after profile switch");
        helper.assertTrue(state.getEffectPressureProfile().equals(GameplayPressureProfiles.HIGH_PRESSURE), "High-pressure release stored the wrong gameplay pressure profile");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void releaseStatePersistsProfileId(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos sourcePos = helper.absolutePos(SOURCE_POS);
        GasReleaseState state = new GasReleaseState();
        GasStack partialRelease = new GasStack(CCBGases.NATURAL_AIR.get(), GasReleaseService.EFFECT_INTERVAL / 2);
        long highPressurePa = GameplayPressureProfiles.HIGH_PRESSURE.minimumPressurePa();

        GasReleaseService.release(level, GasReleaseRequest.radial(partialRelease, sourcePos, GasReleaseCause.MANUAL_VENT, highPressurePa), state);
        CompoundTag stored = state.write(level.registryAccess());
        helper.assertTrue(stored.contains("EffectPressureProfile", Tag.TAG_STRING), "Gas release state did not persist the gameplay pressure profile id");
        helper.assertTrue(stored.getString("EffectPressureProfile").equals(GameplayPressureProfiles.HIGH_PRESSURE.id().toString()), "Gas release state persisted the wrong gameplay pressure profile id");

        GasReleaseState roundTripped = GasReleaseState.read(stored, level.registryAccess());
        helper.assertTrue(roundTripped.getEffectPressureProfile().equals(GameplayPressureProfiles.HIGH_PRESSURE), "Gas release state lost its gameplay pressure profile after NBT round-trip");
        helper.assertValueEqual(roundTripped.getEffectProgress(), GasReleaseService.EFFECT_INTERVAL / 2, "Gas release state progress after profile NBT round-trip");

        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void releaseRequestsPreserveDirectionalAndRadialGeometry(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos sourcePos = helper.absolutePos(SOURCE_POS);
        GasStack gas = new GasStack(CCBGases.STEAM.get(), 1);

        GasReleaseRequest directional = GasReleaseRequest.directional(gas, sourcePos, Direction.EAST, GasReleaseCause.ATMOSPHERIC_OUTLET);
        helper.assertTrue(directional.mode() == GasReleaseMode.DIRECTIONAL, "Directional release request lost its mode");
        helper.assertTrue(directional.direction().isPresent() && directional.direction().get() == Direction.EAST, "Directional release request lost its direction");
        helper.assertTrue(directional.sourcePos().equals(sourcePos), "Directional release request changed its source position");
        helper.assertTrue(directional.effectPos().equals(sourcePos.relative(Direction.EAST)), "Directional release request calculated the wrong effect position");

        GasReleaseRequest radial = GasReleaseRequest.radial(gas, sourcePos, GasReleaseCause.TANK_REMOVAL);
        helper.assertTrue(radial.mode() == GasReleaseMode.RADIAL, "Radial release request lost its mode");
        helper.assertTrue(radial.direction().isEmpty(), "Radial release request unexpectedly contains a direction");
        helper.assertTrue(radial.sourcePos().equals(sourcePos), "Radial release request changed its source position");
        helper.assertTrue(radial.effectPos().equals(sourcePos), "Radial release request calculated the wrong effect position");

        assertIllegalArgument(() -> new GasReleaseRequest(gas, sourcePos, sourcePos, Optional.of(Direction.EAST), GasReleaseCause.ATMOSPHERIC_OUTLET, GasReleaseMode.DIRECTIONAL, OptionalLong.empty()), "Directional release request accepted an effect position that does not match its source and direction.");
        assertIllegalArgument(() -> new GasReleaseRequest(gas, sourcePos, sourcePos.relative(Direction.EAST), Optional.empty(), GasReleaseCause.TANK_REMOVAL, GasReleaseMode.RADIAL, OptionalLong.empty()), "Radial release request accepted an effect position that does not match its source.");

        GasReleaseContext directionalContext = GasReleaseService.createContext(level, directional);
        GasReleaseContext radialContext = GasReleaseService.createContext(level, radial);
        helper.assertTrue(directionalContext.effectPos().equals(sourcePos.relative(Direction.EAST)), "Directional release context calculated the wrong effect position");
        helper.assertTrue(radialContext.effectPos().equals(sourcePos), "Radial release context calculated the wrong effect position");
        helper.assertValueEqual(directionalContext.atmosphericPressurePa(), AtmosphereStateResolver.resolve(level, directional.effectPos()).pressurePa(), "directional release atmospheric pressure");
        helper.assertValueEqual(radialContext.atmosphericPressurePa(), AtmosphereStateResolver.resolve(level, radial.effectPos()).pressurePa(), "radial release atmospheric pressure");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void pressureAwareFeedbackUsesPressureDeltaWithoutChangingGasAmount(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos sourcePos = helper.absolutePos(SOURCE_POS);
        long atmosphericPressurePa = AtmosphereStateResolver.resolve(level, sourcePos).pressurePa();
        long sevenAtmospheresPa = GasPressure.pascals(7);
        long sourcePressurePa = Math.addExact(atmosphericPressurePa, sevenAtmospheresPa);
        GasStack suppliedGas = new GasStack(CCBGases.STEAM.get(), 1500);
        GasReleaseRequest pressureAwareRequest = GasReleaseRequest.radial(suppliedGas, sourcePos, GasReleaseCause.MANUAL_VENT, sourcePressurePa);
        GasReleaseContext pressureAwareContext = GasReleaseService.createContext(level, pressureAwareRequest);

        helper.assertTrue(pressureAwareContext.pressureDeltaPa().isPresent(), "Pressure-aware release context did not expose pressure delta");
        helper.assertTrue(pressureAwareContext.pressureDeltaPa().getAsLong() == sevenAtmospheresPa, "Pressure-aware release delta did not subtract atmospheric pressure");

        GasReleaseFeedback pressureAwareFeedback = GasReleaseFeedback.of(pressureAwareContext);
        helper.assertTrue(pressureAwareContext.effectBounds().equals(new AABB(sourcePos).inflate(pressureAwareFeedback.outlineInflation())), "Actual release bounds differ from the displayed outline for pressureAware pressure.");
        assertClose(helper, pressureAwareFeedback.pressureScale(), 1.5F, "pressure-aware feedback scale at 7 atm delta");
        assertClose(helper, pressureAwareFeedback.outlineInflation(), 1.5F, "pressure-aware outline inflation at 7 atm delta");
        assertClose(helper, pressureAwareFeedback.soundVolume(), 1.5F, "pressure-aware sound volume at 7 atm delta");
        assertClose(helper, pressureAwareFeedback.soundPitch(), 0.9F, "pressure-aware sound pitch at 7 atm delta");

        GasReleaseContext zeroDeltaContext = GasReleaseService.createContext(level, GasReleaseRequest.radial(suppliedGas, sourcePos, GasReleaseCause.MANUAL_VENT, atmosphericPressurePa));
        GasReleaseFeedback zeroDeltaFeedback = GasReleaseFeedback.of(zeroDeltaContext);
        helper.assertTrue(zeroDeltaContext.effectBounds().equals(new AABB(sourcePos).inflate(zeroDeltaFeedback.outlineInflation())), "Actual release bounds differ from the displayed outline for zeroDelta pressure.");
        helper.assertTrue(zeroDeltaContext.pressureDeltaPa().isPresent() && zeroDeltaContext.pressureDeltaPa().getAsLong() == 0, "Equal source and atmospheric pressure did not produce zero pressure delta");
        assertClose(helper, zeroDeltaFeedback.pressureScale(), 0.75F, "zero-delta feedback scale");
        assertClose(helper, zeroDeltaFeedback.outlineInflation(), 0.75F, "zero-delta outline inflation");
        assertClose(helper, zeroDeltaFeedback.soundVolume(), 0.75F, "zero-delta sound volume");
        assertClose(helper, zeroDeltaFeedback.soundPitch(), 1.05F, "zero-delta sound pitch");

        GasReleaseContext unknownPressureContext = GasReleaseService.createContext(level, GasReleaseRequest.radial(suppliedGas, sourcePos, GasReleaseCause.MANUAL_VENT));
        GasReleaseFeedback unknownPressureFeedback = GasReleaseFeedback.of(unknownPressureContext);
        helper.assertTrue(unknownPressureContext.effectBounds().equals(new AABB(sourcePos).inflate(unknownPressureFeedback.outlineInflation())), "Actual release bounds differ from the displayed outline for unknownPressure pressure.");
        helper.assertTrue(unknownPressureContext.pressureDeltaPa().isEmpty(), "Unknown source pressure unexpectedly produced a pressure delta");
        assertClose(helper, unknownPressureFeedback.pressureScale(), 1, "unknown-pressure feedback scale");
        assertClose(helper, unknownPressureFeedback.outlineInflation(), 1, "unknown-pressure outline inflation");
        assertClose(helper, unknownPressureFeedback.soundVolume(), 1, "unknown-pressure sound volume");
        assertClose(helper, unknownPressureFeedback.soundPitch(), 1, "unknown-pressure sound pitch");

        long suppliedAmount = suppliedGas.getAmount();
        GasReleaseResult result = GasReleaseService.release(level, pressureAwareRequest);
        helper.assertTrue(result.released(), "Pressure-aware burst release was not reported as released");
        helper.assertTrue(result.feedbackDue(), "Pressure-aware burst release did not schedule feedback");
        helper.assertTrue(result.effectDue(), "1500 GU pressure-aware burst release did not schedule its gas effect");
        helper.assertValueEqual(result.context().gasAmount(), suppliedAmount, "pressure-aware released context gas amount");
        helper.assertValueEqual(suppliedGas.getAmount(), suppliedAmount, "caller gas amount after pressure-aware release");
        helper.assertValueEqual(pressureAwareRequest.gas().getAmount(), suppliedAmount, "release request gas amount after pressure-aware release");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_8x3x7", timeoutTicks = 20)
    public static void moistReleaseUsesScaledEffectBoundary(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos sourcePos = helper.absolutePos(new BlockPos(2, 1, 3));
        Pig expandedTarget = helper.spawn(EntityType.PIG, new BlockPos(4, 1, 3));
        Pig outsideTarget = helper.spawn(EntityType.PIG, new BlockPos(5, 1, 3));
        expandedTarget.setRemainingFireTicks(100);
        outsideTarget.setRemainingFireTicks(100);
        GasStack gas = new GasStack(CCBGases.MOIST_AIR.get(), GasReleaseService.EFFECT_INTERVAL);
        long atmosphericPressurePa = AtmosphereStateResolver.resolve(level, sourcePos).pressurePa();
        GasReleaseService.release(level, GasReleaseRequest.radial(gas, sourcePos, GasReleaseCause.MANUAL_VENT, atmosphericPressurePa));
        helper.assertTrue(expandedTarget.isOnFire(), "Zero-delta release affected a pig outside its contracted bounds.");

        GasReleaseService.release(level, GasReleaseRequest.radial(gas, sourcePos, GasReleaseCause.MANUAL_VENT, atmosphericPressurePa + GasPressure.pascals(7)));
        helper.assertTrue(!expandedTarget.isOnFire(), "Pressure-scaled release did not extinguish a pig inside its expanded bounds.");
        helper.assertTrue(outsideTarget.isOnFire(), "Pressure-scaled release affected a pig outside its expanded bounds.");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void releaseBoundsClampPressureAndUseLocalAtmosphere(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos sourcePos = helper.absolutePos(SOURCE_POS);
        GasStack gas = new GasStack(CCBGases.NATURAL_AIR.get(), GasReleaseService.EFFECT_INTERVAL);
        long sourcePressurePa = GasPressure.pascals(64);
        long[] atmosphericPressures = {0, GasPressure.pascals(57), sourcePressurePa, GasPressure.pascals(65)};
        float[] expectedInflations = {2, 1.5F, 0.75F, 0.75F};
        for (int index = 0; index < atmosphericPressures.length; index++) {
            GasReleaseContext context = new GasReleaseContext(level, gas, sourcePos, sourcePos, Optional.empty(), GasReleaseCause.MANUAL_VENT, GasReleaseMode.RADIAL, OptionalLong.of(sourcePressurePa), atmosphericPressures[index]);
            GasReleaseFeedback feedback = GasReleaseFeedback.of(context);
            assertClose(helper, context.effectInflation(), expectedInflations[index], "Release inflation at atmospheric pressure " + atmosphericPressures[index]);
            helper.assertTrue(context.effectBounds().equals(new AABB(sourcePos).inflate(feedback.outlineInflation())), "Actual release bounds differ from the displayed outline.");
        }

        helper.succeed();
    }

    private static void assertIllegalArgument(Runnable action, String message) {
        try {
            action.run();
        }
        catch (IllegalArgumentException ignored) {
            return;
        }

        throw new IllegalStateException(message);
    }

    private static void assertClose(GameTestHelper helper, float actual, float expected, String description) {
        helper.assertTrue(Math.abs(actual - expected) <= EPSILON, description + ": expected " + expected + ", got " + actual);
    }
}
