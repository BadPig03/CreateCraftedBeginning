package net.ty.createcraftedbeginning;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.ty.createcraftedbeginning.client.CCBClientBridgeImpl;
import net.ty.createcraftedbeginning.client.CCBClientRegistrations;
import net.ty.createcraftedbeginning.client.CCBFluidClientExtensions;
import net.ty.createcraftedbeginning.client.CCBParticleProviders;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.AirtightCannonRenderHandler;
import net.ty.createcraftedbeginning.content.airtights.airtightextendarm.AirtightExtendArmRenderHandler;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.AirtightHandheldDrillRenderHandler;
import net.ty.createcraftedbeginning.content.breezes.breezechamber.client.BreezeChamberClientAnimation;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.client.BreezeCoolerClientAnimation;
import net.ty.createcraftedbeginning.content.end.endincinerationblower.EndIncinerationBlowerClient;
import net.ty.createcraftedbeginning.content.end.endsculksilencer.EndSculkSilencerClient;
import net.ty.createcraftedbeginning.platform.CCBClientBridge;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mod(value = CreateCraftedBeginning.MOD_ID, dist = Dist.CLIENT)
public class CreateCraftedBeginningClient {
    public static final AirtightCannonRenderHandler AIRTIGHT_CANNON_RENDER_HANDLER = new AirtightCannonRenderHandler();
    public static final AirtightExtendArmRenderHandler AIRTIGHT_EXTEND_ARM_RENDER_HANDLER = new AirtightExtendArmRenderHandler();
    public static final AirtightHandheldDrillRenderHandler AIRTIGHT_HAND_DRILL_RENDER_HANDLER = new AirtightHandheldDrillRenderHandler();

    public CreateCraftedBeginningClient(IEventBus modEventBus) {
        CCBClientBridge.install(new CCBClientBridgeImpl());
        CCBClientRegistrations.initialize();

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

        AIRTIGHT_CANNON_RENDER_HANDLER.registerListeners(eventBus);
        AIRTIGHT_EXTEND_ARM_RENDER_HANDLER.registerListeners(eventBus);
        AIRTIGHT_HAND_DRILL_RENDER_HANDLER.registerListeners(eventBus);
    }
}