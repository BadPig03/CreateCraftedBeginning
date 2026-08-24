package net.ty.createcraftedbeginning.config;

import net.createmod.catnip.config.ConfigBase;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBEndDevices extends ConfigBase {
    @SuppressWarnings("unused")
    public final ConfigGroup endIncinerationBlower = group(0, "end_incineration_blower", "End Incineration Blower");
    public final ConfigBool ignitionAffectsPlayers = b(true, "ignition_affects_players", Comments.ignitionAffectsPlayers);
    public final ConfigFloat ignitionDamage = f(2, 0, "ignition_damage", Comments.ignitionDamage);
    public final ConfigFloat maxRange = f(3.5f, 0.5f, 7, "max_range", Comments.maxRange);

    @SuppressWarnings("unused")
    public final ConfigGroup endSculkSilencer = group(0, "end_sculk_silencer", "End Sculk Silencer");
    public final ConfigFloat speedRequirementMultiplier = f(1, 0, "speed_requirement_multiplier", Comments.speedRequirementMultiplier);

    @Override
    public String getName() {
        return "end_devices";
    }

    private static class Comments {
        private static final String maxRange = "The maximum radius of the End Incineration Blower's cubic area of effect. Higher values can significantly increase scanning overhead.";
        private static final String ignitionDamage = "The direct damage dealt to living entities other than Snow Golems by each ignition pulse from the End Incineration Blower. Ignition is applied independently of this damage.";
        private static final String ignitionAffectsPlayers = "Whether the End Incineration Blower ignition mode can affect players.";
        private static final String speedRequirementMultiplier = "The multiplier applied to the rotational speed required for each End Sculk Silencer range setting.";
    }
}
