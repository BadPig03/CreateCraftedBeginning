package net.ty.createcraftedbeginning.config;

import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

public class CCBServer extends ConfigBase {
    public final CCBStress stressValues = nested(0, CCBStress::new, Comments.stress);

    @Override
    public @NotNull String getName() {
        return "server";
    }

    private static class Comments {
        static String stress = "Fine tune the kinetic stats of individual components";
    }
}
