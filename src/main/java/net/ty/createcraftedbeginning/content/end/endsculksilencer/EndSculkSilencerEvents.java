package net.ty.createcraftedbeginning.content.end.endsculksilencer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.level.LevelEvent.Load;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;
import net.neoforged.neoforge.event.tick.LevelTickEvent.Post;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID, value = Dist.CLIENT)
public class EndSculkSilencerEvents {
    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound == null) {
            return;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        BlockPos soundPos = BlockPos.containing(sound.getX(), sound.getY(), sound.getZ());
        String dimension = level.dimension().location().toString();
        if (!ClientEndSculkSilencerCache.INSTANCE.checkWithinRange(soundPos, dimension)) {
            return;
        }

        event.setSound(null);
    }

    @SubscribeEvent
    public static void onClientLoggingOut(LoggingOut event) {
        ClientEndSculkSilencerCache.INSTANCE.clear();
    }

    @SubscribeEvent
    public static void onPostTick(Post event) {
        Level level = event.getLevel();
        if (level.isClientSide) {
            return;
        }

        GlobalEndSculkSilencerManager.tick(level);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        GlobalEndSculkSilencerManager.sendToClients(serverPlayer);
    }

    @SubscribeEvent
    public static void onLoad(Load event) {
        LevelAccessor level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }

        GlobalEndSculkSilencerManager.clear();
    }

    @SubscribeEvent
    public static void onUnload(Unload event) {
        LevelAccessor level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }

        GlobalEndSculkSilencerManager.clear();
    }
}
