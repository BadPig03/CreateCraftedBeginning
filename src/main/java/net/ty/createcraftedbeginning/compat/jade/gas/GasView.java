package net.ty.createcraftedbeginning.compat.jade.gas;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import snownee.jade.api.ui.IElement;

import java.text.DecimalFormat;

public class GasView {
    public IElement overlay;
    public String current;
    public String max;
    public float ratio;
    @Nullable
    public Component gasName;
    @Nullable
    public Component overrideText;
    public boolean creative;

    public GasView(@NotNull IElement overlay) {
        this.overlay = overlay;
    }

    @Nullable
    public static GasView readDefault(@NotNull CompoundTag compoundTag) {
        long capacity = compoundTag.getLong(GasConstants.STORAGE_CAPACITY_KEY);
        if (capacity <= 0) {
            return null;
        }

        GasObject gasObject = GasObject.CODEC.parse(NbtOps.INSTANCE, compoundTag.get(GasConstants.STORAGE_GAS_KEY)).result().orElse(null);
        if (gasObject == null) {
            return null;
        }

        GasView gasView = new GasView(new GasStackElement(gasObject));
        gasView.gasName = Component.translatable(gasObject.type().getTranslationKey());
        gasView.current = getDecimalFormat(gasObject.amount());
        gasView.max = getDecimalFormat(capacity);
        gasView.ratio = (float) gasObject.amount() / capacity;
        gasView.creative = compoundTag.getBoolean(GasConstants.STORAGE_CREATIVE_KEY);
        if (gasObject.isEmpty()) {
            gasView.overrideText = Component.translatable("jade.gas.empty", gasView.creative ? Component.empty() : Component.literal(gasView.max).withStyle(ChatFormatting.GRAY));
        }
        return gasView;
    }

    public static @NotNull CompoundTag writeDefault(GasObject gasObject, long capacity, boolean creative) {
        CompoundTag compoundTag = new CompoundTag();
        if (capacity > 0) {
            compoundTag.put(GasConstants.STORAGE_GAS_KEY, GasObject.CODEC.encodeStart(NbtOps.INSTANCE, gasObject).result().orElseThrow());
            compoundTag.putLong(GasConstants.STORAGE_CAPACITY_KEY, capacity);
        }
        if (creative) {
            compoundTag.putBoolean(GasConstants.STORAGE_CREATIVE_KEY, true);
        }
        return compoundTag;
    }

    private static @NotNull String getDecimalFormat(long amount) {
        return new DecimalFormat("#.##").format(amount / 1000.0f) + 'B';
    }
}
