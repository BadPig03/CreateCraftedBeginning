package net.ty.createcraftedbeginning.content.airtights.airtightextendarm;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
import net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID)
public class AirtightExtendArmEvents {
    @SubscribeEvent
    public static void onPlayerTick(Post event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide) {
            return;
        }

        AirtightExtendArmUtils.refreshArmModifiers(player);
        if (!player.getMainHandItem().is(CCBItems.AIRTIGHT_EXTEND_ARM) || !player.getOffhandItem().is(CCBItems.AIRTIGHT_EXTEND_ARM)) {
            return;
        }

        CCBAdvancements.THREE_WAY_HANDSHAKE.awardTo(player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onBreakBlocks(BreakEvent event) {
        if (event.isCanceled()) {
            return;
        }

        Player player = event.getPlayer();
        if (player.level().isClientSide || !AirtightExtendArmUtils.isHoldingArms(player)) {
            return;
        }

        AirtightExtendArmUtils.consumeAndRefresh(player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onPlaceBlocks(EntityPlaceEvent event) {
        if (event.isCanceled()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide || !AirtightExtendArmUtils.isHoldingArms(player)) {
            return;
        }

        AirtightExtendArmUtils.consumeAndRefresh(player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAttackLivingEntities(LivingDamageEvent.Post event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof Player player) || source.getDirectEntity() != player || player.level().isClientSide || !AirtightExtendArmUtils.isHoldingArms(player)) {
            return;
        }

        AirtightExtendArmUtils.consumeAndRefresh(player);
    }
}