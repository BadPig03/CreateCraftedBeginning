package net.ty.createcraftedbeginning.content.airtights.airtighthatch;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import net.createmod.catnip.lang.Lang;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.client.gui.CCBIcons;

import javax.annotation.ParametersAreNonnullByDefault;

import static java.lang.Math.clamp;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
enum AirtightHatchTransferMode implements INamedIconOptions {
    NO_TRANSFER(CCBIcons.I_NO_TRANSFER),
    INPUT_ONLY(CCBIcons.I_INPUT_ONLY),
    OUTPUT_ONLY(CCBIcons.I_OUTPUT_ONLY),
    TARGET_PRESSURE(CCBIcons.I_TARGET_PRESSURE);

    private static final AirtightHatchTransferMode[] VALUES = values();

    private final String translationKey;
    private final CCBIcons icon;

    AirtightHatchTransferMode(CCBIcons icon) {
        this.icon = icon;
        translationKey = "createcraftedbeginning.gui.airtight_hatch.transfer_mode." + Lang.asId(name());
    }

    static AirtightHatchTransferMode fromValue(int modeValue) {
        return VALUES[clamp(modeValue, 0, VALUES.length - 1)];
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
