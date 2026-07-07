package net.ty.createcraftedbeginning.config;

import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

public class CCBClient extends ConfigBase {
    public final ConfigBool enableChestplateFirstPersonArm = b(true, "enable_chestplate_first_person_arm", Comments.enableAirtightChestplateFirstPersonArm);
    public final ConfigBool enableChestplateJetpackParticles = b(true, "enable_chestplate_jetpack_particles", Comments.enableChestplateJetpackParticles);
    public final ConfigBool enableCurrentGasInfo = b(true, "enable_current_gas_info", Comments.enableCurrentGasInfo);
    public final ConfigBool enableGasAreaOutline = b(true, "enable_gas_area_outline", Comments.enableGasAreaOutline);
    public final ConfigBool enableEndIncinerationBlowerOutline = b(true, "enable_end_incineration_blower_outline", Comments.enableEndIncinerationBlowerOutline);
    public final ConfigInt gasInfoXOffset = i(0, "gas_info_x_offset", Comments.gasInfoXOffset);
    public final ConfigInt gasInfoYOffset = i(0, "gas_info_y_offset", Comments.gasInfoYOffset);
    public final ConfigInt maxItemStackDisplay = i(4, 1, 27, "max_item_stack_display", Comments.maxItemStackDisplay);
    public final ConfigInt gasRequestScrollStep = i(1000, 1, "gas_request_scroll_step", Comments.gasRequestScrollStep);
    public final ConfigInt gasRequestAltScrollStep = i(10, 1, "gas_request_alt_scroll_step", Comments.gasRequestScrollStepAlt);
    public final ConfigInt gasRequestCtrlScrollStep = i(1, 1, "gas_request_ctrl_scroll_step", Comments.gasRequestScrollStepCtrl);
    public final ConfigInt gasRequestShiftScrollStep = i(100, 1, "gas_request_shift_scroll_step", Comments.gasRequestScrollStepShift);

    @Override
    public @NotNull String getName() {
        return "client";
    }

    private static class Comments {
        static String enableChestplateJetpackParticles = "Displays jetpack particles when flying with an Airtight Chestplate.";
        static String enableAirtightChestplateFirstPersonArm = "Displays the first-person arm when wearing an Airtight Chestplate.";
        static String enableCurrentGasInfo = "Displays the information about the gas currently being used by the player.";
        static String enableGasAreaOutline = "Displays the outline of the area where gases are being expelled.";
        static String enableEndIncinerationBlowerOutline = "Displays the working range of the End Incineration Blower.";
        static String gasInfoXOffset = "X offset of the display showing the gas information currently used by the player.";
        static String gasInfoYOffset = "Y offset of the display showing the gas information currently used by the player.";
        static String maxItemStackDisplay = "The maximum number of item stacks displayed for the Airtight Reactor Kettle with Engineers' Goggles.";
        static String gasRequestScrollStep = "The amount of gas adjusted per scroll step in the Redstone Requester screen.";
        static String gasRequestScrollStepAlt = "The amount of gas adjusted per scroll step while holding Alt in the Redstone Requester screen.";
        static String gasRequestScrollStepCtrl = "The amount of gas adjusted per scroll step while holding Ctrl in the Redstone Requester screen.";
        static String gasRequestScrollStepShift = "The amount of gas adjusted per scroll step while holding Shift in the Redstone Requester screen.";
    }
}
