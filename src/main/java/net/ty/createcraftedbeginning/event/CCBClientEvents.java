package net.ty.createcraftedbeginning.event;

import net.createmod.ponder.foundation.PonderIndex;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.ponder.CCBPonderPlugin;

@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID, value = Dist.CLIENT)
public class CCBClientEvents {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> PonderIndex.addPlugin(new CCBPonderPlugin()));
    }
}
