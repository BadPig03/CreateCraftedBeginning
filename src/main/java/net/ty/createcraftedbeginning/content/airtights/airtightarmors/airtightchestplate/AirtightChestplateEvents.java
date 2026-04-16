package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent.LivingVisibilityEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickEmpty;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightArmorsUtils;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.AirtightChestplateUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.CreativeFlightUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.ElytraUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.HasteUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.InvisibilityUpgrade;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID)
public class AirtightChestplateEvents {
    @SubscribeEvent
    public static void onRightClickEmpty(@NotNull RightClickEmpty event) {
        Player player = event.getEntity();
        if (!player.getMainHandItem().isEmpty()) {
            return;
        }

        InteractionHand hand = event.getHand();
        if (hand != InteractionHand.MAIN_HAND) {
            return;
        }

        float multiplier = CreativeFlightUpgrade.getBoostMultiplier(player);
        if (multiplier == 0) {
            return;
        }

        CreativeFlightUpgrade.speedBoost(player, multiplier);
    }

    @SubscribeEvent
    public static void onPlayerVisibility(@NotNull LivingVisibilityEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }

        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chestplate.is(CCBItems.AIRTIGHT_CHESTPLATE) || !InvisibilityUpgrade.INSTANCE.isEnabled(chestplate)) {
            return;
        }

        event.modifyVisibility(0);
    }

    @SubscribeEvent
    public static void onInvalidateWallDamage(@NotNull LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !event.getSource().is(DamageTypes.FLY_INTO_WALL)) {
            return;
        }

        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chestplate.is(CCBItems.AIRTIGHT_CHESTPLATE) || !ElytraUpgrade.INSTANCE.isEnabled(chestplate) || !AirtightArmorsUtils.canInvalidateDamage(player, 0, () -> !player.level().isClientSide)) {
            return;
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerTick(@NotNull Post event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide) {
            CreativeFlightUpgrade.spawnParticles(player, level);
            return;
        }

        CreativeFlightUpgrade.refreshModifiers(player);
        HasteUpgrade.refreshModifiers(player);
        AirtightChestplateUpgradeRegistry.forEach(upgrade -> {
            if (!upgrade.canApply(player)) {
                return;
            }

            upgrade.applyEffect(player);
        });

        if (level.getGameTime() % 20 != 0 || CCBAdvancements.SKY_IS_NOT_THE_LIMIT.isAlreadyAwardedTo(player) || !player.getItemBySlot(EquipmentSlot.CHEST).is(CCBItems.AIRTIGHT_CHESTPLATE) || player.getEyePosition().y <= level.getMaxBuildHeight()) {
            return;
        }

        CCBAdvancements.SKY_IS_NOT_THE_LIMIT.awardTo(player);
    }
}
