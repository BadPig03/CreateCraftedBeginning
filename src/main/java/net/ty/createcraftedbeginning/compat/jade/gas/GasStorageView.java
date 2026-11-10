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
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.foundation.NbtValues;
import net.ty.createcraftedbeginning.gas.visual.GasUnitFormat;
import org.jetbrains.annotations.Nullable;
import snownee.jade.api.ui.IElement;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.NoSuchElementException;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasStorageView {
    static final long NO_PRESSURE_READING = -1;
    static final String COMPOUND_KEY_STORAGE_GAS = "gas";
    private static final String COMPOUND_KEY_STORAGE_MAX_AMOUNT = "max_amount";
    private static final String COMPOUND_KEY_STORAGE_PRESSURE = "pressure_pa";
    static final String COMPOUND_KEY_STORAGE_CREATIVE = "creative";

    final IElement overlay;
    final String currentAmountText;
    final String maxAmountText;
    final boolean hasCapacity;
    final float ratio;
    final long pressurePa;
    final Component gasName;
    final List<Component> gasTooltip;
    @Nullable
    final Component overrideText;
    final boolean creative;

    private GasStorageView(GasStorageEntry gas, String maxAmountText, boolean hasCapacity, float ratio, long pressurePa, @Nullable Component overrideText, boolean creative) {
        long amount = gas.amount();
        GasStack stack = new GasStack(gas.gasType(), amount, gas.components());
        overlay = new GasStorageElement(stack);
        currentAmountText = GasUnitFormat.formatPrecise(amount);
        this.maxAmountText = maxAmountText;
        this.hasCapacity = hasCapacity;
        this.ratio = ratio;
        this.pressurePa = pressurePa;
        gasName = stack.getHoverName();
        gasTooltip = stack.getGasType().getTooltip(stack);
        this.overrideText = overrideText;
        this.creative = creative;
    }

    static CompoundTag writeAmountOnly(GasStorageEntry storageEntry, boolean creative) {
        CompoundTag viewData = new CompoundTag();
        DataResult<Tag> encodedEntry = GasStorageEntry.CODEC.encodeStart(NbtOps.INSTANCE, storageEntry);
        viewData.put(COMPOUND_KEY_STORAGE_GAS, encodedEntry.result().orElseThrow(() -> new NoSuchElementException("Failed to encode Jade gas storage entry: " + encodedEntry.error().map(Error::message).orElse("No encoded value was returned."))));
        if (creative) {
            viewData.putBoolean(COMPOUND_KEY_STORAGE_CREATIVE, true);
        }
        return viewData;
    }

    @Nullable
    static GasStorageView readDefault(CompoundTag compoundTag, boolean includeCapacity, boolean includePressure) {
        boolean hasCapacity = includeCapacity && compoundTag.contains(COMPOUND_KEY_STORAGE_MAX_AMOUNT);
        long maxAmount = hasCapacity ? compoundTag.getLong(COMPOUND_KEY_STORAGE_MAX_AMOUNT) : 0;
        if (hasCapacity && maxAmount <= 0) {
            return null;
        }

        GasStorageEntry gas = GasStorageEntry.CODEC.parse(NbtOps.INSTANCE, compoundTag.get(COMPOUND_KEY_STORAGE_GAS)).result().orElse(null);
        if (gas == null) {
            return null;
        }

        boolean creative = compoundTag.getBoolean(COMPOUND_KEY_STORAGE_CREATIVE);
        boolean empty = gas.isEmpty();
        String maxAmountText = hasCapacity ? GasUnitFormat.formatPrecise(maxAmount) : "";
        float ratio = empty ? 0 : 1;
        if (hasCapacity && !creative) {
            ratio = (float) gas.amount() / maxAmount;
        }
        long pressurePa = includePressure ? Math.max(NO_PRESSURE_READING, NbtValues.getLongOrDefault(compoundTag, COMPOUND_KEY_STORAGE_PRESSURE, NO_PRESSURE_READING)) : NO_PRESSURE_READING;
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

    static CompoundTag writeObserved(GasStorageEntry storageEntry, long maxAmount, boolean creative, long pressurePa, boolean includeCapacity, boolean includePressure) {
        CompoundTag viewData = writeAmountOnly(storageEntry, creative);
        if (includeCapacity && maxAmount > 0) {
            viewData.putLong(COMPOUND_KEY_STORAGE_MAX_AMOUNT, maxAmount);
        }
        if (includePressure && pressurePa >= GasPressure.VACUUM_PA) {
            viewData.putLong(COMPOUND_KEY_STORAGE_PRESSURE, pressurePa);
        }
        return viewData;
    }
}
