package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.CreateCraftedBeginningClient;
import net.ty.createcraftedbeginning.api.gascanisters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterUtils;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID)
public class AirtightHandheldDrillEvents {
    @SubscribeEvent
    public static void onPlayerPreTick(Pre event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (!level.isClientSide) {
            return;
        }

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
        boolean shouldRotate = player.isUsingItem() && player.getUseItem().is(CCBItems.AIRTIGHT_HANDHELD_DRILL) || minecraft.options.keyAttack.isDown() && minecraft.hitResult != null && minecraft.hitResult.getType() == Type.BLOCK;
        if (shouldRotate && !hasAnimation) {
            renderHandler.start();
        }
        else if (!shouldRotate && hasAnimation) {
            renderHandler.stop();
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(LeftClickBlock event) {
        Player player = event.getEntity();
        ItemStack drill = player.getMainHandItem();
        if (!drill.is(CCBItems.AIRTIGHT_HANDHELD_DRILL)) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.START) {
            return;
        }

        BlockPos pos = AirtightHandheldDrillUtils.getHitResult(player);
        if (pos == null) {
            return;
        }

        float newSpeed = AirtightHandheldDrillUtils.calculateFinalBreakSpeed(1, player, drill, pos);
        if (newSpeed >= 0) {
            return;
        }

        if (newSpeed == -1) {
            GasCanisterUtils.displayCustomWarningHint(player, "gui.warnings.insufficient_gas", CanisterContainerSuppliers.getFirstAvailableGasContent(player).getHoverName());
        }
        else if (newSpeed == -2) {
            GasCanisterUtils.displayCustomWarningHint(player, "gui.warnings.invalid_mining_target");
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(BreakSpeed event) {
        Player player = event.getEntity();
        ItemStack drill = player.getMainHandItem();
        if (!drill.is(CCBItems.AIRTIGHT_HANDHELD_DRILL)) {
            return;
        }

        BlockPos pos = event.getPosition().orElse(null);
        if (pos == null) {
            return;
        }

        float oldSpeed = event.getNewSpeed();
        float newSpeed = Math.max(0, AirtightHandheldDrillUtils.calculateFinalBreakSpeed(oldSpeed, player, drill, pos));
        if (oldSpeed == newSpeed) {
            return;
        }

        event.setNewSpeed(newSpeed);
    }
}
