package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerClients;
import net.ty.createcraftedbeginning.api.gas.cansiters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID)
public class AirtightHandheldDrillEvents {
    @SubscribeEvent
    public static void onAirtightHandheldDrillAnimationPreUpdate(@NotNull Pre event) {
        AirtightHandheldDrillUtils.tryUpdateAnimation(event.getEntity());
    }

    @SubscribeEvent
    public static void onAirtightHandheldDrillLeftClickBlockStart(@NotNull LeftClickBlock event) {
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

        GasStack gasContent = CanisterContainerSuppliers.getFirstAvailableGasContent(player);
        float newSpeed = AirtightHandheldDrillUtils.calculateFinalBreakSpeed(1, player, drill, pos);
        if (newSpeed >= 0) {
            return;
        }

        if (newSpeed == -1) {
            CanisterContainerClients.displayCustomWarningHint(player, "gui.warnings.insufficient_gas", gasContent.getHoverName());
        }
        else if (newSpeed == -2) {
            CanisterContainerClients.displayCustomWarningHint(player, "gui.warnings.invalid_mining_target");
        }
    }

    @SubscribeEvent
    public static void onAirtightHandheldDrillGetBreakSpeed(@NotNull BreakSpeed event) {
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
