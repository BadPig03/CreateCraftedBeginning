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

    private static BaseCoolerState readState(CompoundTag tag) {
        CompoundTag stateData = tag.getCompound(STATE_DATA);
        CoolantType type = CoolantType.fromTag(tag, STATE_TYPE, CoolantType.NONE);
        boolean creative = stateData.contains(IS_CREATIVE, Tag.TAG_BYTE) && stateData.getBoolean(IS_CREATIVE);
        int remainingTime = stateData.contains(REMAINING_TIME, Tag.TAG_ANY_NUMERIC) ? Mth.clamp(stateData.getInt(REMAINING_TIME), 0, BreezeCoolerBlockEntity.getMaxCoolantCapacity()) : 0;
        if (creative && type != CoolantType.NONE) {
            return new CreativeCoolerState(type);
        }
        return switch (type) {
            case NORMAL -> remainingTime > 0 ? new ChilledCoolerState(remainingTime, false) : new InactiveCoolerState();
            case NONE -> new InactiveCoolerState();
        };
    }

    void write(BreezeCoolerBlockEntity cooler, CompoundTag tag) {
        BaseCoolerState state = cooler.getCurrentState();
        CompoundTag stateTag = new CompoundTag();
        state.save(stateTag);
        tag.put(STATE_DATA, stateTag);
        tag.putString(STATE_TYPE, state.getCoolantType().name());
        tag.putBoolean(GOGGLES, cooler.hasGoggles());
        tag.putBoolean(TRAIN_HAT, cooler.hasTrainHat());
    }

    void read(BreezeCoolerBlockEntity cooler, CompoundTag tag) {
        if (tag.contains(STATE_DATA, Tag.TAG_COMPOUND)) {
            cooler.setCoolerStateFromSerialization(readState(tag));
        }
        if (tag.contains(GOGGLES, Tag.TAG_BYTE)) {
            cooler.setGogglesFromSerialization(tag.getBoolean(GOGGLES));
        }
        if (!tag.contains(TRAIN_HAT, Tag.TAG_BYTE)) {
            return;
        }

        cooler.setTrainHatFromSerialization(tag.getBoolean(TRAIN_HAT));
    }
}
