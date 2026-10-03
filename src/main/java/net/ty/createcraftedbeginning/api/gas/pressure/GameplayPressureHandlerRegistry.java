package net.ty.createcraftedbeginning.api.gas.pressure;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.Gas;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GameplayPressureHandlerRegistry<H> {
    private final Map<GameplayPressureProfile, Map<Gas, H>> handlers = new HashMap<>();

    private GameplayPressureHandlerRegistry() {
    }

    public static <H> GameplayPressureHandlerRegistry<H> create() {
        return new GameplayPressureHandlerRegistry<>();
    }

    private static void requireKnownProfile(GameplayPressureProfile profile) {
        if (GameplayPressureProfiles.orderedProfiles().contains(profile)) {
            return;
        }

        throw new IllegalArgumentException("Gameplay pressure profile '" + profile.id() + "' with minimum pressure " + profile.minimumPressurePa() + " Pa is not registered.");
    }

    @Nullable
    public H get(Gas gasType, long sourcePressurePa) {
        return get(gasType, GameplayPressureProfiles.resolve(sourcePressurePa));
    }

    @Nullable
    public H get(Gas gasType, GameplayPressureProfile profile) {
        List<GameplayPressureProfile> orderedProfiles = GameplayPressureProfiles.orderedProfiles();
        int requestedIndex = orderedProfiles.indexOf(profile);
        if (requestedIndex < 0) {
            throw new IllegalArgumentException("Gameplay pressure profile '" + profile.id() + "' with minimum pressure " + profile.minimumPressurePa() + " Pa is not registered.");
        }

        for (int i = requestedIndex; i >= 0; i--) {
            H handler = getExact(gasType, orderedProfiles.get(i));
            if (handler == null) {
                continue;
            }

            return handler;
        }
        return null;
    }

    public boolean containsExact(Gas gasType, GameplayPressureProfile profile) {
        requireKnownProfile(profile);
        return getExact(gasType, profile) != null;
    }

    public void register(Gas gasType, GameplayPressureProfile profile, H handler) {
        requireKnownProfile(profile);
        if (containsExact(gasType, profile)) {
            throw new IllegalStateException("A gameplay pressure handler is already registered for gas '" + gasType + "' and profile '" + profile.id() + "'.");
        }

        handlers.computeIfAbsent(profile, ignored -> new IdentityHashMap<>()).put(gasType, handler);
    }

    @Nullable
    private H getExact(Gas gasType, GameplayPressureProfile profile) {
        Map<Gas, H> profileHandlers = handlers.get(profile);
        if (profileHandlers == null) {
            return null;
        }

        return profileHandlers.get(gasType);
    }
}
