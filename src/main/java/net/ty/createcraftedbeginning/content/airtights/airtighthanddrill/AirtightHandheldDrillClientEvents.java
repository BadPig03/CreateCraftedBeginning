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
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.CreateCraftedBeginningClient;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID, value = Dist.CLIENT)
public final class AirtightHandheldDrillClientEvents {
    private AirtightHandheldDrillClientEvents() {
    }

    @SubscribeEvent
    public static void onPlayerPreTick(Pre event) {
        Player player = event.getEntity();
        AirtightHandheldDrillRenderHandler renderHandler = CreateCraftedBeginningClient.AIRTIGHT_HAND_DRILL_RENDER_HANDLER;
        ItemStack drill = player.getMainHandItem();
        if (!drill.is(CCBItems.AIRTIGHT_HANDHELD_DRILL)) {
            if (renderHandler.hasHandAnimation(0)) {
                renderHandler.stop();
            }
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        boolean hasAnimation = renderHandler.hasHandAnimation(0);
        boolean isUsingDrill = player.isUsingItem() && player.getUseItem().is(CCBItems.AIRTIGHT_HANDHELD_DRILL);
        boolean isMiningBlock = minecraft.options.keyAttack.isDown() && minecraft.hitResult != null && minecraft.hitResult.getType() == Type.BLOCK;
        boolean shouldRotate = isUsingDrill || isMiningBlock;
        if (shouldRotate && !hasAnimation) {
            renderHandler.start();
        }
        else if (!shouldRotate && hasAnimation) {
            renderHandler.stop();
        }
    }
}
