package net.ty.createcraftedbeginning.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.DefaultSuperRenderTypeBuffer;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.ClientTickEvent.Pre;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.CreateCraftedBeginningClient;
import net.ty.createcraftedbeginning.api.gas.GasFilteringRenderer;
import net.ty.createcraftedbeginning.api.outliner.CCBOutliner;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightChestplateLayer;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.AirtightLeggingsLayer;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.AirtightCannonItemRenderer;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.AirtightHandheldDrillBlockRender;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterOverlay;
import net.ty.createcraftedbeginning.ponder.CCBPonderPlugin;
import net.ty.createcraftedbeginning.recipe.SequencedAssemblyWithGasRecipe;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID, value = Dist.CLIENT)
public class CCBClientEvents {
    @SubscribeEvent
    public static void onClientSetup(@NotNull FMLClientSetupEvent event) {
        event.enqueueWork(() -> PonderIndex.addPlugin(new CCBPonderPlugin()));
    }

    @SubscribeEvent
    public static void onTickPost(Post event) {
        onTick(false);
    }

    @SubscribeEvent
    public static void onTickPre(Pre event) {
        onTick(true);
    }

    @SubscribeEvent
    public static void onRenderWorld(@NotNull RenderLevelStageEvent event) {
        if (event.getStage() != Stage.AFTER_PARTICLES) {
            return;
        }

        onRenderWorld(event.getPoseStack());
    }

    @SubscribeEvent
    public static void registerItemDecorations(@NotNull RegisterItemDecorationsEvent event) {
        event.register(CCBItems.AIRTIGHT_CANNON, AirtightCannonItemRenderer.DECORATOR);
    }

    @SubscribeEvent
    public static void addEntityRendererLayers(AddLayers event) {
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        AirtightChestplateLayer.registerOnAll(dispatcher);
        AirtightLeggingsLayer.registerOnAll(dispatcher);
    }

    @SubscribeEvent
    public static void addToItemTooltip(ItemTooltipEvent event) {
        if (!AllConfigs.client().tooltips.get()) {
            return;
        }
        if (event.getEntity() == null) {
            return;
        }

        SequencedAssemblyWithGasRecipe.addToTooltip(event);
    }

    @SubscribeEvent
    public static void registerGuiOverlays(@NotNull RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, GasCanisterOverlay.RESOURCE, GasCanisterOverlay.INSTANCE);
    }

    public static void onTick(boolean isPreEvent) {
        if (Minecraft.getInstance().level == null || Minecraft.getInstance().player == null || isPreEvent) {
            return;
        }

        GasFilteringRenderer.tick();
        AirtightHandheldDrillBlockRender.tick();

        CreateCraftedBeginningClient.AIRTIGHT_CANNON_RENDER_HANDLER.tick();
        CreateCraftedBeginningClient.AIRTIGHT_HAND_DRILL_RENDER_HANDLER.tick();
        CreateCraftedBeginningClient.CINDER_INCINERATION_BLOWER_OUTLINER.tick();

        CCBOutliner.INSTANCE.tickOutlines();
    }

    private static void onRenderWorld(@NotNull PoseStack ms) {
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        float partialTicks = AnimationTickHolder.getPartialTicks();

        ms.pushPose();
        SuperRenderTypeBuffer buffer = DefaultSuperRenderTypeBuffer.getInstance();

        CCBOutliner.INSTANCE.renderOutlines(ms, buffer, cameraPos, partialTicks);

        buffer.draw();
        ms.popPose();
    }
}
