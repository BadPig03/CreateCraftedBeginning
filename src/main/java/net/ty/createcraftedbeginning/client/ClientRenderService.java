package net.ty.createcraftedbeginning.client;

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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseArea;
import net.ty.createcraftedbeginning.client.gui.TooltipBarAlignment;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.AirtightCannonRenderHandler;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.gas.flow.GasIntakeArea;
import net.ty.createcraftedbeginning.gas.visual.GasAreaOutline;
import net.ty.createcraftedbeginning.platform.client.ClientRenderBridge.Service;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public final class ClientRenderService implements Service {
    private static final int COLOR_RED = 0xFFFF5D6C;
    private static final int COLOR_INTAKE = 0x78D8E8;

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
    public void showGasAreaOutline(Player player, BlockPos sourcePos, Optional<Direction> direction, GasReleaseArea area, int color, long amount, boolean continuous) {
        if (!GogglesItem.isWearingGoggles(player) || !CCBConfig.client().outlines.showGasReleaseAreas.get()) {
            return;
        }

        AABB localBounds = area.bounds(Vec3.ZERO, new Vec3(0.5, 0.5, 0.5), direction.map(value -> Vec3.atLowerCornerOf(value.getNormal())).orElse(Vec3.ZERO));
        GasAreaOutline.show(player.level(), sourcePos, direction, localBounds, color, amount, continuous, CCBConfig.client().outlines.showGasReleaseAreas::get);
    }

    @Override
    public void showGasIntakeOutline(Player player, BlockPos sourcePos, Direction direction, long amount) {
        if (!GogglesItem.isWearingGoggles(player) || !CCBConfig.client().outlines.showGasIntakeAreas.get()) {
            return;
        }

        AABB localBounds = GasIntakeArea.bounds(new Vec3(0.5, 0.5, 0.5), Vec3.atLowerCornerOf(direction.getNormal()));
        GasAreaOutline.show(player.level(), sourcePos, Optional.of(direction), localBounds, COLOR_INTAKE, amount, true, CCBConfig.client().outlines.showGasIntakeAreas::get);
    }
}
