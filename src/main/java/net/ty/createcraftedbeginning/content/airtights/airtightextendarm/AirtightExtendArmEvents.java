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
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gascanisters.events.GasTypeChangedEvent;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterUtils;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.WeakHashMap;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID)
public class AirtightExtendArmEvents {
    private static final Map<Player, InteractionCharge> LAST_INTERACTION_CHARGES = new WeakHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(Post event) {
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
    public static void onGasTypeChanged(GasTypeChangedEvent event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide || !AirtightExtendArmUtils.isHoldingArms(player)) {
            return;
        }

        AirtightExtendArmUtils.refreshArmModifiers(player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreakBlocks(BreakEvent event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide || !AirtightExtendArmUtils.requiresExtendedBlockRange(player, event.getPos())) {
            return;
        }

        if (AirtightExtendArmUtils.tryConsumeAndRefresh(player)) {
            return;
        }

        event.setCanceled(true);
        displayInsufficientGasWarning(player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRightClickBlock(RightClickBlock event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }

        boolean interactionDenied = event.getUseBlock() == TriState.FALSE && event.getUseItem() == TriState.FALSE;
        if (interactionDenied || !AirtightExtendArmUtils.requiresExtendedBlockRange(player, event.getPos())) {
            return;
        }

        ChargeAttempt attempt = consumeInteractionOnce(player, InteractionType.BLOCK, event.getPos().asLong());
        if (attempt.success()) {
            return;
        }

        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
        displayWarningOnFirstAttempt(player, attempt);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();
        if (player.level().isClientSide || !AirtightExtendArmUtils.requiresPoweredAttack(player, target)) {
            return;
        }

        if (AirtightExtendArmUtils.tryConsumeAndRefresh(player)) {
            return;
        }

        event.setCanceled(true);
        displayInsufficientGasWarning(player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityInteractSpecific(EntityInteractSpecific event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }

        Entity target = event.getTarget();
        if (!AirtightExtendArmUtils.requiresExtendedEntityRange(player, target)) {
            return;
        }

        ChargeAttempt attempt = consumeInteractionOnce(player, InteractionType.ENTITY, target.getId());
        if (attempt.success()) {
            return;
        }

        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
        displayWarningOnFirstAttempt(player, attempt);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityInteract(EntityInteract event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }

        Entity target = event.getTarget();
        if (!AirtightExtendArmUtils.requiresExtendedEntityRange(player, target)) {
            return;
        }

        ChargeAttempt attempt = consumeInteractionOnce(player, InteractionType.ENTITY, target.getId());
        if (attempt.success()) {
            return;
        }

        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
        displayWarningOnFirstAttempt(player, attempt);
    }

    private static ChargeAttempt consumeInteractionOnce(Player player, InteractionType type, long targetKey) {
        long gameTime = player.level().getGameTime();
        InteractionCharge previous = LAST_INTERACTION_CHARGES.get(player);
        if (previous != null && previous.gameTime() == gameTime && previous.type() == type && previous.targetKey() == targetKey) {
            return new ChargeAttempt(previous.success(), false);
        }

        boolean success = AirtightExtendArmUtils.tryConsumeAndRefresh(player);
        LAST_INTERACTION_CHARGES.put(player, new InteractionCharge(gameTime, type, targetKey, success));
        return new ChargeAttempt(success, true);
    }

    private static void displayWarningOnFirstAttempt(Player player, ChargeAttempt attempt) {
        if (!attempt.firstAttempt()) {
            return;
        }

        displayInsufficientGasWarning(player);
    }

    private static void displayInsufficientGasWarning(Player player) {
        GasStack gasContent = CanisterContainerSuppliers.getFirstAvailableGasContent(player);
        if (gasContent.isEmpty()) {
            GasCanisterUtils.displayCustomWarningHint(player, "gui.warnings.insufficient_gas");
            return;
        }

        GasCanisterUtils.displayCustomWarningHint(player, "gui.warnings.insufficient_gas", gasContent.getHoverName());
    }

    private enum InteractionType {
        BLOCK,
        ENTITY
    }

    private record InteractionCharge(long gameTime, InteractionType type, long targetKey, boolean success) {}

    private record ChargeAttempt(boolean success, boolean firstAttempt) {}
}
