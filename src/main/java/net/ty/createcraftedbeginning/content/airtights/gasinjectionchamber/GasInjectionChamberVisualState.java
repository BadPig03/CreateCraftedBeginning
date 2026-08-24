package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

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

    Optional<Integer> readCloud(CompoundTag compoundTag, boolean clientPacket) {
        if (!clientPacket || !compoundTag.contains(COMPOUND_KEY_CLOUD)) {
            return Optional.empty();
        }
        if (!compoundTag.contains(COMPOUND_KEY_CLOUD_COLOR)) {
            return Optional.of(0xFFFFFFFF);
        }
        return Optional.of(compoundTag.getInt(COMPOUND_KEY_CLOUD_COLOR));
    }
}
