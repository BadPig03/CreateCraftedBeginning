package net.ty.createcraftedbeginning;

import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.ty.createcraftedbeginning.advancement.CCBAdvancements;
import net.ty.createcraftedbeginning.advancement.CCBTriggers;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.data.CCBDataGen;
import net.ty.createcraftedbeginning.data.CCBRegistrate;
import net.ty.createcraftedbeginning.data.CCBTags;
import net.ty.createcraftedbeginning.registry.CCBArmInteractionPointTypes;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBCreativeTabs;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;
import net.ty.createcraftedbeginning.registry.CCBFluids;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBMountedStorage;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
import org.slf4j.Logger;

@Mod(CreateCraftedBeginning.MOD_ID)
public class CreateCraftedBeginning {
    public static final String MOD_ID = "createcraftedbeginning";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final CCBRegistrate CCB_REGISTRATE = CCBRegistrate.create(MOD_ID).defaultCreativeTab(CCBCreativeTabs.CREATIVE_TAB.getKey()).setTooltipModifierFactory(item -> new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE).andThen(TooltipModifier.mapNull(KineticStats.create(item))));

    public CreateCraftedBeginning(IEventBus modEventBus, ModContainer modContainer) {
        CCB_REGISTRATE.registerEventListeners(modEventBus);

        CCBTags.register();
        CCBCreativeTabs.register(modEventBus);
        CCBBlocks.register();
        CCBItems.register();
        CCBFluids.register(modEventBus);
        CCBBlockEntities.register();
        CCBDataComponents.register(modEventBus);
        CCBMountedStorage.register();
        CCBPartialModels.register();
        CCBRecipeTypes.register(modEventBus);
        CCBArmInteractionPointTypes.register(modEventBus);

        CCBConfig.register(modContainer);

        NeoForge.EVENT_BUS.register(this);
        modEventBus.addListener(CreateCraftedBeginning::onRegister);
        modEventBus.addListener(EventPriority.HIGHEST, CCBDataGen::gatherDataHighPriority);
        modEventBus.addListener(EventPriority.LOWEST, CCBDataGen::gatherData);
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static CCBRegistrate registrate() {
        return CCB_REGISTRATE;
    }

    public static void onRegister(final RegisterEvent event) {
        if (event.getRegistry() == BuiltInRegistries.TRIGGER_TYPES) {
            CCBAdvancements.register();
            CCBTriggers.register();
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }
}
