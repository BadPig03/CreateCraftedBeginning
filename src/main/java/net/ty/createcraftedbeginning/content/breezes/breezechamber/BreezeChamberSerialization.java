package net.ty.createcraftedbeginning.content.breezes.breezechamber;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberBlockEntity.ChargerType;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates.BaseChamberState;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates.CreativeChamberState;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates.GaleChamberState;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates.IllChamberState;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.chamberstates.InactiveChamberState;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
import net.ty.createcraftedbeginning.foundation.NbtValues;

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

    private static BaseChamberState readState(CompoundTag compoundTag) {
        CompoundTag stateData = compoundTag.getCompound(STATE_DATA);
        ChargerType chargerType = ChargerType.fromTag(compoundTag, STATE_TYPE);
        boolean isCreative = NbtValues.getBooleanOrDefault(stateData, IS_CREATIVE, false);
        int maxWindCapacity = BreezeChamberBlockEntity.getMaxWindCapacity();
        int remainingTime = BoundedMath.clampMagnitude(NbtValues.getIntOrDefault(stateData, REMAINING_TIME, 0), maxWindCapacity);
        return createState(chargerType, remainingTime, isCreative);
    }

    private static BaseChamberState createState(ChargerType chargerType, int remainingTime, boolean isCreative) {
        if (isCreative && chargerType != ChargerType.NONE) {
            return new CreativeChamberState(chargerType);
        }

        return switch (chargerType) {
            case NORMAL -> {
                if (remainingTime > 0) {
                    yield new GaleChamberState(remainingTime, false);
                }

                yield new InactiveChamberState();
            }
            case BAD -> {
                if (remainingTime < 0) {
                    yield new IllChamberState(remainingTime, false);
                }

                yield new InactiveChamberState();
            }
            case NONE -> new InactiveChamberState();
        };
    }

    void write(BreezeChamberBlockEntity chamber, CompoundTag compoundTag) {
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

    void read(BreezeChamberBlockEntity chamber, CompoundTag compoundTag) {
        if (compoundTag.contains(STATE_DATA, Tag.TAG_COMPOUND)) {
            chamber.setChamberStateFromSerialization(readState(compoundTag));
        }
        chamber.getGasProcessorInternal().readPendingProcessing(NbtValues.getCompoundOrEmpty(compoundTag, GAS_PROCESSING));
        if (compoundTag.contains(GOGGLES, Tag.TAG_BYTE)) {
            chamber.setGogglesFromSerialization(compoundTag.getBoolean(GOGGLES));
        }
        if (!compoundTag.contains(TRAIN_HAT, Tag.TAG_BYTE)) {
            return;
        }

        chamber.setTrainHatFromSerialization(compoundTag.getBoolean(TRAIN_HAT));
    }

}
