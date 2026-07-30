package net.ty.createcraftedbeginning.content.airtights.airtightcannon;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID, value = Dist.CLIENT)
public final class AirtightCannonClientEvents {
    private AirtightCannonClientEvents() {
    }

    @SubscribeEvent
    public static void onComputeFovModifier(ComputeFovModifierEvent event) {
        Player player = event.getPlayer();
        ItemStack usingItem = player.getUseItem();
        if (!usingItem.is(CCBItems.AIRTIGHT_CANNON) || !player.isUsingItem()) {
            return;
        }

        int useTime = usingItem.getUseDuration(player) - player.getUseItemRemainingTicks();
        int efficientUseTime = AirtightCannonUtils.getEfficientUseTime(usingItem);
        float chargeProgress = Math.min((float) useTime / (efficientUseTime * 2), 1);
        float fovModifier = 1 - chargeProgress * 0.15f;
        event.setNewFovModifier(event.getFovModifier() * fovModifier);
    }
}
