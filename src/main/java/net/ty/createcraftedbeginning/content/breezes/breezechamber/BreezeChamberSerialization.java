package net.ty.createcraftedbeginning.content.breezes.breezechamber;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity.ChargerType;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates.BaseChamberState;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates.CreativeChamberState;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates.GaleChamberState;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates.IllChamberState;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates.InactiveChamberState;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BreezeChamberSerialization {
    private static final String STATE_TYPE = "StateType";
    private static final String STATE_DATA = "StateData";
    private static final String GOGGLES = "Goggles";
    private static final String TRAIN_HAT = "TrainHat";
    private static final String IS_CREATIVE = "isCreative";
    private static final String REMAINING_TIME = "RemainingTime";

    static BaseChamberState stateForItem(int time, boolean creative) {
        return createState(chargerTypeForTime(time), time, creative);
    }

    private static BaseChamberState readState(CompoundTag tag) {
        CompoundTag stateData = tag.getCompound(STATE_DATA);
        ChargerType stateType = ChargerType.fromTag(tag, STATE_TYPE, ChargerType.NONE);
        boolean creative = stateData.contains(IS_CREATIVE, Tag.TAG_BYTE) && stateData.getBoolean(IS_CREATIVE);
        int maxWindCapacity = BreezeChamberBlockEntity.getMaxWindCapacity();
        int remainingTime = stateData.contains(REMAINING_TIME, Tag.TAG_ANY_NUMERIC) ? Mth.clamp(stateData.getInt(REMAINING_TIME), -maxWindCapacity, maxWindCapacity) : 0;
        return createState(stateType, remainingTime, creative);
    }

    private static BaseChamberState createState(ChargerType chargerType, int remainingTime, boolean creative) {
        if (creative && chargerType != ChargerType.NONE) {
            return new CreativeChamberState(chargerType);
        }
        return switch (chargerType) {
            case NORMAL -> remainingTime > 0 ? new GaleChamberState(remainingTime, false) : new InactiveChamberState();
            case BAD -> remainingTime < 0 ? new IllChamberState(remainingTime, false) : new InactiveChamberState();
            case NONE -> new InactiveChamberState();
        };
    }

    private static ChargerType chargerTypeForTime(int time) {
        if (time > 0) {
            return ChargerType.NORMAL;
        }
        if (time < 0) {
            return ChargerType.BAD;
        }
        return ChargerType.NONE;
    }

    void write(BreezeChamberBlockEntity chamber, CompoundTag tag) {
        BaseChamberState state = chamber.getChamberStateInternal();
        CompoundTag stateTag = new CompoundTag();
        state.save(stateTag);
        tag.put(STATE_DATA, stateTag);
        tag.putString(STATE_TYPE, state.getChargerType().name());
        tag.putBoolean(GOGGLES, chamber.hasGoggles());
        tag.putBoolean(TRAIN_HAT, chamber.hasTrainHat());
    }

    void read(BreezeChamberBlockEntity chamber, CompoundTag tag) {
        if (tag.contains(STATE_DATA, Tag.TAG_COMPOUND)) {
            chamber.setChamberStateFromSerialization(readState(tag));
        }
        if (tag.contains(GOGGLES, Tag.TAG_BYTE)) {
            chamber.setGogglesFromSerialization(tag.getBoolean(GOGGLES));
        }
        if (tag.contains(TRAIN_HAT, Tag.TAG_BYTE)) {
            chamber.setTrainHatFromSerialization(tag.getBoolean(TRAIN_HAT));
        }
    }

    void saveToItem(BreezeChamberBlockEntity chamber, ItemStack stack) {
        BaseChamberState state = chamber.getChamberStateInternal();
        stack.set(CCBDataComponents.BREEZE_TIME, state.getRemainingTime());
        stack.set(CCBDataComponents.BREEZE_CREATIVE, state.isCreative());
    }
}
