package net.ty.createcraftedbeginning.gametests.content.opticalpower;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.opticalpower.photothermalreceiver.PhotothermalHeatState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PhotothermalHeatStateGameTests {
    private static final int FULL_TRANSITION_TICKS = 100;
    private static final int PROGRESS_PER_TICK = 3;
    private static final int HEATING_START_TICK = 34;
    private static final int LAST_HEATED_COOLING_TICK = 66;

    private static final String COMPOUND_KEY_HEAT_PROGRESS = "HeatProgress";

    private PhotothermalHeatStateGameTests() {
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void heatThresholdsMatchProgressAndCooling(GameTestHelper helper) {
        PhotothermalHeatState state = new PhotothermalHeatState();
        for (int tick = 1; tick <= FULL_TRANSITION_TICKS; tick++) {
            state.tick(48);
            int expectedLevel = 0;
            if (tick >= HEATING_START_TICK) {
                expectedLevel = 1;
            }
            if (tick == FULL_TRANSITION_TICKS) {
                expectedLevel = 2;
            }

            helper.assertTrue(state.getHeatProgress() == tick * PROGRESS_PER_TICK && state.getHeatLevel() == expectedLevel, "Cold startup crossed a heat threshold at the wrong tick: " + tick + '.');
        }
        for (int tick = 0; tick < 100; tick++) {
            state.tick(48);
            helper.assertTrue(state.getHeatProgress() == 300 && state.getHeatLevel() == 2, "Sustained full input did not remain at full progress.");
        }
        for (int tick = 1; tick <= FULL_TRANSITION_TICKS; tick++) {
            state.tick(0);
            int expectedLevel = 0;
            if (tick <= LAST_HEATED_COOLING_TICK) {
                expectedLevel = 1;
            }

            helper.assertTrue(state.getHeatProgress() == 300 - tick * PROGRESS_PER_TICK && state.getHeatLevel() == expectedLevel, "Cooling retained superheating or crossed a threshold incorrectly at tick " + tick + '.');
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void inputTargetsRemainStableAndRecoveredLightReheats(GameTestHelper helper) {
        PhotothermalHeatState state = new PhotothermalHeatState();
        for (int tick = 0; tick < 5000; tick++) {
            state.tick(15);
        }
        helper.assertTrue(state.getHeatProgress() == 0, "Weak light accumulated heat.");
        for (int power = 16; power <= 48; power++) {
            int expectedProgress = 100;
            int expectedLevel = 1;
            if (power >= 32) {
                expectedProgress = 200;
            }
            if (power == 48) {
                expectedProgress = 300;
                expectedLevel = 2;
            }
            for (int tick = 0; tick < FULL_TRANSITION_TICKS; tick++) {
                state.tick(power);
            }
            helper.assertTrue(state.getHeatProgress() == expectedProgress && state.getHeatLevel() == expectedLevel, "Optical input reached the wrong thermal target at " + power + " LP.");
        }
        state.tick(47);
        helper.assertTrue(state.getHeatProgress() == 297 && state.getHeatLevel() == 1, "Input below 48 LP retained superheating.");
        for (int tick = 0; tick < FULL_TRANSITION_TICKS; tick++) {
            state.tick(32);
        }
        helper.assertTrue(state.getHeatProgress() == 200 && state.getHeatLevel() == 1, "32 LP failed to settle at 100 percent ordinary heating.");
        for (int tick = 0; tick < HEATING_START_TICK; tick++) {
            state.tick(48);
        }
        helper.assertTrue(state.getHeatProgress() == 300 && state.getHeatLevel() == 2, "Recovered full input did not restore superheating.");
        for (int tick = 0; tick < FULL_TRANSITION_TICKS; tick++) {
            state.tick(0);
        }
        for (int tick = 0; tick < HEATING_START_TICK; tick++) {
            state.tick(16);
        }
        helper.assertTrue(state.getHeatProgress() == 100, "16 LP did not reach 50 percent in 34 ticks.");
        state.tick(0);
        helper.assertTrue(state.getHeatProgress() == 97 && state.getHeatLevel() == 0, "Ordinary heating continued below its threshold.");
        state.tick(16);
        helper.assertTrue(state.getHeatProgress() == 100 && state.getHeatLevel() == 1, "Recovered input failed to restore ordinary heating.");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_3x3", timeoutTicks = 20)
    public static void savedProgressResumesWithoutPersistingLight(GameTestHelper helper) {
        PhotothermalHeatState original = new PhotothermalHeatState();
        for (int tick = 0; tick < 50; tick++) {
            original.tick(32);
        }
        CompoundTag tag = new CompoundTag();
        original.write(tag);
        helper.assertTrue(tag.getAllKeys().size() == 1 && tag.getInt(COMPOUND_KEY_HEAT_PROGRESS) == 150, "Heat state did not persist only its progress.");
        PhotothermalHeatState restored = new PhotothermalHeatState();
        restored.read(tag);
        helper.assertTrue(restored.getHeatProgress() == 150 && restored.getHeatLevel() == 1, "Saved progress did not restore its heat level.");
        for (int tick = 0; tick < 100; tick++) {
            original.tick(0);
            restored.tick(0);
            helper.assertTrue(original.getHeatProgress() == restored.getHeatProgress() && original.getHeatLevel() == restored.getHeatLevel(), "Reload changed cooling behavior.");
        }
        helper.assertTrue(restored.getHeatProgress() == 0, "Restored heat persisted without light.");
        tag.putInt(COMPOUND_KEY_HEAT_PROGRESS, Integer.MAX_VALUE);
        restored.read(tag);
        helper.assertTrue(restored.getHeatProgress() == 300, "Stored progress exceeded the upper bound.");
        tag.putInt(COMPOUND_KEY_HEAT_PROGRESS, -1);
        restored.read(tag);
        helper.assertTrue(restored.getHeatProgress() == 0, "Stored progress fell below zero.");
        restored.tick(32);
        restored.read(new CompoundTag());
        helper.assertTrue(restored.getHeatProgress() == 0, "Missing progress did not restore a cold state.");
        helper.succeed();
    }
}
