package net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate;

import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre;
import net.neoforged.neoforge.event.entity.living.LivingEvent.LivingVisibilityEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickEmpty;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.AirtightChestplateUpgradeRegistry;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.ChestplateResistanceUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.CreativeFlightUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.ElytraUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.upgrades.InvisibilityUpgrade;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID)
public class AirtightChestplateEvents {
    @SubscribeEvent
    public static void onRightClickEmpty(RightClickEmpty event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide || event.getHand() != InteractionHand.MAIN_HAND || !player.getMainHandItem().isEmpty() || !ElytraUpgrade.canRequestBoost(player)) {
            return;
        }

        ElytraUpgrade.applySpeedBoost(player);
        CatnipServices.NETWORK.sendToServer(AirtightChestplateElytraBoostPacket.INSTANCE);
    }

    @SubscribeEvent
    public static void onPlayerVisibility(LivingVisibilityEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player) || !InvisibilityUpgrade.INSTANCE.canApply(player)) {
            return;
        }

        event.modifyVisibility(0);
    }

    @SubscribeEvent
    public static void onPlayerIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !event.getSource().is(DamageTypes.FLY_INTO_WALL) || !ElytraUpgrade.INSTANCE.canApply(player)) {
            return;
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerPreTakeDamage(Pre event) {
        if (!(event.getEntity() instanceof Player player) || event.getSource().is(DamageTypeTags.BYPASSES_RESISTANCE)) {
            return;
        }

        ChestplateResistanceUpgrade.INSTANCE.canApply(player, event.getOriginalDamage());
    }

    @SubscribeEvent
    public static void onPlayerTick(Post event) {
        Player player = event.getEntity();
        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chestplate.is(CCBItems.AIRTIGHT_CHESTPLATE)) {
            return;
        }

        Level level = player.level();
        if (level.isClientSide) {
            CreativeFlightUpgrade.spawnParticles(player, level);
            return;
        }

        AirtightChestplateUpgradeRegistry.forEach(upgrade -> {
            if (!upgrade.canApply(player)) {
                return;
            }

            upgrade.applyEffect(player);
        });

        if (level.getGameTime() % 20 != 0 || player.getEyePosition().y <= level.getMaxBuildHeight()) {
            return;
        }

        CCBAdvancements.SKY_IS_NOT_THE_LIMIT.awardTo(player);
    }
}
