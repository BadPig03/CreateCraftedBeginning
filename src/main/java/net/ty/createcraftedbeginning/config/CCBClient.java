package net.ty.createcraftedbeginning.config;

import net.createmod.catnip.config.ConfigBase;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBClient extends ConfigBase {
    public final EquipmentRendering equipmentRendering = nested(0, EquipmentRendering::new, "Equipment Rendering");
    public final Outlines outlines = nested(0, Outlines::new, "Outlines");
    public final Particles particles = nested(0, Particles::new, "Particles");
    public final Overlays overlays = nested(0, Overlays::new, "Overlays");
    public final GasRequests gasRequests = nested(0, GasRequests::new, "Gas Requests");

    @Override
    public String getName() {
        return "client";
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class EquipmentRendering extends ConfigBase {
        public final ConfigBool showChestplateFirstPersonArm = b(true, "show_chestplate_first_person_arm", "Show the player's arm in first person while wearing an Airtight Chestplate.");

        @Override
        public String getName() {
            return "equipment_rendering";
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class Outlines extends ConfigBase {
        public final ConfigBool showAirtightEncasedPipeSides = b(true, "show_airtight_encased_pipe_sides", "Show the open and closed sides of Airtight Encased Pipes while holding a Wrench and wearing Engineer's Goggles.");
        public final ConfigBool showEndIncinerationBlowerRange = b(true, "show_end_incineration_blower_range", "Show the End Incineration Blower's working range when its range display is enabled and the player is wearing Engineer's Goggles.");
        public final ConfigBool showEndSculkSilencerChunks = b(true, "show_end_sculk_silencer_chunks", "Show the chunk columns affected by the End Sculk Silencer when its range display is enabled and the player is wearing Engineer's Goggles.");
        public final ConfigBool showGasReleaseAreas = b(true, "show_gas_release_areas", "Show gas release area outlines while wearing Engineer's Goggles.");

        @Override
        public String getName() {
            return "outlines";
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class Particles extends ConfigBase {
        public final ConfigBool showChestplateJetpackParticles = b(true, "show_chestplate_jetpack_particles", "Show jetpack particles while flying with an Airtight Chestplate.");
        public final ConfigBool showEndIncinerationBlowerParticles = b(true, "show_end_incineration_blower_particles", "Show particles produced by the End Incineration Blower.");

        @Override
        public String getName() {
            return "particles";
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class Overlays extends ConfigBase {
        public final ConfigBool showCurrentGasInfo = b(true, "show_current_gas_info", "Show the currently selected gas supply in the player HUD. The display remains hidden while the HUD is hidden.");
        public final ConfigInt gasInfoXOffset = i(0, "gas_info_x_offset", "[Unit: pixels]", "Horizontal offset of the current gas supply display.");
        public final ConfigInt gasInfoYOffset = i(0, "gas_info_y_offset", "[Unit: pixels]", "Vertical offset of the current gas supply display.");
        public final ConfigInt maxGoggleItemStacks = i(4, 1, 27, "max_goggle_item_stacks", "[Unit: item stacks]", "Maximum number of item stacks displayed in Airtight Forging Press and Airtight Reactor Kettle tooltips while wearing Engineer's Goggles.");

        @Override
        public String getName() {
            return "overlays";
        }
    }

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final class GasRequests extends ConfigBase {
        public final ConfigInt altScrollStep = i(10, 1, "alt_scroll_step", "[Unit: GU per scroll step]", "Amount added to or removed from a gas request per scroll step while holding Alt on the Redstone Requester screen.");
        public final ConfigInt ctrlScrollStep = i(1, 1, "ctrl_scroll_step", "[Unit: GU per scroll step]", "Amount added to or removed from a gas request per scroll step while holding Ctrl on the Redstone Requester screen.");
        public final ConfigInt scrollStep = i(1000, 1, "scroll_step", "[Unit: GU per scroll step]", "Amount added to or removed from a gas request per scroll step without a modifier key on the Redstone Requester screen.");
        public final ConfigInt shiftScrollStep = i(100, 1, "shift_scroll_step", "[Unit: GU per scroll step]", "Amount added to or removed from a gas request per scroll step while holding Shift on the Redstone Requester screen.");

        @Override
        public String getName() {
            return "gas_requests";
        }
    }
}
