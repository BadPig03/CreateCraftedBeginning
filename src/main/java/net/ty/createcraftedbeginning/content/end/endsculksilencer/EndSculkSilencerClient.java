package net.ty.createcraftedbeginning.content.end.endsculksilencer;

import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.ponder.api.PonderPalette;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.Projection;

import javax.annotation.ParametersAreNonnullByDefault;

@OnlyIn(Dist.CLIENT)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class EndSculkSilencerClient {
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

        short activeRange = silencer.getActiveWorkingRange();
        Level silencerLevel = silencer.getLevel();
        if (activeRange <= 0 || silencerLevel == null) {
            return;
        }

        Projection projection = SubLevelBridge.resolve(silencerLevel, silencer.getBlockPos());
        Vec3 projectedCenter = projection.worldPosition();
        if (player.distanceToSqr(projectedCenter.x, projectedCenter.y, projectedCenter.z) > 9216) {
            return;
        }

        Outliner.getInstance().chaseAABB(silencer, EndSculkSilencerBlockEntity.calculateArea(player.level(), projection.blockPos(), activeRange)).colored(PonderPalette.INPUT.getColor()).withFaceTexture(AllSpecialTextures.CHECKERED).lineWidth(0.0625f);
    }
}
