package net.ty.createcraftedbeginning.content.airtights.airtightcannon;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID, value = Dist.CLIENT)
public final class AirtightCannonClientEvents {
    private AirtightCannonClientEvents() {
    }

    @SubscribeEvent
    public static void onComputeFovModifier(ComputeFovModifierEvent event) {
        Player player = event.getPlayer();
        if (!player.isUsingItem()) {
            return;
        }

        ItemStack cannon = player.getUseItem();
        if (!cannon.is(CCBItems.AIRTIGHT_CANNON)) {
            return;
        }

        int useTime = cannon.getUseDuration(player) - player.getUseItemRemainingTicks();
        float chargeProgress = Math.min((float) useTime / (AirtightCannonUtils.getEfficientUseTime(cannon) * 2), 1);
        event.setNewFovModifier(event.getFovModifier() * (1 - chargeProgress * 0.15f));
    }
}
