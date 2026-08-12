package net.ty.createcraftedbeginning.content.airtights.aircompressor;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.coolantshandlers.CoolantEfficiency;
import net.ty.createcraftedbeginning.recipe.PressurizationRecipe;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirCompressorState {
    private static final String COMPOUND_KEY_STORED_HEAT = "StoredHeat";
    private static final String COMPOUND_KEY_COOLANT_EFFICIENCY = "CoolantEfficiency";
    private static final String COMPOUND_KEY_OVERHEAT_STATE = "OverheatState";

    private CoolantEfficiency coolantEfficiency = CoolantEfficiency.NONE;
    private int storedHeat;
    private OverheatState overheatState = OverheatState.NORMAL;
    private WorkState workState = WorkState.EMPTY;

    private static OverheatState readOverheatState(CompoundTag tag) {
        if (!tag.contains(COMPOUND_KEY_OVERHEAT_STATE)) {
            return OverheatState.NORMAL;
        }
        return OverheatState.fromName(tag.getString(COMPOUND_KEY_OVERHEAT_STATE));
    }

    private static int readStoredHeat(CompoundTag tag, OverheatState savedOverheatState) {
        if (!tag.contains(COMPOUND_KEY_STORED_HEAT)) {
            return inferStoredHeat(savedOverheatState);
        }
        return AirCompressorThermal.clampStoredHeat(tag.getInt(COMPOUND_KEY_STORED_HEAT));
    }

    private static CoolantEfficiency readCoolantEfficiency(CompoundTag tag) {
        if (!tag.contains(COMPOUND_KEY_COOLANT_EFFICIENCY)) {
            return CoolantEfficiency.NONE;
        }
        return CoolantEfficiency.fromName(tag.getString(COMPOUND_KEY_COOLANT_EFFICIENCY));
    }

    private static int inferStoredHeat(OverheatState savedOverheatState) {
        if (savedOverheatState == OverheatState.NORMAL) {
            return 0;
        }

        if (savedOverheatState == OverheatState.MELTDOWN) {
            return AirCompressorThermal.getMaxStoredHeat();
        }

        int overheatThreshold = AirCompressorThermal.getNextOverheatThreshold();
        return AirCompressorThermal.clampStoredHeat(savedOverheatState.ordinal() * overheatThreshold + overheatThreshold / 2);
    }

    CoolantEfficiency getCoolantEfficiency() {
        return coolantEfficiency;
    }

    boolean setCoolantEfficiency(CoolantEfficiency newCoolantEfficiency) {
        if (coolantEfficiency == newCoolantEfficiency) {
            return false;
        }

        coolantEfficiency = newCoolantEfficiency;
        return true;
    }

    int getStoredHeat() {
        return storedHeat;
    }

    void setStoredHeat(int newStoredHeat) {
        storedHeat = AirCompressorThermal.clampStoredHeat(newStoredHeat);
        overheatState = AirCompressorThermal.getOverheatState(storedHeat);
    }

    OverheatState getOverheatState() {
        return overheatState;
    }

    WorkState getWorkState() {
        return workState;
    }

    void setWorkState(WorkState workState) {
        this.workState = workState;
    }

    void loadFromItem(ItemStack stack) {
        if (!stack.has(CCBDataComponents.COMPRESSOR_STORED_HEAT)) {
            setStoredHeat(inferStoredHeat(OverheatState.fromItem(stack)));
            return;
        }

        setStoredHeat(stack.getOrDefault(CCBDataComponents.COMPRESSOR_STORED_HEAT, 0));
    }

    void saveToItem(ItemStack stack) {
        stack.set(CCBDataComponents.COMPRESSOR_OVERHEAT_STATE, overheatState.getSerializedName());
        stack.set(CCBDataComponents.COMPRESSOR_STORED_HEAT, AirCompressorThermal.clampStoredHeat(storedHeat));
    }

    void write(CompoundTag tag, boolean clientPacket) {
        tag.putString(COMPOUND_KEY_OVERHEAT_STATE, overheatState.getSerializedName());
        if (clientPacket) {
            return;
        }

        tag.putInt(COMPOUND_KEY_STORED_HEAT, AirCompressorThermal.clampStoredHeat(storedHeat));
        tag.putString(COMPOUND_KEY_COOLANT_EFFICIENCY, coolantEfficiency.getSerializedName());
    }

    void read(CompoundTag tag, boolean clientPacket) {
        OverheatState savedOverheatState = readOverheatState(tag);
        overheatState = savedOverheatState;
        if (clientPacket) {
            return;
        }

        storedHeat = readStoredHeat(tag, savedOverheatState);
        overheatState = AirCompressorThermal.getOverheatState(storedHeat);
        coolantEfficiency = readCoolantEfficiency(tag);
    }

    record WorkState(@Nullable PressurizationRecipe recipe, long accumulatedWork) {
        static final WorkState EMPTY = new WorkState(null, 0);

        WorkState {
            accumulatedWork = Math.max(0, accumulatedWork);
        }
    }
}
