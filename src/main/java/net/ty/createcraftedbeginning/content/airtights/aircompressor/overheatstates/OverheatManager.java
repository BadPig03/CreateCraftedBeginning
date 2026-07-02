package net.ty.createcraftedbeginning.content.airtights.aircompressor.overheatstates;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class OverheatManager {
    private static final Map<String, IOverheatState> STATES = new HashMap<>();

    public static final IOverheatState NORMAL = register(new NormalOverheatState());
    public static final IOverheatState SLIGHT = register(new SlightOverheatState());
    public static final IOverheatState MODERATE = register(new ModerateOverheatState());
    public static final IOverheatState SEVERE = register(new SevereOverheatState());
    public static final IOverheatState MELTDOWN = register(new MeltdownOverheatState());

    @Contract("_ -> param1")
    private static IOverheatState register(IOverheatState state) {
        STATES.put(state.getSerializedName(), state);
        return state;
    }

    public static IOverheatState getStateByName(String name) {
        return STATES.getOrDefault(name, NORMAL);
    }

    public static IOverheatState getStateByItem(ItemStack item) {
        return getStateByName(item.getOrDefault(CCBDataComponents.COMPRESSOR_OVERHEAT_STATE, NORMAL.getSerializedName()));
    }

    public static int getCount() {
        return STATES.size();
    }
}
