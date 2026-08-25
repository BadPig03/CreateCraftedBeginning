package net.ty.createcraftedbeginning.content.end.endsculksilencer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Position;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.GameEventTags;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.VanillaGameEvent;
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
public final class EndSculkSilencerEvents {
    private EndSculkSilencerEvents() {
    }

    @SubscribeEvent
    private static void onServerStarting(ServerStartingEvent event) {
        GlobalEndSculkSilencerManager.clear();
    }

    @SubscribeEvent
    private static void onPostTick(Post event) {
        GlobalEndSculkSilencerManager.tick(event.getLevel());
    }

    @SubscribeEvent
    private static void onVanillaGameEvent(VanillaGameEvent event) {
        if (!isSilenceableGameEvent(event.getVanillaEvent())) {
            return;
        }

        ResourceLocation dimension = event.getLevel().dimension().location();
        if (!GlobalEndSculkSilencerManager.hasCoverage(dimension)) {
            return;
        }

        BlockPos sourcePos = SubLevelBridge.resolve(event.getLevel(), event.getEventPosition()).blockPos();
        if (!GlobalEndSculkSilencerManager.checkWithinRange(sourcePos, dimension)) {
            return;
        }

        awardWardenProtection(event, sourcePos);
        event.setCanceled(true);
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

        GlobalEndSculkSilencerManager.removeDimension(serverLevel);
    }

    public static boolean isSilenceableGameEvent(Holder<GameEvent> gameEvent) {
        return gameEvent.is(GameEventTags.VIBRATIONS) || gameEvent.is(GameEventTags.WARDEN_CAN_LISTEN) || gameEvent.is(GameEventTags.SHRIEKER_CAN_LISTEN) || gameEvent.is(GameEventTags.ALLAY_CAN_LISTEN);
    }

    public static boolean hasSilencerCoverage(Level level) {
        return GlobalEndSculkSilencerManager.hasCoverage(level.dimension().location());
    }

    public static boolean isWithinSilencedArea(Level level, Position position) {
        ResourceLocation dimension = level.dimension().location();
        return GlobalEndSculkSilencerManager.hasCoverage(dimension) && GlobalEndSculkSilencerManager.checkWithinRange(SubLevelBridge.resolve(level, position).blockPos(), dimension);
    }

    private static void syncPlayer(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        GlobalEndSculkSilencerManager.sendToClient(serverPlayer);
    }

    private static void awardWardenProtection(VanillaGameEvent event, BlockPos sourcePos) {
        if (!(event.getCause() instanceof Player player) || !(event.getLevel() instanceof ServerLevel serverLevel) || CCBAdvancements.STEVES_REDEMPTION.isAlreadyAwardedTo(player)) {
            return;
        }

        int notificationRadius = event.getVanillaEvent().value().notificationRadius();
        if (serverLevel.getEntitiesOfClass(Warden.class, new AABB(sourcePos).inflate(notificationRadius)).isEmpty()) {
            return;
        }

        CCBAdvancements.STEVES_REDEMPTION.awardTo(player);
    }
}
