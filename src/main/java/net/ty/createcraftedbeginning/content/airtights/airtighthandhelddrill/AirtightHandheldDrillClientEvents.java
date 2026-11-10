package net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;
import net.ty.createcraftedbeginning.api.CCBAPI;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID, value = Dist.CLIENT)
final class AirtightHandheldDrillClientEvents {
    private AirtightHandheldDrillClientEvents() {
    }

    @SubscribeEvent
    private static void onClientPostTick(Post event) {
        AirtightHandheldDrillRenderHandler.INSTANCE.tick();
    }

    @SubscribeEvent
    private static void onLogout(LoggingOut event) {
        AirtightHandheldDrillRenderHandler.INSTANCE.reset();
        AirtightHandheldDrillOutlineRenderer.clear();
    }

    @SubscribeEvent
    private static void onLevelUnload(Unload event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }

        AirtightHandheldDrillOutlineRenderer.clear();
    }
}
