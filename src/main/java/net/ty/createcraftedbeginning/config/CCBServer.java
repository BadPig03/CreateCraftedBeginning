package net.ty.createcraftedbeginning.config;

import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

public class CCBServer extends ConfigBase {
    public final CCBStress stressValues = nested(0, CCBStress::new, Comments.stress);
    public final CCBCompressedAir compressedAir = nested(0, CCBCompressedAir::new, Comments.compressedAir);
    public final CCBEquipment equipment = nested(0, CCBEquipment::new, Comments.equipment);

    @Override
    public @NotNull String getName() {
        return "server";
    }

    private static class Comments {
        static String stress = "Fine tune the kinetic stats of individual components.";
        static String compressedAir = "Everything related to Compressed Air.";
        static String equipment = "Equipment and gadgets added by Create: Crafted Beginning.";
    }
}
