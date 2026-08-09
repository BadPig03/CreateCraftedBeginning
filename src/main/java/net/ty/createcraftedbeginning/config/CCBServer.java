package net.ty.createcraftedbeginning.config;

import net.createmod.catnip.config.ConfigBase;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBServer extends ConfigBase {
    @SuppressWarnings("unused")
    public final ConfigGroup infrastructure = group(0, "infrastructure", Comments.infrastructure);
    public final CCBStress stressValues = nested(0, CCBStress::new, Comments.stress);
    public final CCBCrates crates = nested(0, CCBCrates::new, Comments.crates);
    public final CCBAirtights airtights = nested(0, CCBAirtights::new, Comments.airtights);
    public final CCBEquipments equipments = nested(0, CCBEquipments::new, Comments.equipments);
    public final CCBEndDevices endDevices = nested(0, CCBEndDevices::new, Comments.endDevices);

    @Override
    public String getName() {
        return "server";
    }

    private static class Comments {
        static String infrastructure = "Core settings for Create: Crafted Beginning.";
        static String stress = "Fine-tune the kinetic properties of individual components.";
        static String crates = "Settings for crates added by Create: Crafted Beginning.";
        static String airtights = "Settings for airtight blocks and items added by Create: Crafted Beginning.";
        static String equipments = "Settings for equipment added by Create: Crafted Beginning.";
        static String endDevices = "Settings for endgame mechanical devices added by Create: Crafted Beginning.";
    }
}
