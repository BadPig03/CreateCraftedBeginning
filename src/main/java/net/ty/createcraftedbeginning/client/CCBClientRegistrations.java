package net.ty.createcraftedbeginning.client;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.kinetics.base.OrientedRotatingVisual;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.foundation.block.render.CustomBlockModels;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.client.blockextensions.AirtightForgingPressClientExtensions;
import net.ty.createcraftedbeginning.client.blockextensions.AirtightForgingPressStructuralClientExtensions;
import net.ty.createcraftedbeginning.client.blockextensions.AirtightReactorKettleClientExtensions;
import net.ty.createcraftedbeginning.client.blockextensions.AirtightReactorKettleStructuralClientExtensions;
import net.ty.createcraftedbeginning.client.blockextensions.TeslaTurbineClientExtensions;
import net.ty.createcraftedbeginning.client.blockextensions.TeslaTurbineStructuralClientExtensions;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.AirCompressorRenderer;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightboots.AirtightBootScreen;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightchestplate.AirtightChestplateScreen;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtighthelmet.AirtightHelmetScreen;
import net.ty.createcraftedbeginning.content.airtights.airtightarmors.airtightleggings.AirtightLeggingsScreen;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.windcharge.AirtightCannonWindChargeProjectileEntityRenderer;
import net.ty.createcraftedbeginning.content.airtights.airtightengine.AirtightEngineRenderer;
import net.ty.createcraftedbeginning.content.airtights.airtightengine.AirtightEngineVisual;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressRenderer;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressStructuralShaftRenderer;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressStructuralShaftVisual;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressVisual;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.AirtightHandheldDrillScreen;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeAttachmentModel;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpRenderer;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleRenderer;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleStructuralCogRenderer;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleStructuralCogVisual;
import net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle.AirtightReactorKettleVisual;
import net.ty.createcraftedbeginning.content.airtights.airvents.AirVentRenderer;
import net.ty.createcraftedbeginning.content.airtights.airvents.AirVentVisual;
import net.ty.createcraftedbeginning.content.airtights.gascanisterpack.GasCanisterPackScreen;
import net.ty.createcraftedbeginning.content.airtights.gasfactorygauge.GasFactoryGaugeModel;
import net.ty.createcraftedbeginning.content.airtights.gasfactorygauge.GasFactoryGaugeRenderer;
import net.ty.createcraftedbeginning.content.airtights.gasfactorygauge.GasFactoryGaugeSetGasScreen;
import net.ty.createcraftedbeginning.content.airtights.gasfilter.GasFilterScreen;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberRenderer;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberVisual;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasPackagerRenderer;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.GasPackagerVisual;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerRenderer;
import net.ty.createcraftedbeginning.content.airtights.gaspackager.gasrepackager.GasRepackagerVisual;
import net.ty.createcraftedbeginning.content.airtights.portablegasinterface.PortableGasInterfaceRenderer;
import net.ty.createcraftedbeginning.content.airtights.portablegasinterface.PortableGasInterfaceVisual;
import net.ty.createcraftedbeginning.content.airtights.smartairtightpipe.SmartAirtightPipeRenderer;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineRenderer;
import net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineVisual;
import net.ty.createcraftedbeginning.content.airtights.weatherflares.projectile.WeatherFlareProjectileRenderer;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberRenderer;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.BreezeChamberVisual;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerRenderer;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerVisual;
import net.ty.createcraftedbeginning.content.end.endincinerationblower.EndIncinerationBlowerRenderer;
import net.ty.createcraftedbeginning.content.end.endincinerationblower.EndIncinerationBlowerStructuralRenderer;
import net.ty.createcraftedbeginning.content.end.endsculksilencer.EndSculkSilencerRenderer;
import net.ty.createcraftedbeginning.content.end.endsculksilencer.EndSculkSilencerVisual;
import net.ty.createcraftedbeginning.content.obsolete.pneumaticengine.PneumaticEngineRenderer;
import net.ty.createcraftedbeginning.content.photostresses.phohostressbearing.PhotoStressBearingRenderer;
import net.ty.createcraftedbeginning.foundation.client.CCBPartialModels;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBEntityTypes;
import net.ty.createcraftedbeginning.registry.CCBMenuTypes;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBClientRegistrations {
    private CCBClientRegistrations() {
    }

    public static void initialize() {
        CCBPartialModels.register();

        CustomBlockModels models = CreateClient.MODEL_SWAPPER.getCustomBlockModels();
        models.register(CCBAPI.asResource("airtight_pipe"), AirtightPipeAttachmentModel::withAO);
        models.register(CCBAPI.asResource("airtight_check_valve"), AirtightPipeAttachmentModel::withAO);
        models.register(CCBAPI.asResource("smart_airtight_pipe"), AirtightPipeAttachmentModel::withAO);
        models.register(CCBAPI.asResource("airtight_pump"), AirtightPipeAttachmentModel::withAO);
        models.register(CCBAPI.asResource("gas_factory_gauge"), GasFactoryGaugeModel::new);
    }

    public static void registerBlockExtensions(RegisterClientExtensionsEvent event) {
        event.registerBlock(new TeslaTurbineClientExtensions(), CCBBlocks.TESLA_TURBINE_BLOCK.get());
        event.registerBlock(new TeslaTurbineStructuralClientExtensions(), CCBBlocks.TESLA_TURBINE_STRUCTURAL_BLOCK.get());
        event.registerBlock(new AirtightReactorKettleClientExtensions(), CCBBlocks.AIRTIGHT_REACTOR_KETTLE_BLOCK.get());
        event.registerBlock(new AirtightReactorKettleStructuralClientExtensions(), CCBBlocks.AIRTIGHT_REACTOR_KETTLE_STRUCTURAL_BLOCK.get(), CCBBlocks.AIRTIGHT_REACTOR_KETTLE_STRUCTURAL_COG_BLOCK.get());
        event.registerBlock(new AirtightForgingPressClientExtensions(), CCBBlocks.AIRTIGHT_FORGING_PRESS_BLOCK.get());
        event.registerBlock(new AirtightForgingPressStructuralClientExtensions(), CCBBlocks.AIRTIGHT_FORGING_PRESS_STRUCTURAL_BLOCK.get(), CCBBlocks.AIRTIGHT_FORGING_PRESS_STRUCTURAL_SHAFT_BLOCK.get());
    }

    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(CCBMenuTypes.AIRTIGHT_HELMET_MENU.get(), AirtightHelmetScreen::new);
        event.register(CCBMenuTypes.AIRTIGHT_CHESTPLATE_MENU.get(), AirtightChestplateScreen::new);
        event.register(CCBMenuTypes.AIRTIGHT_LEGGINGS_MENU.get(), AirtightLeggingsScreen::new);
        event.register(CCBMenuTypes.AIRTIGHT_BOOTS_MENU.get(), AirtightBootScreen::new);
        event.register(CCBMenuTypes.AIRTIGHT_HANDHELD_DRILL_MENU.get(), AirtightHandheldDrillScreen::new);
        event.register(CCBMenuTypes.GAS_CANISTER_PACK_MENU.get(), GasCanisterPackScreen::new);
        event.register(CCBMenuTypes.GAS_FILTER_MENU.get(), GasFilterScreen::new);
        event.register(CCBMenuTypes.GAS_FACTORY_GAUGE_SET_GAS_MENU.get(), GasFactoryGaugeSetGasScreen::new);
    }

    public static void registerRenderers(RegisterRenderers event) {
        event.registerBlockEntityRenderer(CCBBlockEntities.SMART_AIRTIGHT_PIPE.get(), SmartAirtightPipeRenderer::new);
        event.registerBlockEntityRenderer(CCBBlockEntities.AIRTIGHT_PUMP.get(), AirtightPumpRenderer::new);
        event.registerBlockEntityRenderer(CCBBlockEntities.GAS_INJECTION_CHAMBER.get(), GasInjectionChamberRenderer::new);
        event.registerBlockEntityRenderer(CCBBlockEntities.PORTABLE_GAS_INTERFACE.get(), PortableGasInterfaceRenderer::new);
        event.registerBlockEntityRenderer(CCBBlockEntities.BREEZE_COOLER.get(), BreezeCoolerRenderer::new);
        event.registerBlockEntityRenderer(CCBBlockEntities.BREEZE_CHAMBER.get(), BreezeChamberRenderer::new);
        event.registerBlockEntityRenderer(CCBBlockEntities.AIR_COMPRESSOR.get(), AirCompressorRenderer::new);
        event.registerBlockEntityRenderer(CCBBlockEntities.AIRTIGHT_ENGINE.get(), AirtightEngineRenderer::new);
        event.registerBlockEntityRenderer(CCBBlockEntities.TESLA_TURBINE.get(), TeslaTurbineRenderer::new);
        event.registerBlockEntityRenderer(CCBBlockEntities.AIRTIGHT_REACTOR_KETTLE.get(), AirtightReactorKettleRenderer::new);
        event.registerBlockEntityRenderer(CCBBlockEntities.AIRTIGHT_REACTOR_KETTLE_STRUCTURAL.get(), SmartBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(CCBBlockEntities.AIRTIGHT_REACTOR_KETTLE_STRUCTURAL_COG.get(), AirtightReactorKettleStructuralCogRenderer::new);
        event.registerBlockEntityRenderer(CCBBlockEntities.AIRTIGHT_FORGING_PRESS.get(), AirtightForgingPressRenderer::new);
        event.registerBlockEntityRenderer(CCBBlockEntities.AIRTIGHT_FORGING_PRESS_STRUCTURAL.get(), SmartBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(CCBBlockEntities.AIRTIGHT_FORGING_PRESS_STRUCTURAL_SHAFT.get(), AirtightForgingPressStructuralShaftRenderer::new);
        event.registerBlockEntityRenderer(CCBBlockEntities.GAS_PACKAGER.get(), GasPackagerRenderer::new);
        event.registerBlockEntityRenderer(CCBBlockEntities.GAS_REPACKAGER.get(), GasRepackagerRenderer::new);
        event.registerBlockEntityRenderer(CCBBlockEntities.GAS_FACTORY_GAUGE.get(), GasFactoryGaugeRenderer::new);
        event.registerBlockEntityRenderer(CCBBlockEntities.PHOTO_STRESS_BEARING.get(), PhotoStressBearingRenderer::new);
        event.registerBlockEntityRenderer(CCBBlockEntities.END_INCINERATION_BLOWER.get(), EndIncinerationBlowerRenderer::new);
        event.registerBlockEntityRenderer(CCBBlockEntities.END_INCINERATION_BLOWER_STRUCTURAL.get(), EndIncinerationBlowerStructuralRenderer::new);
        event.registerBlockEntityRenderer(CCBBlockEntities.END_SCULK_SILENCER.get(), EndSculkSilencerRenderer::new);
        event.registerBlockEntityRenderer(CCBBlockEntities.ANDESITE_CRATE.get(), SmartBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(CCBBlockEntities.BRASS_CRATE.get(), SmartBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(CCBBlockEntities.STURDY_CRATE.get(), SmartBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(CCBBlockEntities.CARDBOARD_CRATE.get(), SmartBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(CCBBlockEntities.AIR_VENT.get(), AirVentRenderer::new);
        event.registerBlockEntityRenderer(CCBBlockEntities.PNEUMATIC_ENGINE.get(), PneumaticEngineRenderer::new);

        event.registerEntityRenderer(CCBEntityTypes.AIRTIGHT_CANNON_WIND_CHARGE_PROJECTILE.get(), AirtightCannonWindChargeProjectileEntityRenderer::new);
        event.registerEntityRenderer(CCBEntityTypes.WEATHER_FLARE_PROJECTILE.get(), WeatherFlareProjectileRenderer::new);
    }

    public static void registerVisualizers(FMLClientSetupEvent ignoredEvent) {
        SimpleBlockEntityVisualizer.builder(CCBBlockEntities.AIR_VENT.get()).factory(AirVentVisual::new).skipVanillaRender(be -> true).apply();
        SimpleBlockEntityVisualizer.builder(CCBBlockEntities.AIR_COMPRESSOR.get()).factory(SingleAxisRotatingVisual.of(CCBPartialModels.SHAFT_HALF_UP)).skipVanillaRender(be -> true).apply();
        SimpleBlockEntityVisualizer.builder(CCBBlockEntities.AIRTIGHT_ENGINE.get()).factory(AirtightEngineVisual::new).skipVanillaRender(be -> true).apply();
        SimpleBlockEntityVisualizer.builder(CCBBlockEntities.AIRTIGHT_FORGING_PRESS.get()).factory(AirtightForgingPressVisual::new).neverSkipVanillaRender().apply();
        SimpleBlockEntityVisualizer.builder(CCBBlockEntities.AIRTIGHT_FORGING_PRESS_STRUCTURAL_SHAFT.get()).factory(AirtightForgingPressStructuralShaftVisual::new).skipVanillaRender(be -> true).apply();
        SimpleBlockEntityVisualizer.builder(CCBBlockEntities.AIRTIGHT_PUMP.get()).factory(SingleAxisRotatingVisual.ofZ(CCBPartialModels.AIRTIGHT_PUMP_COGS)).skipVanillaRender(be -> true).apply();
        SimpleBlockEntityVisualizer.builder(CCBBlockEntities.AIRTIGHT_REACTOR_KETTLE.get()).factory(AirtightReactorKettleVisual::new).neverSkipVanillaRender().apply();
        SimpleBlockEntityVisualizer.builder(CCBBlockEntities.AIRTIGHT_REACTOR_KETTLE_STRUCTURAL_COG.get()).factory(AirtightReactorKettleStructuralCogVisual::new).skipVanillaRender(be -> true).apply();
        SimpleBlockEntityVisualizer.builder(CCBBlockEntities.BREEZE_CHAMBER.get()).factory(BreezeChamberVisual::new).skipVanillaRender(be -> true).apply();
        SimpleBlockEntityVisualizer.builder(CCBBlockEntities.BREEZE_COOLER.get()).factory(BreezeCoolerVisual::new).skipVanillaRender(be -> true).apply();
        SimpleBlockEntityVisualizer.builder(CCBBlockEntities.END_INCINERATION_BLOWER.get()).factory(SingleAxisRotatingVisual.of(CCBPartialModels.END_INCINERATION_BLOWER_CORE)).skipVanillaRender(be -> true).apply();
        SimpleBlockEntityVisualizer.builder(CCBBlockEntities.END_INCINERATION_BLOWER_STRUCTURAL.get()).factory(SingleAxisRotatingVisual.of(CCBPartialModels.SHAFT_HALF_DOWN)).skipVanillaRender(be -> true).apply();
        SimpleBlockEntityVisualizer.builder(CCBBlockEntities.END_SCULK_SILENCER.get()).factory(EndSculkSilencerVisual::new).skipVanillaRender(be -> true).apply();
        SimpleBlockEntityVisualizer.builder(CCBBlockEntities.GAS_INJECTION_CHAMBER.get()).factory(GasInjectionChamberVisual::new).neverSkipVanillaRender().apply();
        SimpleBlockEntityVisualizer.builder(CCBBlockEntities.GAS_PACKAGER.get()).factory(GasPackagerVisual::new).neverSkipVanillaRender().apply();
        SimpleBlockEntityVisualizer.builder(CCBBlockEntities.GAS_REPACKAGER.get()).factory(GasRepackagerVisual::new).neverSkipVanillaRender().apply();
        SimpleBlockEntityVisualizer.builder(CCBBlockEntities.PHOTO_STRESS_BEARING.get()).factory((ctx, blockEntity, partialTick) -> new OrientedRotatingVisual<>(ctx, blockEntity, partialTick, Direction.SOUTH, Direction.DOWN, Models.partial(AllPartialModels.SHAFT_HALF))).skipVanillaRender(be -> true).apply();
        SimpleBlockEntityVisualizer.builder(CCBBlockEntities.PORTABLE_GAS_INTERFACE.get()).factory(PortableGasInterfaceVisual::new).skipVanillaRender(be -> true).apply();
        SimpleBlockEntityVisualizer.builder(CCBBlockEntities.TESLA_TURBINE.get()).factory(TeslaTurbineVisual::new).skipVanillaRender(be -> true).apply();
    }
}
