package net.ty.createcraftedbeginning.content.end.endsculksilencer;

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
public final class EndSculkSilencerClient {
    private static final double OUTLINE_DISTANCE_SQR = 9216;

    private EndSculkSilencerClient() {
    }

    public static void initialize() {
        EndSculkSilencerBlockEntity.setClientTicker(EndSculkSilencerClient::tick);
    }

    private static void tick(EndSculkSilencerBlockEntity silencer) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !silencer.isShowingOutline() || !GogglesItem.isWearingGoggles(player) || !CCBConfig.client().enableEndSculkSilencerOutline.get()) {
            return;
        }

        if (player.distanceToSqr(silencer.getBlockPos().getX() + 0.5, silencer.getBlockPos().getY() + 0.5, silencer.getBlockPos().getZ() + 0.5) > OUTLINE_DISTANCE_SQR) {
            return;
        }

        short range = silencer.getActiveWorkingRange();
        if (range <= 0 || silencer.getLevel() == null) {
            return;
        }

        Outliner.getInstance().chaseAABB(silencer, EndSculkSilencerBlockEntity.calculateArea(silencer.getLevel(), silencer.getBlockPos(), range)).colored(PonderPalette.INPUT.getColor()).lineWidth(0.0625F);
    }
}
