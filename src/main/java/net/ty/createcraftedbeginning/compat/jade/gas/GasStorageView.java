package net.ty.createcraftedbeginning.compat.jade.gas;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DataResult.Error;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.foundation.NbtValues;
import net.ty.createcraftedbeginning.gas.visual.GasUnitFormat;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;
import snownee.jade.api.ui.IElement;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.NoSuchElementException;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasStorageView {
    static final long NO_PRESSURE_READING = -1;
    private static final String STORAGE_GAS_KEY = "gas";
    private static final String STORAGE_MAX_AMOUNT_KEY = "max_amount";
    private static final String STORAGE_PRESSURE_KEY = "pressure_pa";
    private static final String STORAGE_CREATIVE_KEY = "creative";

    final IElement overlay;
    final String currentAmountText;
    final String maxAmountText;
    final boolean hasCapacity;
    final float ratio;
    final long pressurePa;
    final Component gasName;
    @Nullable
    final Component overrideText;
    final boolean creative;

    private GasStorageView(GasStorageEntry gas, String maxAmountText, boolean hasCapacity, float ratio, long pressurePa, @Nullable Component overrideText, boolean creative) {
        overlay = new GasStorageElement(gas);
        currentAmountText = GasUnitFormat.formatPrecise(gas.amount());
        this.maxAmountText = maxAmountText;
        this.hasCapacity = hasCapacity;
        this.ratio = ratio;
        this.pressurePa = pressurePa;
        gasName = Component.translatable(gas.gasType().getTranslationKey());
        this.overrideText = overrideText;
        this.creative = creative;
    }

    @Nullable
    static GasStorageView readDefault(CompoundTag compoundTag, boolean includeCapacity, boolean includePressure) {
        boolean hasCapacity = includeCapacity && compoundTag.contains(STORAGE_MAX_AMOUNT_KEY);
        long maxAmount = hasCapacity ? compoundTag.getLong(STORAGE_MAX_AMOUNT_KEY) : 0;
        if (hasCapacity && maxAmount <= 0) {
            return null;
        }

        GasStorageEntry gas = GasStorageEntry.CODEC.parse(NbtOps.INSTANCE, compoundTag.get(STORAGE_GAS_KEY)).result().orElse(null);
        if (gas == null) {
            return null;
        }

        boolean creative = compoundTag.getBoolean(STORAGE_CREATIVE_KEY);
        boolean empty = gas.isEmpty();
        String maxAmountText = hasCapacity ? GasUnitFormat.formatPrecise(maxAmount) : "";
        float ratio = empty ? 0 : 1;
        if (hasCapacity && !creative) {
            ratio = (float) gas.amount() / maxAmount;
        }
        long pressurePa = includePressure ? Math.max(NO_PRESSURE_READING, NbtValues.getLongOrDefault(compoundTag, STORAGE_PRESSURE_KEY, NO_PRESSURE_READING)) : NO_PRESSURE_READING;
        if (!empty) {
            return new GasStorageView(gas, maxAmountText, hasCapacity, ratio, pressurePa, null, creative);
        }

        Component overrideText;
        if (creative) {
            overrideText = Component.translatable("jade.gas.empty_creative");
        }
        else if (hasCapacity) {
            overrideText = Component.translatable("jade.gas.empty", Component.literal(maxAmountText).withStyle(ChatFormatting.GRAY));
        }
        else {
            overrideText = Component.translatable("jade.gas.empty_unmeasured");
        }
        return new GasStorageView(gas, maxAmountText, hasCapacity, ratio, pressurePa, overrideText, creative);
    }

    @Internal
    public static CompoundTag writeAmountOnly(GasStorageEntry storageEntry, boolean creative) {
        CompoundTag viewData = new CompoundTag();
        DataResult<Tag> encodedEntry = GasStorageEntry.CODEC.encodeStart(NbtOps.INSTANCE, storageEntry);
        viewData.put(STORAGE_GAS_KEY, encodedEntry.result().orElseThrow(() -> new NoSuchElementException("Failed to encode Jade gas storage entry: " + encodedEntry.error().map(Error::message).orElse("No encoded value was returned."))));
        if (creative) {
            viewData.putBoolean(STORAGE_CREATIVE_KEY, true);
        }
        return viewData;
    }

    static CompoundTag writeObserved(GasStorageEntry storageEntry, long maxAmount, boolean creative, long pressurePa, boolean includeCapacity, boolean includePressure) {
        CompoundTag viewData = writeAmountOnly(storageEntry, creative);
        if (includeCapacity && maxAmount > 0) {
            viewData.putLong(STORAGE_MAX_AMOUNT_KEY, maxAmount);
        }
        if (includePressure && pressurePa >= GasPressure.VACUUM_PA) {
            viewData.putLong(STORAGE_PRESSURE_KEY, pressurePa);
        }
        return viewData;
    }
}
