package net.ty.createcraftedbeginning.content.airtights.aircompressor;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.coolantshandlers.CoolantEfficiency;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirCompressorState {
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
        if (!compoundTag.contains(CLIENT_KEY_OVERHEAT_STATE)) {
            return OverheatState.NORMAL;
        }
        return OverheatState.fromName(compoundTag.getString(CLIENT_KEY_OVERHEAT_STATE));
    }

    private static int readStoredHeat(CompoundTag compoundTag) {
        return AirCompressorThermal.clampStoredHeat(compoundTag.getInt(COMPOUND_KEY_STORED_HEAT));
    }

    private static WorkState readWorkState(CompoundTag compoundTag) {
        if (!compoundTag.contains(COMPOUND_KEY_WORK_STATE)) {
            return WorkState.EMPTY;
        }

        CompoundTag workTag = compoundTag.getCompound(COMPOUND_KEY_WORK_STATE);
        ResourceLocation recipeId = ResourceLocation.tryParse(workTag.getString(COMPOUND_KEY_WORK_RECIPE));
        long accumulatedWork = workTag.getLong(COMPOUND_KEY_ACCUMULATED_WORK);
        return new WorkState(recipeId, accumulatedWork);
    }

    private static void writeWorkState(CompoundTag compoundTag, WorkState workState) {
        ResourceLocation recipeId = workState.recipeId();
        if (recipeId == null) {
            compoundTag.remove(COMPOUND_KEY_WORK_STATE);
            return;
        }

        CompoundTag workTag = new CompoundTag();
        workTag.putString(COMPOUND_KEY_WORK_RECIPE, recipeId.toString());
        workTag.putLong(COMPOUND_KEY_ACCUMULATED_WORK, workState.accumulatedWork());
        compoundTag.put(COMPOUND_KEY_WORK_STATE, workTag);
    }

    public CoolantEfficiency getCoolantEfficiency() {
        return coolantEfficiency;
    }

    public void setCoolantEfficiency(CoolantEfficiency newCoolantEfficiency) {
        coolantEfficiency = newCoolantEfficiency;
    }

    public int getStoredHeat() {
        return storedHeat;
    }

    public void setStoredHeat(int newStoredHeat) {
        storedHeat = AirCompressorThermal.clampStoredHeat(newStoredHeat);
        overheatState = AirCompressorThermal.getOverheatState(storedHeat);
    }

    public OverheatState getOverheatState() {
        return overheatState;
    }

    public WorkState getWorkState() {
        return workState;
    }

    public void setWorkState(WorkState workState) {
        this.workState = workState;
    }

    public void loadFromItem(ItemStack stack) {
        setStoredHeat(stack.getOrDefault(CCBDataComponents.COMPRESSOR_STORED_HEAT, 0));
    }

    public void saveToItem(ItemStack stack) {
        stack.set(CCBDataComponents.COMPRESSOR_STORED_HEAT, AirCompressorThermal.clampStoredHeat(storedHeat));
    }

    public void write(CompoundTag compoundTag, boolean clientPacket) {
        if (clientPacket) {
            compoundTag.putString(CLIENT_KEY_OVERHEAT_STATE, overheatState.getSerializedName());
            return;
        }

        compoundTag.putInt(COMPOUND_KEY_STORED_HEAT, AirCompressorThermal.clampStoredHeat(storedHeat));
        writeWorkState(compoundTag, workState);
    }

    public void read(CompoundTag compoundTag, boolean clientPacket) {
        if (clientPacket) {
            overheatState = readClientOverheatState(compoundTag);
            return;
        }

        setStoredHeat(readStoredHeat(compoundTag));
        coolantEfficiency = CoolantEfficiency.NONE;
        workState = readWorkState(compoundTag);
    }

    public record WorkState(@Nullable ResourceLocation recipeId, long accumulatedWork) {
        public static final WorkState EMPTY = new WorkState(null, 0);

        public WorkState {
            accumulatedWork = Math.max(0, accumulatedWork);
            if (recipeId == null || accumulatedWork == 0) {
                recipeId = null;
                accumulatedWork = 0;
            }
        }

        public boolean matches(ResourceLocation recipeId) {
            return recipeId.equals(this.recipeId);
        }

        public boolean isEmpty() {
            return recipeId == null;
        }
    }
}
