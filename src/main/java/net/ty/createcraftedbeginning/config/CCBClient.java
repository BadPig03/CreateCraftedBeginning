package net.ty.createcraftedbeginning.config;

import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

public class CCBClient extends ConfigBase {
    public final ConfigBool enableHelmetGoggles = b(true, "enable_helmet_goggles", Comments.enableAirtightHelmetGoggles);
    public final ConfigBool enableChestplateFirstPersonArm = b(true, "enable_chestplate_first_person_arm", Comments.enableAirtightChestplateFirstPersonArm);
    public final ConfigBool enableCurrentGasInfo = b(true, "enable_current_gas_info", Comments.enableCurrentGasInfo);
    public final ConfigBool enableGasAreaOutline = b(true, "enable_gas_area_outline", Comments.enableGasAreaOutline);
    public final ConfigBool enableEndIncinerationBlowerOutline = b(true, "enable_end_incineration_blower_outline", Comments.enableEndIncinerationBlowerOutline);
    public final ConfigInt gasInfoXOffset = i(0, "gas_info_x_offset", Comments.gasInfoXOffset);
    public final ConfigInt gasInfoYOffset = i(0, "gas_info_y_offset", Comments.gasInfoYOffset);
    public final ConfigInt maxItemStackDisplay = i(4, 1, 27, "max_item_stack_display", Comments.maxItemStackDisplay);

    @Override
    public @NotNull String getName() {
        return "client";
    }

    private static class Comments {
        static String enableAirtightHelmetGoggles = "Displays Engineer's Goggles information when wearing an Airtight Helmet.";
        static String enableAirtightChestplateFirstPersonArm = "Displays the first-person arm when wearing an Airtight Chestplate.";
        static String enableCurrentGasInfo = "Displays the information about the gas currently being used by the player.";
        static String enableGasAreaOutline = "Displays the outline of the area where gases are being expelled.";
        static String enableEndIncinerationBlowerOutline = "Displays the working range of the End Incineration Blower.";
        static String gasInfoXOffset = "X offset of the display showing the gas information currently used by the player.";
        static String gasInfoYOffset = "Y offset of the display showing the gas information currently used by the player.";
        static String maxItemStackDisplay = "The maximum number of item stacks displayed for the Airtight Reactor Kettle with Engineers' Goggles.";
    }
}
