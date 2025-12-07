package net.ty.createcraftedbeginning.config;

import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

public class CCBClient extends ConfigBase {
    public final ConfigBool enableHelmetGoggles = b(true, "enable_helmet_goggles", Comments.enableAirtightHelmetGoggles);
    public final ConfigBool enableChestplateFirstPersonArm = b(true, "enable_chestplate_first_person_arm", Comments.enableAirtightChestplateFirstPersonArm);
    public final ConfigBool enableCurrentGasInfo = b(true, "enable_current_gas_info", Comments.enableCurrentGasInfo);
    public final ConfigInt gasInfoXOffset = i(0, "gas_info_x_offset", Comments.gasInfoXOffset);
    public final ConfigInt gasInfoYOffset = i(0, "gas_info_y_offset", Comments.gasInfoYOffset);

    @Override
    public @NotNull String getName() {
        return "client";
    }

    private static class Comments {
        static String enableAirtightHelmetGoggles = "Displays Engineer's Goggles information when wearing an Airtight Helmet.";
        static String enableAirtightChestplateFirstPersonArm = "Displays the first-person arm when wearing an Airtight Chestplate.";
        static String enableCurrentGasInfo = "Displays information about the gas currently being used by the player.";
        static String gasInfoXOffset = "X offset of the display showing the gas information currently used by the player.";
        static String gasInfoYOffset = "Y offset of the display showing the gas information currently used by the player.";
    }
}
