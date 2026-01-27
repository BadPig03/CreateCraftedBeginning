package net.ty.createcraftedbeginning.content.airtights.airtightextendarm;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
import net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.cansiters.events.GasTypeChangedEvent;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID)
public class AirtightExtendArmEvents {
    @SubscribeEvent
    public static void onPlayerTick(@NotNull Post event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide) {
            return;
        }

        AirtightExtendArmUtils.refreshArmModifiers(player);
        if (CCBAdvancements.THREE_WAY_HANDSHAKE.isAlreadyAwardedTo(player) || !player.getMainHandItem().is(CCBItems.AIRTIGHT_EXTEND_ARM) || !player.getOffhandItem().is(CCBItems.AIRTIGHT_EXTEND_ARM)) {
            return;
        }

        CCBAdvancements.THREE_WAY_HANDSHAKE.awardTo(player);
    }

    @SubscribeEvent
    public static void onBreakBlocks(@NotNull BreakEvent event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide || !AirtightExtendArmUtils.isHoldingArms(player)) {
            return;
        }

        AirtightExtendArmUtils.setOperability(player, AirtightExtendArmUtils.updateOperationAbility(player));
    }

    @SubscribeEvent
    public static void onPlaceBlocks(@NotNull EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (player.level().isClientSide || !AirtightExtendArmUtils.isHoldingArms(player)) {
            return;
        }

        AirtightExtendArmUtils.setOperability(player, AirtightExtendArmUtils.updateOperationAbility(player));
    }

    @SubscribeEvent
    public static void onAttackLivingEntities(@NotNull LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        if (player.level().isClientSide || !AirtightExtendArmUtils.isHoldingArms(player)) {
            return;
        }

        AirtightExtendArmUtils.setOperability(player, AirtightExtendArmUtils.updateOperationAbility(player));
    }

    @SubscribeEvent
    public static void onGasTypeChanged(@NotNull GasTypeChangedEvent event) {
        Player player = event.getPlayer();
        Gas currentGasType = event.getCurrentGasType();
        if (currentGasType.isEmpty()) {
            return;
        }

        if (AirtightExtendArmUtils.getCached(player)) {
            AirtightExtendArmUtils.setCached(player, false);
        }
        Gas previousGasType = event.getPreviousGasType();
        if (previousGasType.isEmpty()) {
            return;
        }

        AirtightExtendArmUtils.removeModifiers(player, previousGasType);
    }
}
