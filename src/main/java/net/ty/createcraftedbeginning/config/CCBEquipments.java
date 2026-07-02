package net.ty.createcraftedbeginning.config;

import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class CCBEquipments extends ConfigBase {
    public final ConfigGroup airtightCannon = group(0, "airtight_cannon", "Airtight Cannon");
    public final ConfigInt perShotConsumption = i(50, 0, "per_shot_cost", Comments.perShotConsumption);

    public final ConfigGroup airtightExtendArm = group(0, "airtight_extend_arm", "Airtight Extend Arm");
    public final ConfigInt perUseConsumption = i(5, 0, "per_use_consumption", Comments.perUseConsumption);

    public final ConfigGroup airtightHandheldDrill = group(0, "airtight_handheld_drill", "Airtight Handheld Drill");
    public final ConfigInt perBlockConsumption = i(5, 0, "per_block_consumption", Comments.perBlockConsumption);
    public final ConfigInt perEntityHitConsumption = i(3, 0, "per_entity_hit_consumption", Comments.perEntityHitConsumption);
    public final ConfigFloat silkTouchMultiplier = f(2, 1, "silk_touch_multiplier", Comments.silkTouchMultiplier);
    public final ConfigFloat liquidReplacementMultiplier = f(1, 1, "liquid_replacement_multiplier", Comments.liquidReplacementMultiplier);
    public final ConfigFloat magnetMultiplier = f(1.5f, 1, "magnet_multiplier", Comments.magnetMultiplier);
    public final ConfigFloat experienceConversionMultiplier = f(3, 1, "experience_conversion_multiplier", Comments.experienceConversionMultiplier);

    public final ConfigGroup airtightHelmet = group(0, "airtight_helmet", "Airtight Helmet");
    public final ConfigFloat effectsProtectionMultiplier = f(0.5f, 0, "effects_protection_multiplier", Comments.effectsProtectionMultiplier);
    public final ConfigInt waterBreathingConsumption = i(10, 0, "water_breathing_consumption", Comments.waterBreathingConsumption);
    public final ConfigInt visionConsumption = i(5, 0, "vision_consumption", Comments.visionConsumption);
    public final ConfigFloat helmetResistanceMultiplier = f(5, 0, "helmet_resistance_multiplier", Comments.helmetResistanceMultiplier);

    public final ConfigGroup airtightChestplate = group(0, "airtight_chestplate", "Airtight Chestplate");
    public final ConfigInt elytraConsumption = i(5, 0, "elytra_consumption", Comments.elytraConsumption);
    public final ConfigInt creativeFlightConsumption = i(20, 0, "creative_flight_flying_consumption", Comments.creativeFlightConsumption);
    public final ConfigInt regenerationConsumption = i(10, 0, "regeneration_consumption", Comments.regenerationConsumption);
    public final ConfigInt invisibilityConsumption = i(5, 0, "invisibility_consumption", Comments.invisibilityConsumption);
    public final ConfigFloat chestplateResistanceMultiplier = f(5, 0, "chestplate_resistance_multiplier", Comments.chestplateResistanceMultiplier);

    public final ConfigGroup airtightLeggings = group(0, "airtight_leggings", "Airtight Leggings");
    public final ConfigFloat projectileDeflectionMultiplier = f(25, 0, "projectile_deflection_multiplier", Comments.projectileDeflectionMultiplier);
    public final ConfigInt quickSwimmingConsumption = i(5, 0, "quick_swimming_consumption", Comments.quickSwimmingConsumption);
    public final ConfigFloat leggingsResistanceMultiplier = f(5, 0, "leggings_resistance_multiplier", Comments.leggingsResistanceMultiplier);

    public final ConfigGroup airtightBoots = group(0, "airtight_boots", "Airtight Boots");
    public final ConfigFloat bootsResistanceMultiplier = f(5, 0, "boots_resistance_multiplier", Comments.bootsResistanceMultiplier);

    @Override
    public @NotNull String getName() {
        return "equipments";
    }

    private static class Comments {
        static String perShotConsumption = "The amount of gas consumed by the Airtight Cannon per shot.";

        static String perUseConsumption = "The amount of gas consumed by the Airtight Extend Arm per use";

        static String perBlockConsumption = "The amount of gas consumed by the Airtight Handheld Drill per block mined.";
        static String perEntityHitConsumption = "The amount of gas consumed by the Airtight Handheld Drill each time it hits an entity.";
        static String silkTouchMultiplier = "The gas consumption multiplier of the Airtight Handheld Drill while the Silk Touch Upgrade is active.";
        static String liquidReplacementMultiplier = "The gas consumption multiplier of the Airtight Handheld Drill while the Liquid Replacement Upgrade is active.";
        static String magnetMultiplier = "The gas consumption multiplier of the Airtight Handheld Drill while the Magnet Upgrade is active.";
        static String experienceConversionMultiplier = "The gas consumption multiplier of the Airtight Handheld Drill while the Experience Conversion Upgrade is active.";

        static String effectsProtectionMultiplier = "The gas consumption multiplier of the Airtight Helmet while the Effects Protection Upgrade is active.";
        static String waterBreathingConsumption = "The amount of gas consumed per second by the Airtight Helmet while the Water Breathing Upgrade is active and the wearer is underwater.";
        static String visionConsumption = "The amount of gas consumed per second by the Airtight Helmet while the Vision Upgrade is active.";
        static String helmetResistanceMultiplier = "The gas consumption multiplier of the Airtight Helmet while the Resistance Upgrade is active and the wearer takes damage that does not bypass the Resistance effect.";

        static String elytraConsumption = "The amount of gas consumed per second by the Airtight Chestplate while the Elytra Upgrade is active and the wearer is gliding.";
        static String creativeFlightConsumption = "The amount of gas consumed per second by the Airtight Chestplate while the Creative Flight Upgrade is active and the wearer is flying.";
        static String regenerationConsumption = "The amount of gas consumed per second by the Airtight Chestplate while the Regeneration Upgrade is active and the wearer is regenerating health.";
        static String invisibilityConsumption = "The amount of gas consumed per second by the Airtight Chestplate while the Invisibility Upgrade is active.";
        static String chestplateResistanceMultiplier = "The gas consumption multiplier of the Airtight Chestplate while the Resistance Upgrade is active and the wearer takes damage that does not bypass the Resistance effect.";

        static String projectileDeflectionMultiplier = "The gas consumption multiplier of the Airtight Leggings while the Projectile Deflection Upgrade is active and the wearer deflects a projectile.";
        static String quickSwimmingConsumption = "The amount of gas consumed per second by the Airtight Leggings while the Quick Swimming Upgrade is active and the wearer is underwater.";
        static String leggingsResistanceMultiplier = "The gas consumption multiplier of the Airtight Leggings while the Resistance Upgrade is active and the wearer takes damage that does not bypass the Resistance effect.";

        static String bootsResistanceMultiplier = "The gas consumption multiplier of the Airtight Boots while the Resistance Upgrade is active and the wearer takes damage that does not bypass the Resistance effect.";
    }
}
