package net.ty.createcraftedbeginning.content.breezes.breezechamber;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity.ChargerType;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates.BaseChamberState;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates.CreativeChamberState;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates.GaleChamberState;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates.IllChamberState;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates.InactiveChamberState;
import net.ty.createcraftedbeginning.foundation.CCBMathUtils;
import net.ty.createcraftedbeginning.foundation.CCBNbtUtils;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BreezeChamberSerialization {
    private static final String STATE_TYPE = "StateType";
    private static final String STATE_DATA = "StateData";
    private static final String GOGGLES = "Goggles";
    private static final String TRAIN_HAT = "TrainHat";
    private static final String GAS_PROCESSING = "GasProcessing";
    private static final String IS_CREATIVE = "isCreative";
    private static final String REMAINING_TIME = "RemainingTime";

    static BaseChamberState stateForItem(int remainingTime, boolean isCreative) {
        return createState(chargerTypeForTime(remainingTime), remainingTime, isCreative);
    }

    private static BaseChamberState readState(CompoundTag compoundTag) {
        CompoundTag stateData = CCBNbtUtils.getCompound(compoundTag, STATE_DATA);
        ChargerType chargerType = ChargerType.fromTag(compoundTag, STATE_TYPE);
        boolean isCreative = CCBNbtUtils.getBooleanOrDefault(stateData, IS_CREATIVE, false);
        int maxWindCapacity = BreezeChamberBlockEntity.getMaxWindCapacity();
        int remainingTime = CCBMathUtils.clampMagnitude(CCBNbtUtils.getIntOrDefault(stateData, REMAINING_TIME, 0), maxWindCapacity);
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

    void write(BreezeChamberBlockEntity chamber, CompoundTag compoundTag) {
        BaseChamberState chamberState = chamber.getChamberStateInternal();
        CompoundTag stateTag = new CompoundTag();
        chamberState.save(stateTag);
        CCBNbtUtils.putTag(compoundTag, STATE_DATA, stateTag);
        CCBNbtUtils.putString(compoundTag, STATE_TYPE, chamberState.getChargerType().name());
        CCBNbtUtils.putBoolean(compoundTag, GOGGLES, chamber.hasGoggles());
        CCBNbtUtils.putBoolean(compoundTag, TRAIN_HAT, chamber.hasTrainHat());
        CompoundTag gasProcessingTag = new CompoundTag();
        chamber.getGasProcessorInternal().writePendingProcessing(gasProcessingTag);
        CCBNbtUtils.putTag(compoundTag, GAS_PROCESSING, gasProcessingTag);
    }

    void read(BreezeChamberBlockEntity chamber, CompoundTag compoundTag) {
        if (CCBNbtUtils.contains(compoundTag, STATE_DATA, Tag.TAG_COMPOUND)) {
            chamber.setChamberStateFromSerialization(readState(compoundTag));
        }
        chamber.getGasProcessorInternal().readPendingProcessing(CCBNbtUtils.getCompoundOrEmpty(compoundTag, GAS_PROCESSING));
        if (CCBNbtUtils.contains(compoundTag, GOGGLES, Tag.TAG_BYTE)) {
            chamber.setGogglesFromSerialization(CCBNbtUtils.getBoolean(compoundTag, GOGGLES));
        }
        if (!CCBNbtUtils.contains(compoundTag, TRAIN_HAT, Tag.TAG_BYTE)) {
            return;
        }

        chamber.setTrainHatFromSerialization(CCBNbtUtils.getBoolean(compoundTag, TRAIN_HAT));
    }

    void saveToItem(BreezeChamberBlockEntity chamber, ItemStack stack) {
        BaseChamberState chamberState = chamber.getChamberStateInternal();
        stack.set(CCBDataComponents.BREEZE_TIME, chamberState.getRemainingTime());
        stack.set(CCBDataComponents.BREEZE_CREATIVE, chamberState.isCreative());
    }
}
