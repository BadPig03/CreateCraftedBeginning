package net.ty.createcraftedbeginning.content.airtights.airtightarmors;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades.AirtightBootsUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades.BootsResistanceUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.AirtightChestplateUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.ChestplateResistanceUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades.AirtightHelmetUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades.HelmetResistanceUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.upgrades.AirtightLeggingsUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.upgrades.LeggingsResistanceUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.upgrades.AirtightHandheldDrillUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradeStatus;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightArmorsUtils {
    private static final float RESISTANCE_REDUCTION_PER_LEVEL = 0.2f;

    private AirtightArmorsUtils() {
    }

    public static List<AirtightUpgradeStatus> getDefaultUpgradeList(ItemStack item) {
        if (item.is(CCBItems.AIRTIGHT_HELMET)) {
            return AirtightHelmetUpgradeRegistry.getDefaultUpgradeList();
        }

        if (item.is(CCBItems.AIRTIGHT_CHESTPLATE)) {
            return AirtightChestplateUpgradeRegistry.getDefaultUpgradeList();
        }

        if (item.is(CCBItems.AIRTIGHT_LEGGINGS)) {
            return AirtightLeggingsUpgradeRegistry.getDefaultUpgradeList();
        }

        if (item.is(CCBItems.AIRTIGHT_BOOTS)) {
            return AirtightBootsUpgradeRegistry.getDefaultUpgradeList();
        }

        if (item.is(CCBItems.AIRTIGHT_HANDHELD_DRILL)) {
            return AirtightHandheldDrillUpgradeRegistry.getDefaultUpgradeList();
        }
        return List.of();
    }

    public static List<AirtightUpgrade> getAllUpgrades(ItemStack item) {
        if (item.is(CCBItems.AIRTIGHT_HELMET)) {
            return AirtightHelmetUpgradeRegistry.getAll();
        }

        if (item.is(CCBItems.AIRTIGHT_CHESTPLATE)) {
            return AirtightChestplateUpgradeRegistry.getAll();
        }

        if (item.is(CCBItems.AIRTIGHT_LEGGINGS)) {
            return AirtightLeggingsUpgradeRegistry.getAll();
        }

        if (item.is(CCBItems.AIRTIGHT_BOOTS)) {
            return AirtightBootsUpgradeRegistry.getAll();
        }

        if (item.is(CCBItems.AIRTIGHT_HANDHELD_DRILL)) {
            return AirtightHandheldDrillUpgradeRegistry.getAll();
        }
        return List.of();
    }

    public static boolean isEntireArmoredUp(Player player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!helmet.is(CCBItems.AIRTIGHT_HELMET)) {
            return false;
        }

        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chestplate.is(CCBItems.AIRTIGHT_CHESTPLATE)) {
            return false;
        }

        ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
        if (!leggings.is(CCBItems.AIRTIGHT_LEGGINGS)) {
            return false;
        }

        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        return boots.is(CCBItems.AIRTIGHT_BOOTS);
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
        return currentDamage * Mth.clamp(1 - paidLevels * RESISTANCE_REDUCTION_PER_LEVEL, 0, 1);
    }
}
