package net.ty.createcraftedbeginning.gametests.gas;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.armhandlers.AirtightArmHandlers;
import net.ty.createcraftedbeginning.api.armorhandlers.AirtightArmorsHandlers;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonHandlers;
import net.ty.createcraftedbeginning.api.cannonhandlers.visual.AirtightCannonVisualHandlers;
import net.ty.createcraftedbeginning.api.drillhandlers.AirtightDrillHandlers;
import net.ty.createcraftedbeginning.api.enginehandlers.AirtightEngineHandlers;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureHandlerRegistry;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureBoundary;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseHandlers;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferExecutor;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferExecutor.PlannedDrain;
import net.ty.createcraftedbeginning.gas.network.solver.transfer.GasTransferExecutor.PlannedFill;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PressureAwareGameplayHandlersGameTests {
    private PressureAwareGameplayHandlersGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void explicitProfileLookupMatchesPhysicalPressureAcrossStatelessHandlers(GameTestHelper helper) {
        Gas gas = CCBGases.NATURAL_AIR.get();
        long normalPressure = GameplayPressureProfiles.HIGH_PRESSURE.minimumPressurePa() - 1;
        long highPressure = GameplayPressureProfiles.HIGH_PRESSURE.minimumPressurePa();

        helper.assertTrue(AirtightEngineHandlers.resolve(gas, GameplayPressureProfiles.NORMAL) == AirtightEngineHandlers.resolve(gas, normalPressure), "Engine NORMAL profile lookup diverged from physical pressure resolution");
        helper.assertTrue(AirtightEngineHandlers.resolve(gas, GameplayPressureProfiles.HIGH_PRESSURE) == AirtightEngineHandlers.resolve(gas, highPressure), "Engine HIGH_PRESSURE profile lookup diverged from physical pressure resolution");
        helper.assertTrue(AirtightArmHandlers.resolve(gas, GameplayPressureProfiles.NORMAL) == AirtightArmHandlers.resolve(gas, normalPressure), "Arm NORMAL profile lookup diverged from physical pressure resolution");
        helper.assertTrue(AirtightArmHandlers.resolve(gas, GameplayPressureProfiles.HIGH_PRESSURE) == AirtightArmHandlers.resolve(gas, highPressure), "Arm HIGH_PRESSURE profile lookup diverged from physical pressure resolution");
        helper.assertTrue(AirtightArmorsHandlers.resolve(gas, GameplayPressureProfiles.NORMAL) == AirtightArmorsHandlers.resolve(gas, normalPressure), "Armors NORMAL profile lookup diverged from physical pressure resolution");
        helper.assertTrue(AirtightArmorsHandlers.resolve(gas, GameplayPressureProfiles.HIGH_PRESSURE) == AirtightArmorsHandlers.resolve(gas, highPressure), "Armors HIGH_PRESSURE profile lookup diverged from physical pressure resolution");
        helper.assertTrue(AirtightCannonHandlers.resolve(gas, GameplayPressureProfiles.NORMAL) == AirtightCannonHandlers.resolve(gas, normalPressure), "Cannon NORMAL profile lookup diverged from physical pressure resolution");
        helper.assertTrue(AirtightCannonHandlers.resolve(gas, GameplayPressureProfiles.HIGH_PRESSURE) == AirtightCannonHandlers.resolve(gas, highPressure), "Cannon HIGH_PRESSURE profile lookup diverged from physical pressure resolution");
        helper.assertTrue(AirtightCannonVisualHandlers.resolve(gas, GameplayPressureProfiles.NORMAL) == AirtightCannonVisualHandlers.resolve(gas, normalPressure), "Cannon visual NORMAL profile lookup diverged from physical pressure resolution");
        helper.assertTrue(AirtightCannonVisualHandlers.resolve(gas, GameplayPressureProfiles.HIGH_PRESSURE) == AirtightCannonVisualHandlers.resolve(gas, highPressure), "Cannon visual HIGH_PRESSURE profile lookup diverged from physical pressure resolution");
        helper.assertTrue(AirtightDrillHandlers.resolve(gas, GameplayPressureProfiles.NORMAL) == AirtightDrillHandlers.resolve(gas, normalPressure), "Drill NORMAL profile lookup diverged from physical pressure resolution");
        helper.assertTrue(AirtightDrillHandlers.resolve(gas, GameplayPressureProfiles.HIGH_PRESSURE) == AirtightDrillHandlers.resolve(gas, highPressure), "Drill HIGH_PRESSURE profile lookup diverged from physical pressure resolution");
        helper.assertTrue(GasReleaseHandlers.resolve(gas, GameplayPressureProfiles.NORMAL) == GasReleaseHandlers.resolve(gas, normalPressure), "Gas release NORMAL profile lookup diverged from physical pressure resolution");
        helper.assertTrue(GasReleaseHandlers.resolve(gas, GameplayPressureProfiles.HIGH_PRESSURE) == GasReleaseHandlers.resolve(gas, highPressure), "Gas release HIGH_PRESSURE profile lookup diverged from physical pressure resolution");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void profiledHandlerRegistryFallsBackToNearestLowerProfile(GameTestHelper helper) {
        GameplayPressureHandlerRegistry<String> registry = GameplayPressureHandlerRegistry.create();
        Gas naturalAir = CCBGases.NATURAL_AIR.get();
        Gas moistAir = CCBGases.MOIST_AIR.get();

        registry.register(naturalAir, GameplayPressureProfiles.NORMAL, "natural-normal");
        registry.register(naturalAir, GameplayPressureProfiles.HIGH_PRESSURE, "natural-high-pressure");
        registry.register(moistAir, GameplayPressureProfiles.NORMAL, "moist-normal");

        String normal = registry.get(naturalAir, GameplayPressureProfiles.NORMAL);
        if (normal == null) {
            throw new NullPointerException("Expected the natural air normal handler.");
        }

        helper.assertValueEqual(normal, "natural-normal", "Profiled registry normal handler");
        String highPressure = registry.get(naturalAir, GameplayPressureProfiles.HIGH_PRESSURE);
        if (highPressure == null) {
            throw new NullPointerException("Expected the natural air high-pressure handler.");
        }

        helper.assertValueEqual(highPressure, "natural-high-pressure", "Profiled registry specialized handler");
        String fallback = registry.get(moistAir, GameplayPressureProfiles.HIGH_PRESSURE);
        if (fallback == null) {
            throw new NullPointerException("Expected the moist air normal fallback handler.");
        }

        helper.assertValueEqual(fallback, "moist-normal", "Missing higher-profile handler did not fall back to the normal handler");
        helper.assertTrue(!registry.containsExact(moistAir, GameplayPressureProfiles.HIGH_PRESSURE), "Fallback handler was incorrectly reported as an exact higher-profile registration");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void transferExecutorPropagatesSolvedPressureToBoundaryConsumers(GameTestHelper helper) {
        long sourcePressurePa = GameplayPressureProfiles.HIGH_PRESSURE.minimumPressurePa() + GasPressure.REFERENCE_PRESSURE_PA;
        GasStack gas = new GasStack(CCBGases.NATURAL_AIR.get(), 100);
        RecordingPressureBoundary boundary = new RecordingPressureBoundary();

        helper.assertValueEqual(GasTransferExecutor.simulateFillAmount(boundary, gas, 50, sourcePressurePa), 50L, "Pressure-aware fill simulation amount");
        helper.assertValueEqual(boundary.lastSourcePressurePa, sourcePressurePa, "Pressure-aware fill simulation source pressure");

        GasTank source = new GasTank(1000, GasPressure.pascals(20));
        source.tryReplaceContents(gas).requireAccepted();
        GasTransferExecutor.executePooledTransfer(gas.copyWithAmount(1), List.of(new PlannedDrain(source, 50)), List.of(new PlannedFill(boundary, 50, sourcePressurePa)));
        helper.assertValueEqual(boundary.lastSourcePressurePa, sourcePressurePa, "Pressure-aware fill execution source pressure");
        helper.succeed();
    }

    private static final class RecordingPressureBoundary implements GasPressureBoundary {
        private long lastSourcePressurePa = -1;

        @Override
        public boolean isGasValid(int tank, GasStack stack) {
            return tank == 0 && !stack.isEmpty();
        }

        @Override
        public GasStack drain(GasStack resource, GasAction action) {
            return GasStack.EMPTY;
        }

        @Override
        public GasStack drain(long maxDrain, GasAction action) {
            return GasStack.EMPTY;
        }

        @Override
        public GasStack getGasInTank(int tank) {
            return GasStack.EMPTY;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public long fill(GasStack resource, GasAction action) {
            return resource.getAmount();
        }

        @Override
        public long fillFromPressure(GasStack resource, long sourcePressurePa, GasAction action) {
            lastSourcePressurePa = sourcePressurePa;
            return resource.getAmount();
        }
    }
}
