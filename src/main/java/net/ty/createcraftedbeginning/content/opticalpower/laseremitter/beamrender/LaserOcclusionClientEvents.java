package net.ty.createcraftedbeginning.content.opticalpower.laseremitter.beamrender;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent.Post;
import net.neoforged.neoforge.client.event.RenderFrameEvent.Pre;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.opticalpower.laser.trace.LaserRenderFrame;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID, value = Dist.CLIENT)
final class LaserOcclusionClientEvents {
    private LaserOcclusionClientEvents() {
    }

    @SubscribeEvent
    private static void onFrameStart(Pre event) {
        LaserRenderFrame.begin(Minecraft.getInstance().level);
    }

    @SubscribeEvent
    private static void onFrameEnd(Post event) {
        LaserRenderFrame.clear();
    }

    @SubscribeEvent
    private static void onReload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> LaserRenderFrame.clear());
    }

    @SubscribeEvent
    private static void onShutdown(GameShuttingDownEvent event) {
        LaserRenderFrame.clear();
    }

    @SubscribeEvent
    private static void onLogout(LoggingOut event) {
        LaserRenderFrame.clear();
        LaserEmitterRenderer.clearCaches();
    }
}
