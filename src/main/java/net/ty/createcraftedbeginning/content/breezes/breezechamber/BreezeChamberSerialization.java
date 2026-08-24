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
public final class BreezeChamberSerialization {
    private static final String STATE_TYPE = "StateType";
    private static final String STATE_DATA = "StateData";
    private static final String GOGGLES = "Goggles";
    private static final String TRAIN_HAT = "TrainHat";
    private static final String GAS_PROCESSING = "GasProcessing";
    private static final String IS_CREATIVE = "isCreative";
    private static final String REMAINING_TIME = "RemainingTime";

    public static BaseChamberState stateForItem(int remainingTime, boolean isCreative) {
        return createState(chargerTypeForTime(remainingTime), remainingTime, isCreative);
    }

    private static BaseChamberState readState(CompoundTag compoundTag) {
        CompoundTag stateData = compoundTag.getCompound(STATE_DATA);
        ChargerType chargerType = ChargerType.fromTag(compoundTag, STATE_TYPE, ChargerType.NONE);
        boolean isCreative = stateData.contains(IS_CREATIVE, Tag.TAG_BYTE) && stateData.getBoolean(IS_CREATIVE);
        int maxWindCapacity = BreezeChamberBlockEntity.getMaxWindCapacity();
        int remainingTime = stateData.contains(REMAINING_TIME, Tag.TAG_ANY_NUMERIC) ? Mth.clamp(stateData.getInt(REMAINING_TIME), -maxWindCapacity, maxWindCapacity) : 0;
        return createState(chargerType, remainingTime, isCreative);
    }

    private static BaseChamberState createState(ChargerType chargerType, int remainingTime, boolean isCreative) {
        if (isCreative && chargerType != ChargerType.NONE) {
            return new CreativeChamberState(chargerType);
        }
        return switch (chargerType) {
            case NORMAL -> remainingTime > 0 ? new GaleChamberState(remainingTime, false) : new InactiveChamberState();
            case BAD -> remainingTime < 0 ? new IllChamberState(remainingTime, false) : new InactiveChamberState();
            case NONE -> new InactiveChamberState();
        };
    }

    private static ChargerType chargerTypeForTime(int remainingTime) {
        if (remainingTime > 0) {
            return ChargerType.NORMAL;
        }

        if (remainingTime < 0) {
            return ChargerType.BAD;
        }
        return ChargerType.NONE;
    }

    public void write(BreezeChamberBlockEntity chamber, CompoundTag compoundTag) {
        BaseChamberState chamberState = chamber.getChamberStateInternal();
        CompoundTag stateTag = new CompoundTag();
        chamberState.save(stateTag);
        compoundTag.put(STATE_DATA, stateTag);
        compoundTag.putString(STATE_TYPE, chamberState.getChargerType().name());
        compoundTag.putBoolean(GOGGLES, chamber.hasGoggles());
        compoundTag.putBoolean(TRAIN_HAT, chamber.hasTrainHat());
        CompoundTag gasProcessingTag = new CompoundTag();
        chamber.getGasProcessorInternal().writePendingProcessing(gasProcessingTag);
        compoundTag.put(GAS_PROCESSING, gasProcessingTag);
    }

    public void read(BreezeChamberBlockEntity chamber, CompoundTag compoundTag) {
        if (compoundTag.contains(STATE_DATA, Tag.TAG_COMPOUND)) {
            chamber.setChamberStateFromSerialization(readState(compoundTag));
        }
        chamber.getGasProcessorInternal().readPendingProcessing(compoundTag.contains(GAS_PROCESSING, Tag.TAG_COMPOUND) ? compoundTag.getCompound(GAS_PROCESSING) : new CompoundTag());
        if (compoundTag.contains(GOGGLES, Tag.TAG_BYTE)) {
            chamber.setGogglesFromSerialization(compoundTag.getBoolean(GOGGLES));
        }
        if (!compoundTag.contains(TRAIN_HAT, Tag.TAG_BYTE)) {
            return;
        }

        chamber.setTrainHatFromSerialization(compoundTag.getBoolean(TRAIN_HAT));
    }

    public void saveToItem(BreezeChamberBlockEntity chamber, ItemStack stack) {
        BaseChamberState chamberState = chamber.getChamberStateInternal();
        stack.set(CCBDataComponents.BREEZE_TIME, chamberState.getRemainingTime());
        stack.set(CCBDataComponents.BREEZE_CREATIVE, chamberState.isCreative());
    }
}
