package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightArmorsUtils;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades.AirtightBootsUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades.EnvironmentalDamageProtectionUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades.FallProtectionUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades.JumpStrengthUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades.MovementEfficiencyUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.upgrades.StepHeightUpgrade;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID)
public class AirtightBootsEvents {
    private static final int TICK_RATE = 5;

    @SubscribeEvent
    public static void onInvalidateEnvironmentalDamage(@NotNull LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        DamageSource damageSource = event.getSource();
        if (!damageSource.is(DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES)) {
            return;
        }

        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (!boots.is(CCBItems.AIRTIGHT_BOOTS) || !EnvironmentalDamageProtectionUpgrade.INSTANCE.isEnabled(boots)) {
            return;
        }

        Level level = player.level();
        if (!AirtightArmorsUtils.canInvalidateDamage(player, event.getAmount(), () -> !level.isClientSide && level.getGameTime() % TICK_RATE == 0)) {
            return;
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onInvalidateFallDamage(@NotNull LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !event.getSource().is(DamageTypeTags.IS_FALL)) {
            return;
        }

        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (!boots.is(CCBItems.AIRTIGHT_BOOTS) || !FallProtectionUpgrade.INSTANCE.isEnabled(boots)) {
            return;
        }

        Level level = player.level();
        if (!AirtightArmorsUtils.canInvalidateDamage(player, event.getAmount(), () -> !level.isClientSide)) {
            return;
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerTick(@NotNull Post event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide) {
            return;
        }

        MovementEfficiencyUpgrade.refreshModifiers(player);
        JumpStrengthUpgrade.refreshModifiers(player);
        StepHeightUpgrade.refreshModifiers(player);
        AirtightBootsUpgradeRegistry.forEach(upgrade -> {
            if (!upgrade.canApply(player)) {
                return;
            }

            upgrade.applyEffect(player);
        });
    }
}
