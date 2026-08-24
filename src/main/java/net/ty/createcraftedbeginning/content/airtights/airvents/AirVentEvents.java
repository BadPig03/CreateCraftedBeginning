package net.ty.createcraftedbeginning.content.airtights.airvents;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;
import net.ty.createcraftedbeginning.api.CCBAPI;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID)
final class AirVentEvents {
    private AirVentEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    private static void onRightClickBlock(RightClickBlock event) {
        if (!(event.getItemStack().getItem() instanceof BlockItem) || !AirVentBlock.isInsideAirVent(event.getEntity())) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    private static void onBlockPlaced(EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player player) || !AirVentBlock.isInsideAirVent(player)) {
            return;
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    private static void onPlayerTick(Post event) {
        Player player = event.getEntity();
        if (player.isSpectator()) {
            return;
        }

        if (player.getInBlockState().getBlock() instanceof AirVentBlock) {
            player.setPose(Pose.SWIMMING);
            return;
        }

        if (!player.isShiftKeyDown()) {
            return;
        }

        Vec3 lookAngle = player.getLookAngle();
        Direction lookDirection = Direction.getNearest(lookAngle.x, lookAngle.y, lookAngle.z);
        Level level = player.level();
        BlockPos playerPos = player.blockPosition();
        if (canEnterFrom(level, playerPos.relative(lookDirection), lookDirection)) {
            player.setPose(Pose.SWIMMING);
            return;
        }

        if (lookDirection != Direction.UP || !canEnterFrom(level, playerPos.above(2), lookDirection)) {
            return;
        }

        player.setPose(Pose.SWIMMING);
    }

    private static boolean canEnterFrom(Level level, BlockPos ventPos, Direction entryDirection) {
        BlockState ventState = level.getBlockState(ventPos);
        return ventState.getBlock() instanceof AirVentBlock && AirVentBlock.canPassThrough(ventState, level, ventPos, entryDirection.getOpposite());
    }
}
