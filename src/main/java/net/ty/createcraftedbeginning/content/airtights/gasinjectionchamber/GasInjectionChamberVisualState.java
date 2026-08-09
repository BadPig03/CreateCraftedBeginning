package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;

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

    void writeCloud(CompoundTag tag, boolean clientPacket) {
        if (!sendCloud || !clientPacket) {
            return;
        }

        tag.putBoolean(COMPOUND_KEY_CLOUD, true);
        tag.putInt(COMPOUND_KEY_CLOUD_COLOR, cloudColor);
        sendCloud = false;
    }

    OptionalInt readCloud(CompoundTag tag, boolean clientPacket) {
        if (!clientPacket || !tag.contains(COMPOUND_KEY_CLOUD)) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(tag.contains(COMPOUND_KEY_CLOUD_COLOR) ? tag.getInt(COMPOUND_KEY_CLOUD_COLOR) : 0xFFFFFFFF);
    }
}
