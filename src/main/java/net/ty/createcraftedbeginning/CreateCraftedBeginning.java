package net.ty.createcraftedbeginning;

import com.simibubi.create.foundation.data.CreateRegistrate;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.registry.*;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(CreateCraftedBeginning.MOD_ID)
public class CreateCraftedBeginning
{
    public static final String MOD_ID = "createcraftedbeginning";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final CreateRegistrate CREATE_REGISTRATE = CreateRegistrate.create(MOD_ID).defaultCreativeTab(CCBCreativeTabs.CREATIVE_TAB.getKey());

    public CreateCraftedBeginning(IEventBus modEventBus, ModContainer modContainer) {
        CREATE_REGISTRATE.registerEventListeners(modEventBus);

        CCBItems.register();
        CCBBlocks.register();
        CCBBlockEntities.register();
        CCBMountedStorage.register();
        CCBCreativeTabs.register(modEventBus);
        CCBPartialModels.register();
        CCBDataComponents.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::commonSetup);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
    }

    public static CreateRegistrate registrate() {
        return CREATE_REGISTRATE;
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }
}
