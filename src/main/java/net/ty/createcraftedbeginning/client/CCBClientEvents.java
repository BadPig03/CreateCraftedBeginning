package net.ty.createcraftedbeginning.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.DefaultSuperRenderTypeBuffer;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.ClientTickEvent.Pre;
import net.neoforged.neoforge.client.event.ContainerScreenEvent.Render.Foreground;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers;
import net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.CreateCraftedBeginningClient;
import net.ty.createcraftedbeginning.client.CCBCreativeTabBanners.BannerLayout;
import net.ty.createcraftedbeginning.client.outliner.CCBOutliner;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.AirtightChestplateFirstPersonRenderer;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.AirtightChestplateLayer;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.AirtightLeggingsLayer;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.AirtightCannonItemRenderer;
import net.ty.createcraftedbeginning.content.airtights.airtightencasedpipe.AirtightEncasedPipeOutlineRenderer;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.AirtightHandheldDrillOutlineRenderer;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterOverlay;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberRecipeIndex;
import net.ty.createcraftedbeginning.mixin.client.accessor.CreativeModeInventoryScreenAccessor;
import net.ty.createcraftedbeginning.mixin.client.accessor.ItemPickerMenuAccessor;
import net.ty.createcraftedbeginning.ponder.CCBPonderPlugin;
import net.ty.createcraftedbeginning.registry.CCBCreativeTabLayout;
import net.ty.createcraftedbeginning.registry.CCBCreativeTabLayout.PositionedSection;
import net.ty.createcraftedbeginning.registry.CCBCreativeTabs;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID, value = Dist.CLIENT)
public class CCBClientEvents {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            PonderIndex.addPlugin(new CCBPonderPlugin());
            GasCanisterPackClientOverrides.register(CCBItems.GAS_CANISTER_PACK.get());
        });
    }

    @SubscribeEvent
    public static void onRecipesUpdated(RecipesUpdatedEvent event) {
        BreezeChamberRecipeIndex.rebuild(event.getRecipeManager());
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
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != Stage.AFTER_PARTICLES) {
            return;
        }

        onRenderWorld(event.getPoseStack());
    }

    @SubscribeEvent
    public static void registerItemDecorations(RegisterItemDecorationsEvent event) {
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
        if (event.getEntity() == null || !AllConfigs.client().tooltips.get()) {
            return;
        }

        CCBClientRecipeUtils.addSequencedAssemblyTooltip(event);
    }

    @SubscribeEvent
    static void onRegisterAdditionalModels(RegisterAdditional event) {
        CCBPartialModels.registerBalloons();
    }

    @SubscribeEvent
    public static void onRegisterItemColors(Item event) {
        event.register((stack, tintIndex) -> stack.getOrDefault(CCBDataComponents.GAS_VIRTUAL_ITEM_COLOR, 0xFFFFFFFF), CCBItems.GAS_VIRTUAL_ITEM.get());
        event.register((stack, tintIndex) -> tintIndex != 0 ? 0xFFFFFFFF : stack.getOrDefault(CCBDataComponents.GAS_INJECTION_CHAMBER_FILTER_COLOR, 0xFFFFFFFF), CCBItems.GAS_INJECTION_CHAMBER_FILTER.get());
    }

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, GasCanisterOverlay.RESOURCE, GasCanisterOverlay.INSTANCE);
    }

    @SubscribeEvent
    public static void onRenderForeground(Foreground event) {
        if (!(event.getContainerScreen() instanceof CreativeModeInventoryScreen screen) || !(screen instanceof CreativeModeInventoryScreenAccessor accessor) || !(screen.getMenu() instanceof ItemPickerMenuAccessor menuAccessor)) {
            return;
        }

        CreativeModeTab selectedTab = CreativeModeInventoryScreenAccessor.ccb$getSelectedTab();
        if (selectedTab != CCBCreativeTabs.CREATIVE_TAB.get()) {
            return;
        }

        float scrollOffset = accessor.ccb$getScrollOffs();
        int firstVisibleRow = menuAccessor.ccb$getRowIndexForScroll(scrollOffset);
        for (PositionedSection section : CCBCreativeTabLayout.positionedSections()) {
            int visibleRow = section.bannerRow() - firstVisibleRow;
            if (visibleRow < 0 || visibleRow >= CCBCreativeTabLayout.VISIBLE_ROW_COUNT) {
                continue;
            }

            BannerLayout banner = CCBCreativeTabBanners.getBanner(section.section());
            CCBCreativeTabBanners.render(event.getGuiGraphics(), banner, visibleRow);
        }
    }

    private static void onTick(boolean isPreEvent) {
        if (isPreEvent || Minecraft.getInstance().level == null || Minecraft.getInstance().player == null) {
            return;
        }

        GasFilteringRenderer.tick();
        AirtightEncasedPipeOutlineRenderer.tick();
        AirtightHandheldDrillOutlineRenderer.tick();
        AirtightChestplateFirstPersonRenderer.tick();

        CreateCraftedBeginningClient.AIRTIGHT_CANNON_RENDER_HANDLER.tick();
        CreateCraftedBeginningClient.AIRTIGHT_EXTEND_ARM_RENDER_HANDLER.tick();
        CreateCraftedBeginningClient.AIRTIGHT_HAND_DRILL_RENDER_HANDLER.tick();

        CCBOutliner.INSTANCE.tickOutlines();
    }

    private static void onRenderWorld(PoseStack ms) {
        ms.pushPose();

        SuperRenderTypeBuffer buffer = DefaultSuperRenderTypeBuffer.getInstance();
        CCBOutliner.INSTANCE.renderOutlines(ms, buffer, Minecraft.getInstance().gameRenderer.getMainCamera().getPosition(), AnimationTickHolder.getPartialTicks());
        buffer.draw();

        ms.popPose();
    }
}
