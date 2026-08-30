package net.ty.createcraftedbeginning.content.breezes.breezecooler;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlockEntity.CoolantType;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.coolerstates.BaseCoolerState;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.coolerstates.ChilledCoolerState;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.coolerstates.CreativeCoolerState;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.coolerstates.InactiveCoolerState;
import net.ty.createcraftedbeginning.foundation.CCBNbtUtils;
import net.ty.createcraftedbeginning.foundation.CCBMathUtils;

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

    private static BaseCoolerState readState(CompoundTag compoundTag) {
        CompoundTag stateData = CCBNbtUtils.getCompound(compoundTag, STATE_DATA);
        CoolantType coolantType = CoolantType.fromTag(compoundTag, STATE_TYPE, CoolantType.NONE);
        boolean isCreative = CCBNbtUtils.getBooleanOrDefault(stateData, IS_CREATIVE, false);
        int remainingTime = CCBMathUtils.clampNonNegative(CCBNbtUtils.getIntOrDefault(stateData, REMAINING_TIME, 0), BreezeCoolerBlockEntity.getMaxCoolantCapacity());
        if (isCreative && coolantType != CoolantType.NONE) {
            return new CreativeCoolerState(coolantType);
        }
        return switch (coolantType) {
            case NORMAL -> remainingTime > 0 ? new ChilledCoolerState(remainingTime, false) : new InactiveCoolerState();
            case NONE -> new InactiveCoolerState();
        };
    }

    void write(BreezeCoolerBlockEntity cooler, CompoundTag compoundTag) {
        BaseCoolerState coolerState = cooler.getCurrentState();
        CompoundTag stateData = new CompoundTag();
        coolerState.save(stateData);
        CCBNbtUtils.putTag(compoundTag, STATE_DATA, stateData);
        CCBNbtUtils.putString(compoundTag, STATE_TYPE, coolerState.getCoolantType().name());
        CCBNbtUtils.putBoolean(compoundTag, GOGGLES, cooler.hasGoggles());
        CCBNbtUtils.putBoolean(compoundTag, TRAIN_HAT, cooler.hasTrainHat());
    }

    void read(BreezeCoolerBlockEntity cooler, CompoundTag compoundTag) {
        if (CCBNbtUtils.contains(compoundTag, STATE_DATA, Tag.TAG_COMPOUND)) {
            cooler.setCoolerStateFromSerialization(readState(compoundTag));
        }
        if (CCBNbtUtils.contains(compoundTag, GOGGLES, Tag.TAG_BYTE)) {
            cooler.setGogglesFromSerialization(CCBNbtUtils.getBoolean(compoundTag, GOGGLES));
        }
        if (!CCBNbtUtils.contains(compoundTag, TRAIN_HAT, Tag.TAG_BYTE)) {
            return;
        }

        cooler.setTrainHatFromSerialization(CCBNbtUtils.getBoolean(compoundTag, TRAIN_HAT));
    }
}
