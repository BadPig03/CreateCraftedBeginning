package net.ty.createcraftedbeginning.content.airtights.airtightarmors;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
    private static final int DURATION_THRESHOLD = 30;

    private AirtightArmorsUtils() {
    }

    public static List<AirtightUpgradeStatus> getDefaultUpgradeList(ItemStack item) {
        if (item.is(CCBItems.AIRTIGHT_HELMET)) {
            return AirtightHelmetUpgradeRegistry.getDefaultUpgradeList();
        }
        else if (item.is(CCBItems.AIRTIGHT_CHESTPLATE)) {
            return AirtightChestplateUpgradeRegistry.getDefaultUpgradeList();
        }
        else if (item.is(CCBItems.AIRTIGHT_LEGGINGS)) {
            return AirtightLeggingsUpgradeRegistry.getDefaultUpgradeList();
        }
        else if (item.is(CCBItems.AIRTIGHT_BOOTS)) {
            return AirtightBootsUpgradeRegistry.getDefaultUpgradeList();
        }
        else if (item.is(CCBItems.AIRTIGHT_HANDHELD_DRILL)) {
            return AirtightHandheldDrillUpgradeRegistry.getDefaultUpgradeList();
        }
        return List.of();
    }

    public static List<AirtightUpgrade> getAllUpgrades(ItemStack item) {
        if (item.is(CCBItems.AIRTIGHT_HELMET)) {
            return AirtightHelmetUpgradeRegistry.getAll();
        }
        else if (item.is(CCBItems.AIRTIGHT_CHESTPLATE)) {
            return AirtightChestplateUpgradeRegistry.getAll();
        }
        else if (item.is(CCBItems.AIRTIGHT_LEGGINGS)) {
            return AirtightLeggingsUpgradeRegistry.getAll();
        }
        else if (item.is(CCBItems.AIRTIGHT_BOOTS)) {
            return AirtightBootsUpgradeRegistry.getAll();
        }
        else if (item.is(CCBItems.AIRTIGHT_HANDHELD_DRILL)) {
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

    public static void applyResistance(Player player) {
        int resistanceLevel = getResistanceLevel(player);
        if (resistanceLevel < 0) {
            return;
        }

        MobEffectInstance existingEffect = player.getEffect(MobEffects.DAMAGE_RESISTANCE);
        if (existingEffect != null && (existingEffect.getAmplifier() > resistanceLevel || !existingEffect.endsWithin(DURATION_THRESHOLD))) {
            return;
        }

        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, DURATION_THRESHOLD, resistanceLevel, true, false));
    }

    private static int getResistanceLevel(Player player) {
        int level = -1;
        if (HelmetResistanceUpgrade.INSTANCE.canApply(player)) {
            level++;
        }
        if (ChestplateResistanceUpgrade.INSTANCE.canApply(player)) {
            level++;
        }
        if (LeggingsResistanceUpgrade.INSTANCE.canApply(player)) {
            level++;
        }
        if (BootsResistanceUpgrade.INSTANCE.canApply(player)) {
            level++;
        }
        return level;
    }
}
