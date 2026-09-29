package net.ty.createcraftedbeginning.content.airtights.balloon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import dev.engine_room.flywheel.lib.transform.Affine;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class BalloonRenderHelper {
    private BalloonRenderHelper() {
    }

    public static float getLinearScale(ItemStack stack, @Nullable Level level, BlockPos pos) {
        if (level == null || !BalloonItem.isBalloon(stack)) {
            return 1;
        }

        return BalloonWorldPhysics.of(stack, level, pos).linearScale();
    }

    public static float getBeltLinearScale(ItemStack stack, BeltBlockEntity belt, TransportedItemStack transported, float partialTicks) {
        Direction facing = belt.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        BeltSlope slope = belt.getBlockState().getValue(BeltBlock.SLOPE);
        float offset = belt.getSpeed() == 0 ? transported.beltPosition : Mth.lerp(partialTicks, transported.prevBeltPosition, transported.beltPosition);
        int verticality = switch (slope) {
            case DOWNWARD -> -1;
            case UPWARD -> 1;
            default -> 0;
        };
        int segment = Mth.floor(offset);
        return getLinearScale(stack, belt.getLevel(), belt.getBlockPos().offset(facing.getStepX() * segment, verticality * segment, facing.getStepZ() * segment));
    }

    public static <T extends Affine<T>> void applyChainConveyorAnchoredScale(T transform, ItemStack balloon, float scale) {
        if (scale == 1) {
            return;
        }

        float anchorY = BalloonItem.getHookDistance(balloon) - 1.4375F;
        transform.translate(0, anchorY, 0).scale(scale).translate(0, -anchorY, 0);
    }

    public static void applyBottomAnchoredFixedItemScale(PoseStack poseStack, float scale) {
        if (scale == 1) {
            return;
        }

        poseStack.translate(0, 0.25F * scale - 0.25F, 0);
        poseStack.scale(scale, scale, scale);
    }
}
