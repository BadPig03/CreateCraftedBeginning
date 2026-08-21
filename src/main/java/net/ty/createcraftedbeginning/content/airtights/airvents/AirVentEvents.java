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
public class AirVentEvents {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRightClickBlock(RightClickBlock event) {
        if (!(event.getItemStack().getItem() instanceof BlockItem) || !AirVentBlock.isInsideAirVent(event.getEntity())) {
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockPlaced(EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player player) || !AirVentBlock.isInsideAirVent(player)) {
            return;
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerTick(Post event) {
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
        Direction direction = Direction.getNearest(lookAngle.x, lookAngle.y, lookAngle.z);
        Level level = player.level();
        BlockPos pos = player.blockPosition();
        if (canEnterFrom(level, pos.relative(direction), direction)) {
            player.setPose(Pose.SWIMMING);
            return;
        }

        if (direction != Direction.UP || !canEnterFrom(level, pos.above(2), direction)) {
            return;
        }

        player.setPose(Pose.SWIMMING);
    }

    private static boolean canEnterFrom(Level level, BlockPos pos, Direction direction) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof AirVentBlock && AirVentBlock.canPassThrough(state, level, pos, direction.getOpposite());
    }
}
