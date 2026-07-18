package net.ty.createcraftedbeginning.content.end.endsculksilencer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.GameEventTags;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
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
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID)
public final class EndSculkSilencerEvents {
    private EndSculkSilencerEvents() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        GlobalEndSculkSilencerManager.clear();
    }

    @SubscribeEvent
    public static void onPostTick(Post event) {
        GlobalEndSculkSilencerManager.tick(event.getLevel());
    }

    @SubscribeEvent
    public static void onVanillaGameEvent(VanillaGameEvent event) {
        if (!isSilenceableGameEvent(event.getVanillaEvent())) {
            return;
        }

        BlockPos sourcePos = BlockPos.containing(event.getEventPosition());
        String dimension = event.getLevel().dimension().location().toString();
        if (!GlobalEndSculkSilencerManager.checkWithinRange(sourcePos, dimension)) {
            return;
        }

        awardWardenProtection(event, sourcePos);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        syncPlayer(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
        syncPlayer(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerRespawnEvent event) {
        syncPlayer(event.getEntity());
    }

    @SubscribeEvent
    public static void onUnload(Unload event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        GlobalEndSculkSilencerManager.removeDimension(serverLevel);
    }

    private static boolean isSilenceableGameEvent(Holder<GameEvent> gameEvent) {
        return gameEvent.is(GameEventTags.VIBRATIONS) || gameEvent.is(GameEventTags.WARDEN_CAN_LISTEN) || gameEvent.is(GameEventTags.SHRIEKER_CAN_LISTEN) || gameEvent.is(GameEventTags.ALLAY_CAN_LISTEN);
    }

    private static void syncPlayer(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        GlobalEndSculkSilencerManager.sendToClient(serverPlayer);
    }

    private static void awardWardenProtection(VanillaGameEvent event, BlockPos sourcePos) {
        if (!(event.getCause() instanceof Player player) || !(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        int radius = event.getVanillaEvent().value().notificationRadius();
        if (serverLevel.getEntitiesOfClass(Warden.class, new AABB(sourcePos).inflate(radius)).isEmpty()) {
            return;
        }

        CCBAdvancements.STEVES_REDEMPTION.awardTo(player);
    }
}
