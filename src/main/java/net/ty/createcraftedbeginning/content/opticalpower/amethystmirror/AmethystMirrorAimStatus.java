package net.ty.createcraftedbeginning.content.opticalpower.amethystmirror;

import net.createmod.catnip.lang.Lang;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.StringRepresentable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
enum AmethystMirrorAimStatus implements StringRepresentable {
    NO_TARGET,
    SELF,
    CHECKING,
    READY,
    TOO_FAR,
    NO_INPUT,
    MULTIPLE_INPUTS,
    UNSUPPORTED,
    NO_SOLUTION,
    MIRROR_FRAME,
    BEAM_RANGE,
    BLOCKED,
    INVALID,
    SUBMITTED,
    CANCELLED;

    static AmethystMirrorAimStatus fromId(String id) {
        return Arrays.stream(values()).filter(status -> status.getSerializedName().equals(id)).findFirst().orElse(INVALID);
    }

    @Override
    public String getSerializedName() {
        return Lang.asId(name());
    }
}
