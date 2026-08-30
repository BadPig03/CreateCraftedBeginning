package net.ty.createcraftedbeginning.content.airtights.aircompressor;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.coolantshandlers.CoolantEfficiency;
import net.ty.createcraftedbeginning.foundation.CCBNbtUtils;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirCompressorState {
    private static final String COMPOUND_KEY_STORED_HEAT = "StoredHeat";
    private static final String CLIENT_KEY_OVERHEAT_STATE = "OverheatState";
    private static final String COMPOUND_KEY_WORK_STATE = "WorkState";
    private static final String COMPOUND_KEY_WORK_RECIPE = "Recipe";
    private static final String COMPOUND_KEY_ACCUMULATED_WORK = "AccumulatedWork";

    private CoolantEfficiency coolantEfficiency = CoolantEfficiency.NONE;
    private int storedHeat;
    private OverheatState overheatState = OverheatState.NORMAL;
    private WorkState workState = WorkState.EMPTY;

    private static OverheatState readClientOverheatState(CompoundTag compoundTag) {
        String serializedState = CCBNbtUtils.getStringOrDefault(compoundTag, CLIENT_KEY_OVERHEAT_STATE, OverheatState.NORMAL.getSerializedName());
        return OverheatState.fromName(serializedState);
    }

    private static int readStoredHeat(CompoundTag compoundTag) {
        return AirCompressorThermal.clampStoredHeat(CCBNbtUtils.getInt(compoundTag, COMPOUND_KEY_STORED_HEAT));
    }

    private static WorkState readWorkState(CompoundTag compoundTag) {
        if (!CCBNbtUtils.contains(compoundTag, COMPOUND_KEY_WORK_STATE)) {
            return WorkState.EMPTY;
        }

        CompoundTag workTag = CCBNbtUtils.getCompound(compoundTag, COMPOUND_KEY_WORK_STATE);
        ResourceLocation recipeId = ResourceLocation.tryParse(CCBNbtUtils.getString(workTag, COMPOUND_KEY_WORK_RECIPE));
        long accumulatedWork = CCBNbtUtils.getLong(workTag, COMPOUND_KEY_ACCUMULATED_WORK);
        return new WorkState(recipeId, accumulatedWork);
    }

    private static void writeWorkState(CompoundTag compoundTag, WorkState workState) {
        ResourceLocation recipeId = workState.recipeId();
        if (recipeId == null) {
            CCBNbtUtils.remove(compoundTag, COMPOUND_KEY_WORK_STATE);
            return;
        }

        CompoundTag workTag = new CompoundTag();
        CCBNbtUtils.putString(workTag, COMPOUND_KEY_WORK_RECIPE, recipeId.toString());
        CCBNbtUtils.putLong(workTag, COMPOUND_KEY_ACCUMULATED_WORK, workState.accumulatedWork());
        CCBNbtUtils.putTag(compoundTag, COMPOUND_KEY_WORK_STATE, workTag);
    }

    CoolantEfficiency getCoolantEfficiency() {
        return coolantEfficiency;
    }

    void setCoolantEfficiency(CoolantEfficiency newCoolantEfficiency) {
        coolantEfficiency = newCoolantEfficiency;
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
        setStoredHeat(stack.getOrDefault(CCBDataComponents.COMPRESSOR_STORED_HEAT, 0));
    }

    void saveToItem(ItemStack stack) {
        stack.set(CCBDataComponents.COMPRESSOR_STORED_HEAT, AirCompressorThermal.clampStoredHeat(storedHeat));
    }

    void write(CompoundTag compoundTag, boolean clientPacket) {
        if (clientPacket) {
            CCBNbtUtils.putString(compoundTag, CLIENT_KEY_OVERHEAT_STATE, overheatState.getSerializedName());
            return;
        }

        CCBNbtUtils.putInt(compoundTag, COMPOUND_KEY_STORED_HEAT, AirCompressorThermal.clampStoredHeat(storedHeat));
        writeWorkState(compoundTag, workState);
    }

    void read(CompoundTag compoundTag, boolean clientPacket) {
        if (clientPacket) {
            overheatState = readClientOverheatState(compoundTag);
            return;
        }

        setStoredHeat(readStoredHeat(compoundTag));
        coolantEfficiency = CoolantEfficiency.NONE;
        workState = readWorkState(compoundTag);
    }

    record WorkState(@Nullable ResourceLocation recipeId, long accumulatedWork) {
        private static final WorkState EMPTY = new WorkState(null, 0);

        WorkState {
            accumulatedWork = Math.max(0, accumulatedWork);
            if (recipeId == null || accumulatedWork == 0) {
                recipeId = null;
                accumulatedWork = 0;
            }
        }

        boolean matches(ResourceLocation recipeId) {
            return recipeId.equals(this.recipeId);
        }
    }
}
