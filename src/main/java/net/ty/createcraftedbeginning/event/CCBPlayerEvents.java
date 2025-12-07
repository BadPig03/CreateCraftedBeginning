package net.ty.createcraftedbeginning.event;

import com.simibubi.create.AllItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.CreateCraftedBeginningClient;
import net.ty.createcraftedbeginning.content.obsolete.cinderincinerationblower.CinderIncinerationBlowerBlockEntity;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID)
public class CCBPlayerEvents {
    @SubscribeEvent
    public static void onBlockRightClick(@NotNull RightClickBlock event) {
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

        CreateCraftedBeginningClient.CINDER_INCINERATION_BLOWER_OUTLINER.toggleOutline(pos);
        event.setCanceled(true);
    }
}
