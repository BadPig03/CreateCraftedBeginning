package net.ty.createcraftedbeginning;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.client.CCBClientContextBridgeImpl;
import net.ty.createcraftedbeginning.client.CCBClientRegistrations;
import net.ty.createcraftedbeginning.client.CCBClientRenderBridgeImpl;
import net.ty.createcraftedbeginning.client.CCBClientScreenBridgeImpl;
import net.ty.createcraftedbeginning.client.CCBFluidClientExtensions;
import net.ty.createcraftedbeginning.client.CCBParticleProviders;
import net.ty.createcraftedbeginning.compat.CCBCompatBootstrap;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.AirtightCannonRenderHandler;
import net.ty.createcraftedbeginning.content.airtights.airtightextendarm.AirtightExtendArmRenderHandler;
import net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill.AirtightHandheldDrillRenderHandler;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.client.BreezeChamberClientAnimation;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.client.BreezeCoolerClientAnimation;
import net.ty.createcraftedbeginning.content.end.endincinerationblower.EndIncinerationBlowerClient;
import net.ty.createcraftedbeginning.content.end.endsculksilencer.EndSculkSilencerClient;
import net.ty.createcraftedbeginning.platform.client.ClientContextBridge;
import net.ty.createcraftedbeginning.platform.client.ClientRenderBridge;
import net.ty.createcraftedbeginning.platform.client.ClientScreenBridge;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mod(value = CCBAPI.MOD_ID, dist = Dist.CLIENT)
public class CreateCraftedBeginningClient {

    public CreateCraftedBeginningClient(IEventBus modEventBus) {
        ClientContextBridge.install(new CCBClientContextBridgeImpl());
        ClientRenderBridge.install(new CCBClientRenderBridgeImpl());
        ClientScreenBridge.install(new CCBClientScreenBridgeImpl());

        CCBClientRegistrations.initialize();
        CCBCompatBootstrap.registerClientListeners(modEventBus);

        IEventBus eventBus = NeoForge.EVENT_BUS;
        modEventBus.addListener(CCBFluidClientExtensions::register);
        modEventBus.addListener(CCBParticleProviders::register);
        modEventBus.addListener(CCBClientRegistrations::registerBlockExtensions);
        modEventBus.addListener(CCBClientRegistrations::registerMenuScreens);
        modEventBus.addListener(CCBClientRegistrations::registerRenderers);
        modEventBus.addListener(CCBClientRegistrations::registerVisualizers);

        BreezeChamberClientAnimation.initialize();
        BreezeCoolerClientAnimation.initialize();
        EndIncinerationBlowerClient.initialize();
        EndSculkSilencerClient.initialize();

        AirtightCannonRenderHandler.INSTANCE.registerListeners(eventBus);
        AirtightExtendArmRenderHandler.INSTANCE.registerListeners(eventBus);
        AirtightHandheldDrillRenderHandler.INSTANCE.registerListeners(eventBus);
    }
}