package net.ty.createcraftedbeginning.content.airtights.airtighthatch;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.createmod.catnip.lang.Lang;
import net.ty.createcraftedbeginning.foundation.gui.CCBIcons;

import static java.lang.Math.clamp;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
enum AirtightHatchTransferMode implements INamedIconOptions {
    NO_TRANSFER(CCBIcons.I_NO_TRANSFER),
    INPUT_ONLY(CCBIcons.I_INPUT_ONLY),
    OUTPUT_ONLY(CCBIcons.I_OUTPUT_ONLY),
    STAY_HALF(CCBIcons.I_STAY_HALF);

    private static final AirtightHatchTransferMode[] VALUES = values();

    private final String translationKey;
    private final CCBIcons icon;

    AirtightHatchTransferMode(CCBIcons icon) {
        this.icon = icon;
        translationKey = "createcraftedbeginning.gui.airtight_hatch.transfer_mode." + Lang.asId(name());
    }

    static AirtightHatchTransferMode fromValue(int value) {
        return VALUES[clamp(value, 0, VALUES.length - 1)];
    }

    @Override
    public CCBIcons getIcon() {
        return icon;
    }

    @Override
    public String getTranslationKey() {
        return translationKey;
    }
}
