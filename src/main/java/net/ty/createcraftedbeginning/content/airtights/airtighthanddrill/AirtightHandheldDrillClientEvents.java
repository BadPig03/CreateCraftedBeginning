package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID, value = Dist.CLIENT)
final class AirtightHandheldDrillClientEvents {
    private AirtightHandheldDrillClientEvents() {
    }

    @SubscribeEvent
    private static void onPlayerPreTick(Pre event) {
        Player player = event.getEntity();
        AirtightHandheldDrillRenderHandler renderHandler = AirtightHandheldDrillRenderHandler.INSTANCE;
        ItemStack drill = player.getMainHandItem();
        if (!drill.is(CCBItems.AIRTIGHT_HANDHELD_DRILL)) {
            if (renderHandler.hasHandAnimation()) {
                renderHandler.stop();
            }
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        boolean hasAnimation = renderHandler.hasHandAnimation();
        boolean isUsingDrill = player.isUsingItem() && player.getUseItem().is(CCBItems.AIRTIGHT_HANDHELD_DRILL);
        boolean isMiningBlock = minecraft.options.keyAttack.isDown() && minecraft.hitResult != null && minecraft.hitResult.getType() == Type.BLOCK;
        boolean shouldRotate = isUsingDrill || isMiningBlock;
        if (shouldRotate && !hasAnimation) {
            renderHandler.start();
            return;
        }

        if (shouldRotate || !hasAnimation) {
            return;
        }

        renderHandler.stop();
    }
}
