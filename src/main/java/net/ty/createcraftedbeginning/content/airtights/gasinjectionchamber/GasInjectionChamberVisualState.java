package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.ty.createcraftedbeginning.foundation.CCBNbtUtils;

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

        CCBNbtUtils.putBoolean(compoundTag, COMPOUND_KEY_CLOUD, true);
        CCBNbtUtils.putInt(compoundTag, COMPOUND_KEY_CLOUD_COLOR, cloudColor);
        sendCloud = false;
    }

    Optional<Integer> readCloud(CompoundTag compoundTag, boolean clientPacket) {
        if (!clientPacket || !CCBNbtUtils.contains(compoundTag, COMPOUND_KEY_CLOUD)) {
            return Optional.empty();
        }
        return Optional.of(CCBNbtUtils.getIntOrDefault(compoundTag, COMPOUND_KEY_CLOUD_COLOR, 0xFFFFFFFF));
    }
}
