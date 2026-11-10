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
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Internal
public final class BreezeCoolerSerialization {
    @Internal
    public static final String COMPOUND_KEY_STATE_TYPE = "StateType";
    @Internal
    public static final String COMPOUND_KEY_STATE_DATA = "StateData";
    static final String COMPOUND_KEY_GOGGLES = "Goggles";
    static final String COMPOUND_KEY_TRAIN_HAT = "TrainHat";

    private static BaseCoolerState readState(CompoundTag compoundTag) {
        CompoundTag stateData = compoundTag.getCompound(COMPOUND_KEY_STATE_DATA);
        CoolantType coolantType = CoolantType.fromTag(compoundTag, COMPOUND_KEY_STATE_TYPE, CoolantType.NONE);
        boolean isCreative = NbtValues.getBooleanOrDefault(stateData, BaseCoolerState.COMPOUND_KEY_IS_CREATIVE, false);
        int remainingTime = Mth.clamp(NbtValues.getIntOrDefault(stateData, BaseCoolerState.COMPOUND_KEY_REMAINING_TIME, 0), 0, BreezeCoolerBlockEntity.getMaxCoolantCapacity());
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

    void write(BreezeCoolerBlockEntity cooler, CompoundTag compoundTag) {
        BaseCoolerState coolerState = cooler.getCurrentState();
        CompoundTag stateData = new CompoundTag();
        coolerState.save(stateData);
        compoundTag.put(COMPOUND_KEY_STATE_DATA, stateData);
        compoundTag.putString(COMPOUND_KEY_STATE_TYPE, coolerState.getCoolantType().name());
        compoundTag.putBoolean(COMPOUND_KEY_GOGGLES, cooler.hasGoggles());
        compoundTag.putBoolean(COMPOUND_KEY_TRAIN_HAT, cooler.hasTrainHat());
    }

    void read(BreezeCoolerBlockEntity cooler, CompoundTag compoundTag) {
        if (compoundTag.contains(COMPOUND_KEY_STATE_DATA, Tag.TAG_COMPOUND)) {
            cooler.setCoolerStateFromSerialization(readState(compoundTag));
        }
        if (compoundTag.contains(COMPOUND_KEY_GOGGLES, Tag.TAG_BYTE)) {
            cooler.setGogglesFromSerialization(compoundTag.getBoolean(COMPOUND_KEY_GOGGLES));
        }
        if (!compoundTag.contains(COMPOUND_KEY_TRAIN_HAT, Tag.TAG_BYTE)) {
            return;
        }

        cooler.setTrainHatFromSerialization(compoundTag.getBoolean(COMPOUND_KEY_TRAIN_HAT));
    }
}
