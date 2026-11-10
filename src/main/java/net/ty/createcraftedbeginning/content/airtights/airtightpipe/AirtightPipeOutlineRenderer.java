package net.ty.createcraftedbeginning.content.airtights.airtightpipe;

import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.Tags.Items;
import net.ty.createcraftedbeginning.config.CCBConfig;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class AirtightPipeOutlineRenderer {
    private static final int COLOR_OPEN = 0x9EDE73;
    private static final int COLOR_CLOSED = 0xFF7171;

    private AirtightPipeOutlineRenderer() {
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null || player.isSpectator()) {
            return;
        }

        if (!CCBConfig.client().outlines.showCasedAirtightPipeSides.get() || !player.getMainHandItem().is(Items.TOOLS_WRENCH) && !player.getOffhandItem().is(Items.TOOLS_WRENCH) || !GogglesItem.isWearingGoggles(player)) {
            return;
        }

        if (!(minecraft.hitResult instanceof BlockHitResult hitResult)) {
            return;
        }

        BlockPos pipePos = hitResult.getBlockPos();
        BlockState pipeState = level.getBlockState(pipePos);
        if (!(pipeState.getBlock() instanceof AirtightPipeBlock && pipeState.getValue(AirtightPipeBlock.CASED))) {
            return;
        }

        showCasedPipePorts(pipePos, pipeState);
    }

    private static void showCasedPipePorts(BlockPos pos, BlockState state) {
        for (Direction direction : Iterate.directions) {
            Vec3 center = Vec3.atCenterOf(pos).relative(direction, 0.515625);
            Vec3 halfSize = new Vec3(direction.getStepX() == 0 ? 0.4375 : 0.0078125, direction.getStepY() == 0 ? 0.4375 : 0.0078125, direction.getStepZ() == 0 ? 0.4375 : 0.0078125);
            AABB face = new AABB(center.subtract(halfSize), center.add(halfSize));
            int color = state.getValue(AirtightPipeBlock.PROPERTY_BY_DIRECTION.get(direction)) ? COLOR_OPEN : COLOR_CLOSED;
            Outliner.getInstance().showAABB("airtightPipePort_" + direction.getName(), face).colored(color).withFaceTexture(AllSpecialTextures.CHECKERED).lineWidth(0.03125f);
        }
    }
}
