package net.ty.createcraftedbeginning.gametests.gas;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.GasUsageContext;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfileCompoundTags;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.NoSuchElementException;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CreateCraftedBeginning.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GameplayPressureProfilesGameTests {
    private GameplayPressureProfilesGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void profilesPreserveBuiltInHighPressureBoundary(GameTestHelper helper) {
        long threshold = GameplayPressureProfiles.HIGH_PRESSURE.minimumPressurePa();

        helper.assertValueEqual(threshold, GasPressure.pascals(10), "Built-in high-pressure gameplay threshold");
        helper.assertTrue(GameplayPressureProfiles.resolve(GasPressure.VACUUM_PA).equals(GameplayPressureProfiles.NORMAL), "Vacuum did not resolve to the normal gameplay profile");
        helper.assertTrue(GameplayPressureProfiles.resolve(threshold - 1).equals(GameplayPressureProfiles.NORMAL), "Pressure immediately below the high-pressure profile was classified as high pressure");
        helper.assertTrue(!GameplayPressureProfiles.isAtLeast(threshold - 1, GameplayPressureProfiles.HIGH_PRESSURE), "Pressure below 10 atm reached the high-pressure gameplay profile");
        helper.assertTrue(GameplayPressureProfiles.resolve(threshold).equals(GameplayPressureProfiles.HIGH_PRESSURE), "The 10 atm high-pressure gameplay boundary was not inclusive");
        helper.assertTrue(GameplayPressureProfiles.isAtLeast(threshold, GameplayPressureProfiles.HIGH_PRESSURE), "Physical pressure at the high-pressure profile boundary failed the at-least check");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void profilesExposeStableIdsAndOrdering(GameTestHelper helper) {
        int highPressureIndex = GameplayPressureProfiles.orderedProfiles().indexOf(GameplayPressureProfiles.HIGH_PRESSURE);

        helper.assertTrue(!GameplayPressureProfiles.orderedProfiles().isEmpty(), "Gameplay pressure profile registry was empty");
        helper.assertTrue(GameplayPressureProfiles.orderedProfiles().getFirst().equals(GameplayPressureProfiles.NORMAL), "Normal profile was not first in ascending pressure order");
        helper.assertTrue(highPressureIndex > 0, "High-pressure profile was not ordered above normal");
        helper.assertTrue(GameplayPressureProfiles.HIGH_PRESSURE.id().equals(CreateCraftedBeginning.asResource("high_pressure")), "High-pressure gameplay profile did not expose its canonical id");
        helper.assertTrue(GameplayPressureProfiles.isSealed(), "Gameplay pressure profile registry was not sealed after common setup");
        helper.assertTrue(GameplayPressureProfiles.byId(GameplayPressureProfiles.NORMAL.id()).orElseThrow(() -> new NoSuchElementException("Expected a registered gameplay pressure profile in test 'profilesExposeStableIdsAndOrdering'.")).equals(GameplayPressureProfiles.NORMAL), "Normal gameplay profile could not be resolved by stable id");
        helper.assertTrue(GameplayPressureProfiles.byId(GameplayPressureProfiles.HIGH_PRESSURE.id()).orElseThrow(() -> new NoSuchElementException("Expected a registered gameplay pressure profile in test 'profilesExposeStableIdsAndOrdering'.")).equals(GameplayPressureProfiles.HIGH_PRESSURE), "High-pressure gameplay profile could not be resolved by stable id");
        helper.assertTrue(GameplayPressureProfiles.require(GameplayPressureProfiles.HIGH_PRESSURE.id()).equals(GameplayPressureProfiles.HIGH_PRESSURE), "Required gameplay profile lookup returned the wrong profile");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void profileOrderingSupportsAtLeastChecks(GameTestHelper helper) {
        helper.assertTrue(GameplayPressureProfiles.isAtLeast(GameplayPressureProfiles.NORMAL, GameplayPressureProfiles.NORMAL), "Normal profile was not at least itself");
        helper.assertTrue(GameplayPressureProfiles.isAtLeast(GameplayPressureProfiles.HIGH_PRESSURE, GameplayPressureProfiles.NORMAL), "High-pressure profile was not ordered above normal");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void gasUsageContextKeepsExactPressureAlongsideProfile(GameTestHelper helper) {
        long sourcePressurePa = GasPressure.pascals(14.25);
        GasUsageContext context = new GasUsageContext(new GasStack(CCBGases.NATURAL_AIR.get(), 100), sourcePressurePa);

        helper.assertValueEqual(context.sourcePressurePa(), sourcePressurePa, "Gas usage context exact source pressure");
        helper.assertTrue(context.pressureProfile().equals(GameplayPressureProfiles.HIGH_PRESSURE), "Gas usage context did not resolve the high-pressure gameplay profile");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void pressureProfileNbtUsesStableIds(GameTestHelper helper) {
        CompoundTag stored = new CompoundTag();
        GameplayPressureProfileCompoundTags.write(stored, "PressureProfile", GameplayPressureProfiles.HIGH_PRESSURE);

        helper.assertTrue(stored.contains("PressureProfile", Tag.TAG_STRING), "Gameplay pressure profile NBT did not write a stable string id");
        helper.assertTrue(stored.getString("PressureProfile").equals(GameplayPressureProfiles.HIGH_PRESSURE.id().toString()), "Gameplay pressure profile NBT wrote the wrong stable id");
        helper.assertTrue(GameplayPressureProfileCompoundTags.read(stored, "PressureProfile").equals(GameplayPressureProfiles.HIGH_PRESSURE), "Stable gameplay pressure profile id did not round-trip");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void invalidStoredProfilesAreRejected(GameTestHelper helper) {
        CompoundTag unknown = new CompoundTag();
        unknown.putString("PressureProfile", "createcraftedbeginning:future_profile");
        helper.assertTrue(profileReadFails(unknown), "Unknown gameplay pressure profile id was accepted from NBT");

        CompoundTag malformed = new CompoundTag();
        malformed.putString("PressureProfile", "not a resource location");
        helper.assertTrue(profileReadFails(malformed), "Malformed gameplay pressure profile id was accepted from NBT");

        helper.assertTrue(profileReadFails(new CompoundTag()), "Missing gameplay pressure profile id was accepted from NBT");
        helper.succeed();
    }

    private static boolean profileReadFails(CompoundTag stored) {
        try {
            GameplayPressureProfileCompoundTags.read(stored, "PressureProfile");
            return false;
        }
        catch (IllegalArgumentException ignored) {
            return true;
        }
    }

}
