package net.ty.createcraftedbeginning.config;

import net.createmod.catnip.config.ConfigBase;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public class CCBFluids extends ConfigBase {
    public final ConfigGroup brimstone = group(0, "brimstone", "Brimstone");
    public final ConfigFloat contactDamage = f(10, 0, "contact_damage", Comments.contactDamage);
    public final ConfigInt burnDuration = i(15, 0, "burn_duration", Comments.seconds, Comments.burnDuration);

    @Override
    public String getName() {
        return "fluids";
    }

    private static class Comments {
        static String seconds = "[in seconds]";
        static String contactDamage = "The damage dealt to a living entity while it is inside Brimstone fluid.";
        static String burnDuration = "How long entities and items without fire immunity remain on fire after contact with Brimstone fluid.";
    }
}
