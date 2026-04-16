package net.ty.createcraftedbeginning.config;

import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class CCBEquipments extends ConfigBase {
    public final ConfigGroup airtightCannon = group(0, "airtight_cannon", "Airtight Cannon");
    public final ConfigInt gasCostPerShot = i(50, 0, "gas_cost_per_shot", Comments.gasCostPerShot);

    public final ConfigGroup airtightExtendArm = group(0, "airtight_extend_arm", "Airtight Extend Arm");
    public final ConfigInt gasCostPerUse = i(4, 0, "gas_cost_per_use", Comments.gasCostPerUse);

    public final ConfigGroup airtightHandheldDrill = group(0, "airtight_handheld_drill", "Airtight Handheld Drill");
    public final ConfigInt gasCostPerBlock = i(10, 0, "gas_cost_per_block", Comments.gasCostPerBlock);
    public final ConfigInt gasCostPerLiquidBlock = i(5, 0, "gas_cost_per_liquid_block", Comments.gasCostPerLiquidBlock);
    public final ConfigInt gasCostPerEntityHit = i(5, 0, "gas_cost_per_entity_hit", Comments.gasCostPerEntityHit);
    public final ConfigFloat silkTouchGasCostMultiplier = f(2, 1, "silk_touch_gas_cost_multiplier", Comments.silkTouchGasCostMultiplier);
    public final ConfigFloat magnetGasCostMultiplier = f(1.5f, 1, "magnet_gas_cost_multiplier", Comments.magnetGasCostMultiplier);
    public final ConfigFloat experienceConversionGasCostMultiplier = f(3, 1, "experience_conversion_gas_cost_multiplier", Comments.experienceConversionGasCostMultiplier);

    public final ConfigGroup airtightHelmet = group(0, "airtight_helmet", "Airtight Helmet");
    public final ConfigFloat effectsProtectionGasCostMultiplier = f(0.5f, 0, "effects_protection_gas_cost_multiplier", Comments.effectsProtectionGasCostMultiplier);
    public final ConfigInt waterBreathingGasCost = i(5, 0, "water_breathing_gas_cost", Comments.waterBreathingGasCost);

    public final ConfigGroup airtightChestplate = group(0, "airtight_chestplate", "Airtight Chestplate");
    public final ConfigInt elytraGasCost = i(5, 0, "elytra_gas_cost", Comments.elytraGasCost);
    public final ConfigInt creativeFlightGasCost = i(20, 0, "creative_flight_gas_cost", Comments.creativeFlightGasCost);
    public final ConfigInt regenerationGasCost = i(10, 0, "regeneration_gas_cost", Comments.regenerationGasCost);

    public final ConfigGroup airtightLeggings = group(0, "airtight_leggings", "Airtight Leggings");
    public final ConfigInt projectileDeflectionGasCost = i(10, 0, "projectile_deflection_gas_cost", Comments.projectileDeflectionGasCost);

    public final ConfigInt armorsInvalidateDamage = i(20, 0, "armors_invalidate_damage_gas_cost", Comments.armorsInvalidateDamage);

    @Override
    public @NotNull String getName() {
        return "equipments";
    }

    private static class Comments {
        static String gasCostPerShot = "The amount of gas consumed by the Airtight Cannon for each shot.";

        static String gasCostPerUse = "The amount of gas consumed by the Airtight Extend Arm for each use";

        static String gasCostPerBlock = "The amount of gas consumed by the Airtight Handheld Drill for each block mined.";
        static String gasCostPerLiquidBlock = "The amount of gas consumed by the Airtight Handheld Drill for each liquid block replaced.";
        static String gasCostPerEntityHit = "The amount of gas consumed by the Airtight Handheld Drill each time it attacks an entity.";
        static String silkTouchGasCostMultiplier = "The gas consumption multiplier of the Airtight Handheld Drill with the Silk Touch Upgrade enabled.";
        static String magnetGasCostMultiplier = "The gas consumption multiplier of the Airtight Handheld Drill with the Magnet Upgrade enabled.";
        static String experienceConversionGasCostMultiplier = "The gas consumption multiplier of the Airtight Handheld Drill with the Experience Conversion Upgrade enabled.";

        static String armorsInvalidateDamage = "The amount of gas consumed by Airtight Armors to invalidate each point of damage.";

        static String effectsProtectionGasCostMultiplier = "The gas consumption multiplier of the Airtight Helmet with the Effects Protection Upgrade enabled.";
        static String waterBreathingGasCost = "The amount of gas consumed per second by the Airtight Helmet when breathing underwater.";

        static String elytraGasCost = "The amount of gas consumed per second by the Airtight Chestplate when gliding midair.";
        static String creativeFlightGasCost = "The amount of gas consumed per second by the Airtight Chestplate when activating creative flight.";
        static String regenerationGasCost = "The amount of gas consumed per second by the Airtight Chestplate when restoring the health.";

        static String projectileDeflectionGasCost = "The amount of gas consumed by the Airtight Leggings when deflecting a projectile.";
    }
}
