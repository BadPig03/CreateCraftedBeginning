package net.ty.createcraftedbeginning.content.airtights.airtighthanddrill;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.upgrades.ExperienceConversionUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.upgrades.MagnetUpgrade;
import net.ty.createcraftedbeginning.content.airtights.airtighthanddrill.upgrades.SilkTouchUpgrade;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterUtils;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID)
final class AirtightHandheldDrillEvents {
    private AirtightHandheldDrillEvents() {
    }

    @SubscribeEvent
    private static void onLeftClickBlock(LeftClickBlock event) {
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
            return;
        }

        if (newSpeed != -2) {
            return;
        }

        GasCanisterUtils.displayCustomWarningHint(player, "gui.warnings.invalid_mining_target");
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    private static void onSilkTouchDrillBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof Player player)) {
            return;
        }

        ItemStack drill = event.getTool();
        if (!drill.is(CCBItems.AIRTIGHT_HANDHELD_DRILL) || !SilkTouchUpgrade.INSTANCE.canApply(drill) || ExperienceConversionUpgrade.INSTANCE.canApply(drill)) {
            return;
        }

        ServerLevel level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        ItemStack usedTool = AirtightHandheldDrillUtils.createDrillUsedTool(drill, level);
        event.getDrops().clear();
        for (ItemStack stack : Block.getDrops(state, level, pos, event.getBlockEntity(), player, usedTool)) {
            if (stack.isEmpty()) {
                continue;
            }

            event.getDrops().add(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack));
        }

        int experience = EnchantmentHelper.processBlockExperience(level, usedTool, state.getExpDrop(level, pos, event.getBlockEntity(), player, usedTool));
        event.setDroppedExperience(experience);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    private static void onDrillBlockDropUpgrades(BlockDropsEvent event) {
        ItemStack drill = event.getTool();
        if (!drill.is(CCBItems.AIRTIGHT_HANDHELD_DRILL)) {
            return;
        }

        if (ExperienceConversionUpgrade.INSTANCE.canApply(drill)) {
            BlockState state = event.getState();
            Block block = state.getBlock();
            if (new ItemStack(block.asItem()).canFitInsideContainerItems()) {
                event.getDrops().clear();
                event.setDroppedExperience(Mth.ceil(state.getDestroySpeed(event.getLevel(), event.getPos()) / 10));
            }
        }

        if (!MagnetUpgrade.INSTANCE.canApply(drill) || !(event.getBreaker() instanceof Player player)) {
            return;
        }

        for (ItemEntity dropEntity : event.getDrops()) {
            ItemStack drop = dropEntity.getItem();
            if (drop.isEmpty()) {
                continue;
            }

            ItemHandlerHelper.giveItemToPlayer(player, drop);
        }
        event.getDrops().clear();

        int experience = event.getDroppedExperience();
        if (experience <= 0) {
            return;
        }

        player.giveExperiencePoints(experience);
        event.setDroppedExperience(0);
    }

    @SubscribeEvent
    private static void onBreakSpeed(BreakSpeed event) {
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
