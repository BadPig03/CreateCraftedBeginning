package net.ty.createcraftedbeginning.content.airtights.airtightarmors;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades.BootsResistanceUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.ChestplateResistanceUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades.HelmetResistanceUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.upgrades.LeggingsResistanceUpgrade;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightArmorResistance {
    private static final float RESISTANCE_REDUCTION_PER_LEVEL = 0.2F;

    private AirtightArmorResistance() {
    }

    static float applyPaidResistance(Player player, float originalDamage, float currentDamage) {
        if (originalDamage <= 0 || currentDamage <= 0) {
            return currentDamage;
        }

        int paidLevels = 0;
        if (HelmetResistanceUpgrade.INSTANCE.tryConsumeGas(player, originalDamage)) {
            paidLevels++;
        }
        if (ChestplateResistanceUpgrade.INSTANCE.tryConsumeGas(player, originalDamage)) {
            paidLevels++;
        }
        if (LeggingsResistanceUpgrade.INSTANCE.tryConsumeGas(player, originalDamage)) {
            paidLevels++;
        }
        if (BootsResistanceUpgrade.INSTANCE.tryConsumeGas(player, originalDamage)) {
            paidLevels++;
        }

        if (paidLevels == 0) {
            return currentDamage;
        }

        return currentDamage * Mth.clamp(1 - paidLevels * RESISTANCE_REDUCTION_PER_LEVEL, 0.0F, 1.0F);
    }
}
