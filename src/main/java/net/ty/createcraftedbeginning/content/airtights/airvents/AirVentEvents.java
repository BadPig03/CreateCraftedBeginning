package net.ty.createcraftedbeginning.content.airtights.airvents;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent.Post;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CreateCraftedBeginning.MOD_ID)
public class AirVentEvents {
    @SubscribeEvent
    public static void onPlayerTick(Post event) {
        Player player = event.getEntity();
        if (player.isSpectator() || !player.isShiftKeyDown()) {
            return;
        }

        if (player.getInBlockState().getBlock() instanceof AirVentBlock) {
            player.setPose(Pose.SWIMMING);
            return;
        }

        Level level = player.level();
        Direction direction = player.getDirection();
        BlockPos blockPos = player.blockPosition();
        BlockState blockState = level.getBlockState(blockPos.relative(direction));
        if (!(blockState.getBlock() instanceof AirVentBlock) || !blockState.getValue(AirVentBlock.PROPERTY_BY_DIRECTION.get(direction.getOpposite())).canPassThrough() ) {
            return;
        }

        player.setPose(Pose.SWIMMING);
    }
}
