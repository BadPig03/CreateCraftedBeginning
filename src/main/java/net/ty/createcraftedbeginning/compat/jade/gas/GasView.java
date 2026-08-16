package net.ty.createcraftedbeginning.compat.jade.gas;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.ty.createcraftedbeginning.api.gas.gases.GasAmounts;
import org.jetbrains.annotations.Nullable;
import snownee.jade.api.ui.IElement;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasView {
    public static final String STORAGE_GAS_KEY = "gas";
    public static final String STORAGE_CAPACITY_KEY = "capacity";
    public static final String STORAGE_CREATIVE_KEY = "creative";

    public IElement overlay;
    public String current;
    public String max;
    public float ratio;
    @Nullable
    public Component gasName;
    @Nullable
    public Component overrideText;
    public boolean creative;

    protected GasView(IElement overlay) {
        this.overlay = overlay;
    }

    @Nullable
    public static GasView readDefault(CompoundTag compoundTag) {
        long capacity = compoundTag.getLong(STORAGE_CAPACITY_KEY);
        if (capacity <= 0) {
            return null;
        }

        GasObject gas = GasObject.CODEC.parse(NbtOps.INSTANCE, compoundTag.get(STORAGE_GAS_KEY)).result().orElse(null);
        if (gas == null) {
            return null;
        }

        GasView view = new GasView(new GasStackElement(gas));
        view.gasName = Component.translatable(gas.gasType().getTranslationKey());
        view.current = GasAmounts.formatLosslessCompact(gas.amount());
        view.max = GasAmounts.formatLosslessCompact(capacity);
        view.ratio = (float) gas.amount() / capacity;
        view.creative = compoundTag.getBoolean(STORAGE_CREATIVE_KEY);
        if (!gas.isEmpty()) {
            return view;
        }

        view.overrideText = view.creative ? Component.translatable("jade.gas.empty_creative") : Component.translatable("jade.gas.empty", Component.literal(view.max).withStyle(ChatFormatting.GRAY));
        return view;
    }

    public static CompoundTag writeDefault(GasObject gasObject, long capacity, boolean creative) {
        CompoundTag data = new CompoundTag();
        if (capacity > 0) {
            data.put(STORAGE_GAS_KEY, GasObject.CODEC.encodeStart(NbtOps.INSTANCE, gasObject).result().orElseThrow());
            data.putLong(STORAGE_CAPACITY_KEY, capacity);
        }
        if (!creative) {
            return data;
        }

        data.putBoolean(STORAGE_CREATIVE_KEY, true);
        return data;
    }
}
