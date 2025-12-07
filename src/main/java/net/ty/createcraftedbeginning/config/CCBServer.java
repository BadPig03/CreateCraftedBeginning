package net.ty.createcraftedbeginning.config;

import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

public class CCBServer extends ConfigBase {
    public final CCBStress stressValues = nested(0, CCBStress::new, Comments.stress);
    public final CCBCrates crates = nested(0, CCBCrates::new, Comments.crates);
    public final CCBGas gas = nested(0, CCBGas::new, Comments.gas);
    public final CCBEquipments equipments = nested(0, CCBEquipments::new, Comments.equipments);

    @Override
    public @NotNull String getName() {
        return "server";
    }

    private static class Comments {
        static String stress = "Fine tune the kinetic stats of individual components.";
        static String crates = "Everything related to crates.";
        static String gas = "Everything related to gases.";
        static String equipments = "Tools and weapons added by Create: Crafted Beginning.";
    }
}
