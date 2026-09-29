package net.ty.createcraftedbeginning.client;

import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
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
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.client.CCBCreativeTabBanners.BannerLayout;
import net.ty.createcraftedbeginning.client.gas.GasFilteringRenderer;
import net.ty.createcraftedbeginning.client.render.CCBPartialModels;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.AirtightChestplateFirstPersonRenderer;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.AirtightChestplateLayer;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.AirtightLeggingsLayer;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.AirtightCannonItemRenderer;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.AirtightCannonRenderHandler;
import net.ty.createcraftedbeginning.content.airtights.airtightencasedpipe.AirtightEncasedPipeOutlineRenderer;
import net.ty.createcraftedbeginning.content.airtights.airtightextendarm.AirtightExtendArmRenderHandler;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.AirtightHandheldDrillOutlineRenderer;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.AirtightHandheldDrillRenderHandler;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterOverlay;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackClientOverrides;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberRecipeIndex;
import net.ty.createcraftedbeginning.platform.client.CreativeInventoryBridge;
import net.ty.createcraftedbeginning.platform.client.CreativeInventoryBridge.View;
import net.ty.createcraftedbeginning.ponder.CCBPonderPlugin;
import net.ty.createcraftedbeginning.recipe.ReactorKettleBrewingRecipes;
import net.ty.createcraftedbeginning.registry.CCBCreativeTabLayout;
import net.ty.createcraftedbeginning.registry.CCBCreativeTabLayout.PositionedSection;
import net.ty.createcraftedbeginning.registry.CCBCreativeTabs;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID, value = Dist.CLIENT)
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
        ReactorKettleBrewingRecipes.invalidateCaches();
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
    public static void onRegisterAdditionalModels(RegisterAdditional event) {
        CCBPartialModels.registerBalloons();
    }

    @SubscribeEvent
    public static void onRegisterItemColors(Item event) {
        event.register((stack, tintIndex) -> stack.getOrDefault(CCBDataComponents.GAS_VIRTUAL_ITEM_COLOR, 0xFFFFFFFF), CCBItems.GAS_VIRTUAL_ITEM.get());
        event.register((stack, tintIndex) -> {
            if (tintIndex != 0) {
                return 0xFFFFFFFF;
            }

            return stack.getOrDefault(CCBDataComponents.GAS_INJECTION_CHAMBER_FILTER_COLOR, 0xFFFFFFFF);
        }, CCBItems.GAS_INJECTION_CHAMBER_FILTER.get());
    }

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, GasCanisterOverlay.RESOURCE, GasCanisterOverlay.INSTANCE);
    }

    @SubscribeEvent
    public static void onRenderForeground(Foreground event) {
        if (!(event.getContainerScreen() instanceof CreativeModeInventoryScreen screen)) {
            return;
        }

        View creativeView = CreativeInventoryBridge.getView(screen);
        if (creativeView == null || creativeView.selectedTab() != CCBCreativeTabs.CREATIVE_TAB.get()) {
            return;
        }

        int firstVisibleRow = creativeView.firstVisibleRow();
        for (PositionedSection section : CCBCreativeTabLayout.positionedSections()) {
            int visibleRow = section.bannerRow() - firstVisibleRow;
            if (visibleRow < 0 || visibleRow >= CCBCreativeTabLayout.VISIBLE_ROW_COUNT) {
                continue;
            }

            BannerLayout banner = CCBCreativeTabBanners.getBanner(section.section());
            CCBCreativeTabBanners.render(event.getGuiGraphics(), banner, visibleRow);
        }
    }

    private static void onTick(boolean isPreTick) {
        if (isPreTick || Minecraft.getInstance().level == null || Minecraft.getInstance().player == null) {
            return;
        }

        GasFilteringRenderer.tick();
        AirtightEncasedPipeOutlineRenderer.tick();
        AirtightHandheldDrillOutlineRenderer.tick();
        AirtightChestplateFirstPersonRenderer.tick();

        AirtightCannonRenderHandler.INSTANCE.tick();
        AirtightExtendArmRenderHandler.INSTANCE.tick();
        AirtightHandheldDrillRenderHandler.INSTANCE.tick();

    }

}
