package net.ty.createcraftedbeginning.client;

import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.client.gui.TooltipBarAlignment;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.AirtightCannonRenderHandler;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.gas.visual.GasAreaOutlinePacket;
import net.ty.createcraftedbeginning.platform.client.ClientRenderBridge.Service;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class ClientRenderService implements Service {
    private static final int COLOR_RED = 0xFFFF5D6C;

    @Override
    public boolean addAlignedTooltipBars(List<Component> tooltip, int indent, List<? extends Component> labels, List<? extends Component> bars) {
        TooltipBarAlignment.addAlignedBars(tooltip, indent, labels, bars);
        return true;
    }

    @Override
    public void dontAnimateAirtightCannon(InteractionHand hand) {
        AirtightCannonRenderHandler.INSTANCE.dontAnimateItem(hand);
    }

    @Override
    public void showPlacementBounds(Player player, String outlineId, BlockPos placementPos, AABB bounds, String warningKey) {
        if (!(player instanceof LocalPlayer localPlayer)) {
            return;
        }

        Outliner.getInstance().showAABB(Pair.of(outlineId, placementPos), bounds).colored(COLOR_RED);
        CCBLang.translate(warningKey).color(COLOR_RED).sendStatus(localPlayer);
    }

    @Override
    public void showGasAreaOutline(Player player, BlockPos effectPos, float inflation, int color) {
        if (!GogglesItem.isWearingGoggles(player) || !CCBConfig.client().outlines.showGasReleaseAreas.get()) {
            return;
        }

        AABB outlineBounds = new AABB(effectPos).inflate(inflation);
        Object outlineSlot = Pair.of(GasAreaOutlinePacket.class, effectPos);
        Outliner.getInstance().chaseAABB(outlineSlot, outlineBounds).colored(color).withFaceTextures(AllSpecialTextures.CHECKERED, AllSpecialTextures.HIGHLIGHT_CHECKERED).lineWidth(0.0625F);
    }
}
