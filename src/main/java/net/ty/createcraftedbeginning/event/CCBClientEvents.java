package net.ty.createcraftedbeginning.event;

import com.simibubi.create.AllItems;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.CreateCraftedBeginningClient;
import net.ty.createcraftedbeginning.content.cinderincinerationblower.CinderIncinerationBlowerBlockEntity;
import net.ty.createcraftedbeginning.ponder.CCBPonderPlugin;

@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID, value = Dist.CLIENT)
public class CCBClientEvents {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> PonderIndex.addPlugin(new CCBPonderPlugin()));
    }

    @SubscribeEvent
    public static void onTickPre(ClientTickEvent.Pre event) {
        onTick(true);
    }

    @SubscribeEvent
    public static void onTickPost(ClientTickEvent.Post event) {
        onTick(false);
    }

    public static void onTick(boolean isPreEvent) {
        if (Minecraft.getInstance().level == null || Minecraft.getInstance().player == null) {
            return;
        }

        CreateCraftedBeginningClient.CINDER_NOZZLE_HANDLER.tick();
    }

    @SubscribeEvent
    public static void onBlockRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        if (!AllItems.GOGGLES.isIn(event.getEntity().getItemBySlot(EquipmentSlot.HEAD))) {
            return;
        }

         if (!AllItems.WRENCH.isIn(event.getEntity().getMainHandItem()) && !AllItems.WRENCH.isIn(event.getEntity().getOffhandItem())) {
            return;
        }

        BlockPos pos = event.getPos();
        if (!(event.getLevel().getBlockEntity(pos) instanceof CinderIncinerationBlowerBlockEntity)) {
            return;
        }

        CreateCraftedBeginningClient.CINDER_NOZZLE_HANDLER.toggleOutline(pos);
        event.setCanceled(true);
    }
}
