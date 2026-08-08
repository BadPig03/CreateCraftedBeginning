package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterUtils;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID)
public class AirtightHandheldDrillEvents {

    @SubscribeEvent
    public static void onLeftClickBlock(LeftClickBlock event) {
        Player player = event.getEntity();
        ItemStack drill = player.getMainHandItem();
        if (!drill.is(CCBItems.AIRTIGHT_HANDHELD_DRILL)) {
            return;
        }

        if (event.getAction() != Action.START) {
            return;
        }

        BlockPos pos = event.getPos();
        float newSpeed = AirtightHandheldDrillUtils.calculateFinalBreakSpeed(1, player, drill, pos);
        if (newSpeed >= 0) {
            return;
        }

        if (newSpeed == -1) {
            GasStack gasContent = CanisterContainerSuppliers.getFirstAvailableGasContent(player);
            if (gasContent.isEmpty()) {
                GasCanisterUtils.displayCustomWarningHint(player, "gui.warnings.insufficient_gas");
            }
            else {
                GasCanisterUtils.displayCustomWarningHint(player, "gui.warnings.insufficient_gas", gasContent.getHoverName());
            }
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
