package net.ty.createcraftedbeginning.config;

import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class CCBServer extends ConfigBase {
    public final ConfigGroup infrastructure = group(0, "infrastructure", Comments.infrastructure);
    public final CCBStress stressValues = nested(0, CCBStress::new, Comments.stress);
    public final CCBCrates crates = nested(0, CCBCrates::new, Comments.crates);
    public final CCBGas gas = nested(0, CCBGas::new, Comments.gas);
    public final CCBAirtights airtights = nested(0, CCBAirtights::new, Comments.airtights);
    public final CCBEquipments equipments = nested(0, CCBEquipments::new, Comments.equipments);

    @Override
    public @NotNull String getName() {
        return "server";
    }

    private static class Comments {
        static String infrastructure = "The Backbone of Create: Crafted Beginning";
        static String stress = "Fine tune the kinetic stats of individual components.";
        static String crates = "Everything related to crates.";
        static String gas = "Everything related to gases.";
        static String airtights = "Airtight Blocks and Items added by Create: Crafted Beginning.";
        static String equipments = "Tools and weapons added by Create: Crafted Beginning.";
    }
}
