package net.ty.createcraftedbeginning.config;

import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class CCBEquipments extends ConfigBase {
    public final ConfigGroup airtightCannon = group(1, "airtight_cannon", "Airtight Cannon");
    public final ConfigInt gasCostPerShot = i(50, 0, "gas_cost_per_shot", Comments.cannonGasCostPerShot);

    public final ConfigGroup airtightHandheldDrill = group(1, "airtight_handheld_drill", "Airtight Handheld Drill");
    public final ConfigInt drillGasCostPerBlock = i(10, 0, "gas_cost_per_block", Comments.drillGasCostPerBlock);
    public final ConfigInt drillGasCostPerLiquidBlock = i(5, 0, "gas_cost_per_liquid_block", Comments.drillGasCostPerLiquidBlock);
    public final ConfigInt drillGasCostPerEntityHit = i(5, 0, "gas_cost_per_entity_hit", Comments.drillGasCostPerEntityHit);
    public final ConfigFloat drillGasMultiplierForSilkTouch = f(2, 1, "gas_multiplier_for_silk_touch", Comments.drillGasMultiplierForSilkTouch);
    public final ConfigFloat drillGasMultiplierForMagnet = f(1.5f, 1, "gas_multiplier_for_magnet", Comments.drillGasMultiplierForMagnet);
    public final ConfigFloat drillGasMultiplierForConversion = f(3, 1, "gas_multiplier_for_conversion", Comments.drillGasMultiplierForConversion);

    public final ConfigGroup airtightArmors = group(1, "airtight_armors", "Airtight Armors");
    public final ConfigInt helmetUnderwaterBreathing = i(20, 0, "helmet_underwater_breathing_gas_cost", Comments.helmetUnderwaterBreathing);
    public final ConfigInt chestplateCreativeFlight = i(40, 0, "chestplate_creative_flight_gas_cost", Comments.chestplateCreativeFlight);
    public final ConfigInt chestplateElytraGliding = i(10, 0, "chestplate_elytra_gliding_gas_cost", Comments.chestplateElytraGliding);
    public final ConfigInt chestplateElytraBoost = i(100, 0, "chestplate_elytra_boost_gas_cost", Comments.chestplateElytraBoost);
    public final ConfigInt leggingsProjectileDeflection = i(50, 0, "leggings_projectile_deflection_gas_cost", Comments.leggingsProjectileDeflection);
    public final ConfigInt armorsInvalidateDamage = i(20, 0, "armors_invalidate_damage_gas_cost", Comments.armorsInvalidateDamage);

    @Override
    public @NotNull String getName() {
        return "equipments";
    }

    private static class Comments {
        static String cannonGasCostPerShot = "The amount of gas consumed by the Airtight Cannon for each shot.";
        static String drillGasCostPerBlock = "The amount of gas consumed by the Airtight Handheld Drill for each block mined.";
        static String drillGasCostPerLiquidBlock = "The amount of gas consumed by the Airtight Handheld Drill for each liquid block replaced.";
        static String drillGasCostPerEntityHit = "The amount of gas consumed by the Airtight Handheld Drill each time it attacks an entity.";
        static String drillGasMultiplierForSilkTouch = "The gas consumption multiplier of the Airtight Handheld Drill when the Silk Touch Upgrade is active.";
        static String drillGasMultiplierForMagnet = "The gas consumption multiplier of the Airtight Handheld Drill when the Magnet Upgrade is active.";
        static String drillGasMultiplierForConversion = "The gas consumption multiplier of the Airtight Handheld Drill when the Conversion Upgrade is active.";
        static String helmetUnderwaterBreathing = "The amount of gas consumed per second by the Airtight Helmet during underwater breathing.";
        static String chestplateCreativeFlight = "The amount of gas consumed per second by the Airtight Chestplate to sustain creative flight.";
        static String chestplateElytraGliding = "The amount of gas consumed per second by the Airtight Chestplate during Elytra gliding.";
        static String chestplateElytraBoost = "The amount of gas consumed by the Airtight Chestplate when boosting Elytra gliding.";
        static String leggingsProjectileDeflection = "The amount of gas consumed by the Airtight Leggings when deflecting a projectile.";
        static String armorsInvalidateDamage = "The amount of gas consumed by Airtight Armors to invalidate each point of damage.";
    }
}
