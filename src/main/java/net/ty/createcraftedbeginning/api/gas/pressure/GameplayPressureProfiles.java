package net.ty.createcraftedbeginning.api.gas.pressure;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.GasPressure;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GameplayPressureProfiles {
    private static volatile RegistryState state = new RegistryState(List.of(), Map.of());
    public static final GameplayPressureProfile NORMAL = registerInternal(CCBAPI.asResource("normal"), GasPressure.VACUUM_PA);
    public static final GameplayPressureProfile HIGH_PRESSURE = registerInternal(CCBAPI.asResource("high_pressure"), GasPressureTier.MEDIUM.maximumPressurePa());
    private static volatile boolean sealed;

    private GameplayPressureProfiles() {
    }

    public static synchronized GameplayPressureProfile register(ResourceLocation id, long minimumPressurePa) {
        if (sealed) {
            throw new IllegalStateException("Gameplay pressure profile registration is already sealed.");
        }

        return registerInternal(id, minimumPressurePa);
    }

    public static synchronized void seal() {
        sealed = true;
    }

    public static boolean isSealed() {
        return sealed;
    }

    public static GameplayPressureProfile resolve(long sourcePressurePa) {
        long pressurePa = Math.max(GasPressure.VACUUM_PA, sourcePressurePa);
        GameplayPressureProfile resolved = NORMAL;
        for (GameplayPressureProfile profile : state.orderedProfiles()) {
            if (pressurePa < profile.minimumPressurePa()) {
                break;
            }

            resolved = profile;
        }
        return resolved;
    }

    public static Optional<GameplayPressureProfile> byId(ResourceLocation id) {
        return Optional.ofNullable(state.profilesById().get(id));
    }

    public static GameplayPressureProfile require(ResourceLocation id) {
        return byId(id).orElseThrow(() -> new IllegalArgumentException("Gameplay pressure profile id '" + id + "' is not registered."));
    }

    public static List<GameplayPressureProfile> orderedProfiles() {
        return state.orderedProfiles();
    }

    public static boolean isAtLeast(long sourcePressurePa, GameplayPressureProfile minimumProfile) {
        return isAtLeast(resolve(sourcePressurePa), minimumProfile);
    }

    public static boolean isAtLeast(GameplayPressureProfile profile, GameplayPressureProfile minimumProfile) {
        requireKnownProfile(profile);
        requireKnownProfile(minimumProfile);
        return profile.minimumPressurePa() >= minimumProfile.minimumPressurePa();
    }

    public static boolean isRegistered(GameplayPressureProfile profile) {
        GameplayPressureProfile registered = state.profilesById().get(profile.id());
        return profile.equals(registered);
    }

    private static GameplayPressureProfile registerInternal(ResourceLocation id, long minimumPressurePa) {
        if (minimumPressurePa < GasPressure.VACUUM_PA) {
            throw new IllegalArgumentException("Minimum gameplay pressure must be non-negative; got " + minimumPressurePa + " Pa.");
        }

        RegistryState currentState = state;
        if (currentState.profilesById().containsKey(id)) {
            throw new IllegalStateException("Gameplay pressure profile id '" + id + "' is already registered.");
        }

        for (GameplayPressureProfile profile : currentState.orderedProfiles()) {
            if (profile.minimumPressurePa() != minimumPressurePa) {
                continue;
            }

            throw new IllegalStateException("Gameplay pressure profile '" + profile.id() + "' is already registered at minimum pressure " + minimumPressurePa + " Pa; cannot register '" + id + "' at the same pressure.");
        }

        GameplayPressureProfile profile = new GameplayPressureProfile(id, minimumPressurePa);
        ArrayList<GameplayPressureProfile> nextProfiles = new ArrayList<>(currentState.orderedProfiles());
        nextProfiles.add(profile);
        nextProfiles.sort(Comparator.comparingLong(GameplayPressureProfile::minimumPressurePa));
        if (nextProfiles.getFirst().minimumPressurePa() != GasPressure.VACUUM_PA) {
            throw new IllegalStateException("Gameplay pressure profiles must start at " + GasPressure.VACUUM_PA + " Pa; got " + nextProfiles.getFirst().minimumPressurePa() + " Pa.");
        }

        HashMap<ResourceLocation, GameplayPressureProfile> nextProfilesById = new HashMap<>(currentState.profilesById());
        nextProfilesById.put(id, profile);
        state = new RegistryState(List.copyOf(nextProfiles), Map.copyOf(nextProfilesById));
        return profile;
    }

    private static void requireKnownProfile(GameplayPressureProfile profile) {
        if (isRegistered(profile)) {
            return;
        }

        throw new IllegalArgumentException("Gameplay pressure profile '" + profile.id() + "' with minimum pressure " + profile.minimumPressurePa() + " Pa is not registered.");
    }

    private record RegistryState(List<GameplayPressureProfile> orderedProfiles, Map<ResourceLocation, GameplayPressureProfile> profilesById) {}
}
