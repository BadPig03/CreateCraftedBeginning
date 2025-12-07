package net.ty.createcraftedbeginning;

import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.ftb.FTBIntegration;
import com.simibubi.create.compat.pojav.PojavChecker;
import com.simibubi.create.compat.sodium.SodiumCompat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.AirtightCannonRenderHandler;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.AirtightHandheldDrillRenderHandler;
import net.ty.createcraftedbeginning.content.obsolete.cinderincinerationblower.CinderIncinerationBlowerOutliner;
import net.ty.createcraftedbeginning.registry.CCBParticleTypes;
import org.jetbrains.annotations.NotNull;

@Mod(value = CreateCraftedBeginning.MOD_ID, dist = Dist.CLIENT)
public class CreateCraftedBeginningClient {
    public static final CinderIncinerationBlowerOutliner CINDER_INCINERATION_BLOWER_OUTLINER = new CinderIncinerationBlowerOutliner();
    public static final AirtightCannonRenderHandler AIRTIGHT_CANNON_RENDER_HANDLER = new AirtightCannonRenderHandler();
    public static final AirtightHandheldDrillRenderHandler AIRTIGHT_HAND_DRILL_RENDER_HANDLER = new AirtightHandheldDrillRenderHandler();

    public CreateCraftedBeginningClient(@NotNull IEventBus modEventBus) {
        IEventBus eventBus = NeoForge.EVENT_BUS;
        modEventBus.addListener(CCBParticleTypes::registerFactories);

        AIRTIGHT_CANNON_RENDER_HANDLER.registerListeners(eventBus);
        AIRTIGHT_HAND_DRILL_RENDER_HANDLER.registerListeners(eventBus);

        Mods.FTBLIBRARY.executeIfInstalled(() -> () -> FTBIntegration.init(modEventBus, eventBus));
        Mods.SODIUM.executeIfInstalled(() -> () -> SodiumCompat.init(modEventBus, eventBus));
        PojavChecker.init();
    }
}