package net.ty.createcraftedbeginning.content.airtights.aircompressor.overheatstates;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public final class OverheatManager {
    private static final Map<String, IOverheatState> STATES = new HashMap<>();

    public static final IOverheatState NORMAL = register(new NormalOverheatState());
    public static final IOverheatState SLIGHT = register(new SlightOverheatState());
    public static final IOverheatState MODERATE = register(new ModerateOverheatState());
    public static final IOverheatState SEVERE = register(new SevereOverheatState());
    public static final IOverheatState MELTDOWN = register(new MeltdownOverheatState());

    @Contract("_ -> param1")
    private static @NotNull IOverheatState register(IOverheatState state) {
        STATES.put(state.getSerializedName(), state);
        return state;
    }

    public static IOverheatState getStateByName(String name) {
        return STATES.getOrDefault(name, NORMAL);
    }

    public static int getCount() {
        return STATES.size();
    }
}
