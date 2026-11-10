package net.ty.createcraftedbeginning.content.opticalpower.amethystsoundsuppressor;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent.Post;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID)
final class AmethystSoundSuppressorEvents {
    private AmethystSoundSuppressorEvents() {
    }

    @SubscribeEvent
    private static void onServerStarting(ServerStartingEvent event) {
        AmethystSoundSuppressorManager.clear();
    }

    @SubscribeEvent
    private static void onPostTick(Post event) {
        AmethystSoundSuppressorManager.tick(event.getLevel());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    private static void onContraptionJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel) || !(event.getEntity() instanceof AbstractContraptionEntity entity)) {
            return;
        }

        AmethystSoundSuppressorMovementBehaviour.refreshContraption(entity);
    }

    @SubscribeEvent
    private static void onContraptionLeave(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof AbstractContraptionEntity entity)) {
            return;
        }

        AmethystSoundSuppressorManager.removeContraption(level, entity.getUUID());
    }

    @SubscribeEvent
    private static void onVibrationSuppressed(AmethystSoundSuppressionEvent event) {
        if (!(event.getContext().sourceEntity() instanceof Player player) || CCBAdvancements.STEVES_REDEMPTION.isAlreadyAwardedTo(player)) {
            return;
        }

        ServerLevel level = event.getLevel();
        BlockPos sourcePos = SubLevelBridge.resolve(level, event.getSourcePosition()).blockPos();
        int notificationRadius = event.getGameEvent().value().notificationRadius();
        if (level.getEntitiesOfClass(Warden.class, new AABB(sourcePos).inflate(notificationRadius)).isEmpty()) {
            return;
        }

        CCBAdvancements.STEVES_REDEMPTION.awardTo(player);
    }

    @SubscribeEvent
    private static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        syncPlayer(event.getEntity());
    }

    @SubscribeEvent
    private static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
        syncPlayer(event.getEntity());
    }

    @SubscribeEvent
    private static void onPlayerRespawn(PlayerRespawnEvent event) {
        syncPlayer(event.getEntity());
    }

    @SubscribeEvent
    private static void onUnload(Unload event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        AmethystSoundSuppressorManager.removeDimension(serverLevel);
    }

    private static void syncPlayer(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        AmethystSoundSuppressorManager.sendToClient(serverPlayer);
    }
}
