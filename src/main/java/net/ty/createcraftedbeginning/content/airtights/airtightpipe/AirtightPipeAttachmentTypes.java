package net.ty.createcraftedbeginning.content.airtights.airtightpipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public enum AirtightPipeAttachmentTypes {
    NONE,
    RIM,
    DRAIN,
    INLET_RIM,
    INLET_DRAIN,
    OUTLET_RIM,
    OUTLET_DRAIN
}
