package net.ty.createcraftedbeginning;

import com.simibubi.create.compat.Mods;
import com.simibubi.create.compat.ftb.FTBIntegration;
import com.simibubi.create.compat.pojav.PojavChecker;
import com.simibubi.create.compat.sodium.SodiumCompat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.ty.createcraftedbeginning.content.airtightcannon.AirtightCannonRenderHandler;
import net.ty.createcraftedbeginning.content.cinderincinerationblower.CinderIncinerationBlowerOutliner;

@Mod(value = CreateCraftedBeginning.MOD_ID, dist = Dist.CLIENT)
public class CreateCraftedBeginningClient {
    public static final CinderIncinerationBlowerOutliner CINDER_INCINERATION_BLOWER_OUTLINER = new CinderIncinerationBlowerOutliner();
    public static final AirtightCannonRenderHandler AIRTIGHT_CANNON_RENDER_HANDLER = new AirtightCannonRenderHandler();

    public CreateCraftedBeginningClient(IEventBus modEventBus) {
		onCtorClient(modEventBus);
	}

    public static void onCtorClient(IEventBus modEventBus) {
		IEventBus neoEventBus = NeoForge.EVENT_BUS;

		AIRTIGHT_CANNON_RENDER_HANDLER.registerListeners(neoEventBus);

		Mods.FTBLIBRARY.executeIfInstalled(() -> () -> FTBIntegration.init(modEventBus, neoEventBus));
		Mods.SODIUM.executeIfInstalled(() -> () -> SodiumCompat.init(modEventBus, neoEventBus));
		PojavChecker.init();
	}
}