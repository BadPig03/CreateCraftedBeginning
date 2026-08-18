package net.ty.createcraftedbeginning.content.airtights.airtightencasedpipe;

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
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.Tags.Items;
import net.ty.createcraftedbeginning.config.CCBConfig;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class AirtightEncasedPipeOutlineRenderer {
    private static final int COLOR_OPEN = 0xFF4EB483;
    private static final int COLOR_CLOSED = 0xFFFF5D6C;

    private AirtightEncasedPipeOutlineRenderer() {
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null || player.isSpectator()) {
            return;
        }

        if (!CCBConfig.client().enableAirtightEncasedPipeOutline.get() || !isHoldingWrench(player) || !GogglesItem.isWearingGoggles(player)) {
            return;
        }

        if (!(minecraft.hitResult instanceof BlockHitResult hitResult)) {
            return;
        }

        BlockPos pos = hitResult.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof AirtightEncasedPipeBlock)) {
            return;
        }

        List<BlockPos> openList = new ArrayList<>();
        List<BlockPos> closedList = new ArrayList<>();
        for (Direction direction : Iterate.directions) {
            BlockPos relativePos = pos.relative(direction);
            if (AirtightEncasedPipeBlock.isOpenAt(state, direction)) {
                openList.add(relativePos);
                continue;
            }

            closedList.add(relativePos);
        }
        Outliner outliner = Outliner.getInstance();
        outliner.showCluster("airtightEncasedPipeOpened", openList).colored(COLOR_OPEN).withFaceTexture(AllSpecialTextures.CHECKERED).lineWidth(0.0234375f);
        outliner.showCluster("airtightEncasedPipeClosed", closedList).colored(COLOR_CLOSED).withFaceTexture(AllSpecialTextures.HIGHLIGHT_CHECKERED).lineWidth(0.0390625f);
    }

    private static boolean isHoldingWrench(LocalPlayer player) {
        return player.getMainHandItem().is(Items.TOOLS_WRENCH) || player.getOffhandItem().is(Items.TOOLS_WRENCH);
    }
}
