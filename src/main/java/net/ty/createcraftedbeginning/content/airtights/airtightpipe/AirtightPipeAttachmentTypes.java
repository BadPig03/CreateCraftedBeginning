package net.ty.createcraftedbeginning.content.airtights.airtightpipe;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightPipeAttachmentTypes {
    public enum AttachmentTypes {
        NONE,
        RIM(ComponentPartials.RIM),
        DRAIN(ComponentPartials.DRAIN);

        public final ComponentPartials[] partials;

        AttachmentTypes(ComponentPartials... partials) {
            this.partials = partials;
        }

        public enum ComponentPartials {
            RIM,
            DRAIN
        }
    }
}
