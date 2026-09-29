package net.ty.createcraftedbeginning.gametests.gas;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.events.RegisterAirtightHandlersEvent;
import net.ty.createcraftedbeginning.api.events.RegisterAtmosphereProvidersEvent;
import net.ty.createcraftedbeginning.api.events.RegisterGameplayPressureProfilesEvent;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@GameTestHolder(CCBAPI.MOD_ID)
@PrefixGameTestTemplate(false)
@EventBusSubscriber(modid = CCBAPI.MOD_ID)
public final class RegistrationEventsGameTests {
    private static final List<String> EVENTS = new ArrayList<>();
    private static boolean profilesOpen;

    private RegistrationEventsGameTests() {
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 20)
    public static void pressureEventRunsBeforeRegistrationCloses(GameTestHelper helper) {
        helper.assertValueEqual(EVENTS.stream().filter("profiles"::equals).count(), 1L, "Profile registration event count");
        helper.assertTrue(profilesOpen, "Profile registration was already closed during its event");
        helper.assertTrue(GameplayPressureProfiles.isSealed(), "Profile registration remained open after setup");
        helper.succeed();
    }

    @GameTest(template = "gametest/empty_5x5x5", timeoutTicks = 20)
    public static void atmosphereEventFollowsBothRegistrationStages(GameTestHelper helper) {
        helper.assertTrue(EVENTS.equals(List.of("profiles", "handlers", "atmosphere")), "Registration events were missing, duplicated or out of order: " + EVENTS);
        helper.succeed();
    }

    @SubscribeEvent
    private static void profiles(RegisterGameplayPressureProfilesEvent event) {
        EVENTS.add("profiles");
        profilesOpen = !GameplayPressureProfiles.isSealed();
    }

    @SubscribeEvent
    private static void handlers(RegisterAirtightHandlersEvent event) {
        EVENTS.add("handlers");
    }

    @SubscribeEvent
    private static void atmosphere(RegisterAtmosphereProvidersEvent event) {
        EVENTS.add("atmosphere");
    }
}
