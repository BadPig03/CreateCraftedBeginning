package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.ty.createcraftedbeginning.foundation.NbtValues;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.OptionalInt;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasInjectionChamberVisualState {
    private static final String COMPOUND_KEY_CLOUD = "Cloud";
    private static final String COMPOUND_KEY_CLOUD_COLOR = "CloudColor";

    private int cloudColor = 0xFFFFFFFF;
    private boolean sendCloud;

    void queueCloud(int color) {
        cloudColor = color;
        sendCloud = true;
    }

    void writeCloud(CompoundTag compoundTag, boolean clientPacket) {
        if (!sendCloud || !clientPacket) {
            return;
        }

        compoundTag.putBoolean(COMPOUND_KEY_CLOUD, true);
        compoundTag.putInt(COMPOUND_KEY_CLOUD_COLOR, cloudColor);
        sendCloud = false;
    }

    OptionalInt readCloud(CompoundTag compoundTag, boolean clientPacket) {
        if (!clientPacket || !compoundTag.contains(COMPOUND_KEY_CLOUD)) {
            return OptionalInt.empty();
        }

        return OptionalInt.of(NbtValues.getIntOrDefault(compoundTag, COMPOUND_KEY_CLOUD_COLOR, 0xFFFFFFFF));
    }
}
