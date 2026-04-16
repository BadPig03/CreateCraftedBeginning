package net.ty.createcraftedbeginning.content.airtights.airtightarmors;

import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.api.gas.armorhandlers.AirtightArmorsHandler;
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerConsumers;
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades.AirtightBootsUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades.BootsResistanceUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.AirtightChestplateUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.ChestplateResistanceUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades.AirtightHelmetUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.upgrades.HelmetResistanceUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.upgrades.AirtightLeggingsUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.upgrades.LeggingsResistanceUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightupgrades.AirtightUpgradeStatus;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public final class AirtightArmorsUtils {
    private AirtightArmorsUtils() {
    }

    private static final int DURATION_THRESHOLD = 30;

    public static List<AirtightUpgradeStatus> getDefaultUpgradeList(@NotNull ItemStack armor) {
        if (armor.is(CCBItems.AIRTIGHT_HELMET)) {
            return AirtightHelmetUpgradeRegistry.getDefaultUpgradeList();
        } else if (armor.is(CCBItems.AIRTIGHT_CHESTPLATE)) {
            return AirtightChestplateUpgradeRegistry.getDefaultUpgradeList();
        } else if (armor.is(CCBItems.AIRTIGHT_LEGGINGS)) {
            return AirtightLeggingsUpgradeRegistry.getDefaultUpgradeList();
        } else if (armor.is(CCBItems.AIRTIGHT_BOOTS)) {
            return AirtightBootsUpgradeRegistry.getDefaultUpgradeList();
        }
        return Collections.emptyList();
    }

    public static boolean canInvalidateDamage(@NotNull Player player, float amount, Supplier<Boolean> supplier) {
        if (!CanisterContainerSuppliers.isAnyContainerAvailable(player)) {
            return false;
        }

        GasStack gasContent = CanisterContainerSuppliers.getFirstAvailableGasContent(player);
        if (gasContent.isEmpty()) {
            return false;
        }

        Gas gasType = gasContent.getGasType();
        AirtightArmorsHandler armorsHandler = AirtightArmorsHandler.REGISTRY.get(gasType);
        if (armorsHandler == null) {
            return false;
        }

        int gasConsumption = Mth.ceil(CCBConfig.server().equipments.armorsInvalidateDamage.get() * amount * armorsHandler.getConsumptionMultiplier()[3]);
        return CanisterContainerConsumers.interactContainer(player, gasType, gasConsumption, supplier);
    }

    public static boolean isEntireArmoredUp(@NotNull Player player) {
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

    public static void applyResistance(@NotNull Player player) {
        GasStack gasContent = CanisterContainerSuppliers.getFirstAvailableGasContent(player);
        if (gasContent.isEmpty()) {
            return;
        }

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

    public static int getResistanceLevel(@NotNull Player player) {
        int level = -1;
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.is(CCBItems.AIRTIGHT_HELMET) && HelmetResistanceUpgrade.INSTANCE.isEnabled(helmet)) {
            level++;
        }

        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chestplate.is(CCBItems.AIRTIGHT_CHESTPLATE) && ChestplateResistanceUpgrade.INSTANCE.isEnabled(chestplate)) {
            level++;
        }

        ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
        if (leggings.is(CCBItems.AIRTIGHT_LEGGINGS) && LeggingsResistanceUpgrade.INSTANCE.isEnabled(leggings)) {
            level++;
        }

        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (boots.is(CCBItems.AIRTIGHT_BOOTS) && BootsResistanceUpgrade.INSTANCE.isEnabled(boots)) {
            level++;
        }

        return level;
    }

}
