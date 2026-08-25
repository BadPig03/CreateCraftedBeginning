package net.ty.createcraftedbeginning.client;

import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.AirtightCannonRenderHandler;
import net.ty.createcraftedbeginning.content.airtights.gas.GasAreaOutlinePacket;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.platform.client.ClientRenderBridge.Service;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class CCBClientRenderBridgeImpl implements Service {
    private static final int COLOR_RED = 0xFFFF5D6C;

    @Override
    public boolean addAlignedTooltipBars(List<Component> tooltip, int indent, List<? extends Component> labels, List<? extends Component> bars) {
        CCBTooltipBarAlignment.addAlignedBars(tooltip, indent, labels, bars);
        return true;
    }

    @Override
    public void dontAnimateAirtightCannon(InteractionHand hand) {
        AirtightCannonRenderHandler.INSTANCE.dontAnimateItem(hand);
    }

    @Override
    public void showPlacementBounds(BlockPlaceContext context, String outlineId, BlockPos placementPos, AABB bounds) {
        if (!(context.getPlayer() instanceof LocalPlayer localPlayer)) {
            return;
        }

        Outliner.getInstance().showAABB(Pair.of(outlineId, placementPos), bounds).colored(COLOR_RED);
        CCBLang.translate("gui.warnings.clear_blocks_for_placement").color(COLOR_RED).sendStatus(localPlayer);
    }

    @Override
    public void showGasAreaOutline(Player player, BlockPos pos, Direction direction, float inflation, int color) {
        if (!GogglesItem.isWearingGoggles(player) || !CCBConfig.client().enableGasAreaOutline.get()) {
            return;
        }

        AABB outlineBounds = new AABB(pos.relative(direction)).inflate(inflation);
        Object outlineSlot = Pair.of(GasAreaOutlinePacket.class, Pair.of(pos, direction));
        Outliner.getInstance().chaseAABB(outlineSlot, outlineBounds).colored(color).withFaceTextures(AllSpecialTextures.CHECKERED, AllSpecialTextures.HIGHLIGHT_CHECKERED).lineWidth(0.0625f);
    }
}
