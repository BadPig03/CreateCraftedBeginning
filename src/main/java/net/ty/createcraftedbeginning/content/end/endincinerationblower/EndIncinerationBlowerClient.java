package net.ty.createcraftedbeginning.content.end.endincinerationblower;

import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.ponder.api.PonderPalette;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.config.CCBConfig;

import javax.annotation.ParametersAreNonnullByDefault;

@OnlyIn(Dist.CLIENT)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class EndIncinerationBlowerClient {
    private static final int OUTLINE_DISTANCE_SQR = 4096;

    private EndIncinerationBlowerClient() {
    }

    public static void initialize() {
        EndIncinerationBlowerBlockEntity.setClientTicker(EndIncinerationBlowerClient::tick);
    }

    private static void tick(EndIncinerationBlowerBlockEntity blower) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !blower.isShowingOutline() || !GogglesItem.isWearingGoggles(player) || !CCBConfig.client().enableEndIncinerationBlowerOutline.get()) {
            return;
        }

        if (player.distanceToSqr(blower.getBlockPos().getX() + 0.5, blower.getBlockPos().getY() + 0.5, blower.getBlockPos().getZ() + 0.5) > OUTLINE_DISTANCE_SQR) {
            return;
        }

        float speed = blower.getSpeed();
        if (EndIncinerationBlowerBlockEntity.calculateRange(speed) <= 0) {
            return;
        }

        Outliner.getInstance().chaseAABB(blower, EndIncinerationBlowerBlockEntity.calculateArea(blower.getBlockPos(), speed)).colored(PonderPalette.INPUT.getColor()).withFaceTextures(AllSpecialTextures.CHECKERED, AllSpecialTextures.HIGHLIGHT_CHECKERED).lineWidth(0.0625F);
    }
}
