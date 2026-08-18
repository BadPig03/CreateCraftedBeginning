package net.ty.createcraftedbeginning.content.airtights.airtightextendarm;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gascanisters.events.GasTypeChangedEvent;
import net.ty.createcraftedbeginning.content.airtights.airtightextendarm.AirtightExtendArmUtils.PowerUseResult;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterUtils;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.WeakHashMap;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID)
final class AirtightExtendArmEvents {
    private static final Map<Player, InteractionCharge> LAST_INTERACTION_CHARGES = new WeakHashMap<>();

    private AirtightExtendArmEvents() {
    }

    @SubscribeEvent
    private static void onPlayerTick(Post event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide) {
            return;
        }

        AirtightExtendArmUtils.tick(player);
        if (player.tickCount % 20 != 0 || !player.getMainHandItem().is(CCBItems.AIRTIGHT_EXTEND_ARM) || !player.getOffhandItem().is(CCBItems.AIRTIGHT_EXTEND_ARM)) {
            return;
        }

        CCBAdvancements.THREE_WAY_HANDSHAKE.awardTo(player);
    }

    @SubscribeEvent
    private static void onGasTypeChanged(GasTypeChangedEvent event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide || !AirtightExtendArmUtils.isHoldingArms(player)) {
            return;
        }

        AirtightExtendArmUtils.refreshArmModifiers(player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    private static void onBreakBlocks(BreakEvent event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide) {
            return;
        }

        PowerUseResult result = AirtightExtendArmUtils.tryUseBlockPower(player, event.getPos());
        if (result.allowed()) {
            return;
        }

        event.setCanceled(true);
        displayInsufficientGasWarning(player, result);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    private static void onRightClickBlock(RightClickBlock event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }

        boolean interactionDenied = event.getUseBlock() == TriState.FALSE && event.getUseItem() == TriState.FALSE;
        if (interactionDenied) {
            return;
        }

        ChargeAttempt attempt = consumeInteractionOnce(player, InteractionType.BLOCK, event.getPos().asLong(), () -> AirtightExtendArmUtils.tryUseBlockPower(player, event.getPos()));
        if (attempt.result().allowed()) {
            return;
        }

        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
        displayWarningOnFirstAttempt(player, attempt);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    private static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }

        PowerUseResult result = AirtightExtendArmUtils.tryUseAttackPower(player, event.getTarget());
        if (result.allowed()) {
            return;
        }

        event.setCanceled(true);
        displayInsufficientGasWarning(player, result);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    private static void onEntityInteractSpecific(EntityInteractSpecific event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }

        Entity target = event.getTarget();
        ChargeAttempt attempt = consumeInteractionOnce(player, InteractionType.ENTITY, target.getId(), () -> AirtightExtendArmUtils.tryUseEntityPower(player, target));
        if (attempt.result().allowed()) {
            return;
        }

        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
        displayWarningOnFirstAttempt(player, attempt);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    private static void onEntityInteract(EntityInteract event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }

        Entity target = event.getTarget();
        ChargeAttempt attempt = consumeInteractionOnce(player, InteractionType.ENTITY, target.getId(), () -> AirtightExtendArmUtils.tryUseEntityPower(player, target));
        if (attempt.result().allowed()) {
            return;
        }

        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
        displayWarningOnFirstAttempt(player, attempt);
    }

    private static ChargeAttempt consumeInteractionOnce(Player player, InteractionType type, long targetKey, PowerUseSupplier supplier) {
        long gameTime = player.level().getGameTime();
        InteractionCharge previous = LAST_INTERACTION_CHARGES.get(player);
        if (previous != null && previous.gameTime() == gameTime && previous.type() == type && previous.targetKey() == targetKey) {
            return new ChargeAttempt(previous.result(), false);
        }

        PowerUseResult result = supplier.get();
        LAST_INTERACTION_CHARGES.put(player, new InteractionCharge(gameTime, type, targetKey, result));
        return new ChargeAttempt(result, true);
    }

    private static void displayWarningOnFirstAttempt(Player player, ChargeAttempt attempt) {
        if (!attempt.firstAttempt()) {
            return;
        }

        displayInsufficientGasWarning(player, attempt.result());
    }

    private static void displayInsufficientGasWarning(Player player, PowerUseResult result) {
        if (!result.shouldWarn()) {
            return;
        }

        GasStack attemptedGas = result.attemptedGas();
        if (attemptedGas.isEmpty()) {
            GasCanisterUtils.displayCustomWarningHint(player, "gui.warnings.no_gas");
            return;
        }

        GasCanisterUtils.displayCustomWarningHint(player, "gui.warnings.insufficient_gas", attemptedGas.getHoverName());
    }

    private enum InteractionType {
        BLOCK,
        ENTITY
    }

    @FunctionalInterface
    private interface PowerUseSupplier {
        PowerUseResult get();
    }

    private record InteractionCharge(long gameTime, InteractionType type, long targetKey, PowerUseResult result) {}

    private record ChargeAttempt(PowerUseResult result, boolean firstAttempt) {}
}
