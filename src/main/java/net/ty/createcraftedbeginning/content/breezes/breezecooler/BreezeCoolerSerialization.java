package net.ty.createcraftedbeginning.content.breezes.breezecooler;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity.CoolantType;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.coolerstates.BaseCoolerState;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.coolerstates.ChilledCoolerState;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.coolerstates.CreativeCoolerState;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.coolerstates.InactiveCoolerState;
import net.ty.createcraftedbeginning.foundation.NbtValues;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class BreezeCoolerSerialization {
    private static final String STATE_TYPE = "StateType";
    private static final String STATE_DATA = "StateData";
    private static final String GOGGLES = "Goggles";
    private static final String TRAIN_HAT = "TrainHat";
    private static final String IS_CREATIVE = "isCreative";
    private static final String REMAINING_TIME = "RemainingTime";

    void write(BreezeCoolerBlockEntity cooler, CompoundTag compoundTag) {
        BaseCoolerState coolerState = cooler.getCurrentState();
        CompoundTag stateData = new CompoundTag();
        coolerState.save(stateData);
        compoundTag.put(STATE_DATA, stateData);
        compoundTag.putString(STATE_TYPE, coolerState.getCoolantType().name());
        compoundTag.putBoolean(GOGGLES, cooler.hasGoggles());
        compoundTag.putBoolean(TRAIN_HAT, cooler.hasTrainHat());
    }

    void read(BreezeCoolerBlockEntity cooler, CompoundTag compoundTag) {
        if (compoundTag.contains(STATE_DATA, Tag.TAG_COMPOUND)) {
            cooler.setCoolerStateFromSerialization(readState(compoundTag));
        }
        if (compoundTag.contains(GOGGLES, Tag.TAG_BYTE)) {
            cooler.setGogglesFromSerialization(compoundTag.getBoolean(GOGGLES));
        }
        if (!compoundTag.contains(TRAIN_HAT, Tag.TAG_BYTE)) {
            return;
        }

        cooler.setTrainHatFromSerialization(compoundTag.getBoolean(TRAIN_HAT));
    }

    private static BaseCoolerState readState(CompoundTag compoundTag) {
        CompoundTag stateData = compoundTag.getCompound(STATE_DATA);
        CoolantType coolantType = CoolantType.fromTag(compoundTag, STATE_TYPE, CoolantType.NONE);
        boolean isCreative = NbtValues.getBooleanOrDefault(stateData, IS_CREATIVE, false);
        int remainingTime = Mth.clamp(NbtValues.getIntOrDefault(stateData, REMAINING_TIME, 0), 0, BreezeCoolerBlockEntity.getMaxCoolantCapacity());
        if (isCreative && coolantType != CoolantType.NONE) {
            return new CreativeCoolerState(coolantType);
        }

        return switch (coolantType) {
            case NORMAL -> {
                if (remainingTime > 0) {
                    yield new ChilledCoolerState(remainingTime, false);
                }

                yield new InactiveCoolerState();
            }
            case NONE -> new InactiveCoolerState();
        };
    }
}
