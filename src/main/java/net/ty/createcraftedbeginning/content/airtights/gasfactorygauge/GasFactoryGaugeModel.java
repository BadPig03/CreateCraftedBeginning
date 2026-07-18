package net.ty.createcraftedbeginning.content.airtights.gasfactorygauge;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelSlot;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelState;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock.PanelType;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelModel;
import com.simibubi.create.foundation.model.BakedQuadHelper;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasFactoryGaugeModel extends FactoryPanelModel {
    public GasFactoryGaugeModel(BakedModel originalModel) {
        super(originalModel);
    }

    @Override
    public void addPanel(List<BakedQuad> quads, BlockState state, PanelSlot slot, PanelType type, PanelState panelState, RandomSource random, ModelData data, RenderType renderType, boolean ponder) {
        boolean isNetworkPanel = type == PanelType.NETWORK;
        PartialModel panel = switch (panelState) {
            case PASSIVE -> isNetworkPanel ? CCBPartialModels.GAS_FACTORY_GAUGE_PANEL : CCBPartialModels.GAS_FACTORY_GAUGE_PANEL_RESTOCKER;
            case ACTIVE -> isNetworkPanel ? CCBPartialModels.GAS_FACTORY_GAUGE_PANEL_WITH_BULB : CCBPartialModels.GAS_FACTORY_GAUGE_PANEL_RESTOCKER_WITH_BULB;
        };

        List<BakedQuad> panelQuads = panel.get().getQuads(state, null, random, data, RenderType.solid());
        float xRot = Mth.RAD_TO_DEG * FactoryPanelBlock.getXRot(state);
        float yRot = Mth.RAD_TO_DEG * FactoryPanelBlock.getYRot(state);
        for (BakedQuad quad : panelQuads) {
            int[] vertices = quad.getVertices();
            int[] transformedVertices = Arrays.copyOf(vertices, vertices.length);
            Vec3 quadNormal = Vec3.atLowerCornerOf(quad.getDirection().getNormal());
            quadNormal = VecHelper.rotate(quadNormal, 180, Axis.Y);
            quadNormal = VecHelper.rotate(quadNormal, xRot + 90, Axis.X);
            quadNormal = VecHelper.rotate(quadNormal, yRot, Axis.Y);
            for (int i = 0; i < vertices.length / BakedQuadHelper.VERTEX_STRIDE; i++) {
                Vec3 vertex = BakedQuadHelper.getXYZ(vertices, i);
                vertex = vertex.add(slot.xOffset * 0.5, 0, slot.yOffset * 0.5);
                vertex = VecHelper.rotateCentered(vertex, 180, Axis.Y);
                vertex = VecHelper.rotateCentered(vertex, xRot + 90, Axis.X);
                vertex = VecHelper.rotateCentered(vertex, yRot, Axis.Y);
                BakedQuadHelper.setXYZ(transformedVertices, i, vertex);
                BakedQuadHelper.setNormalXYZ(transformedVertices, i, new Vec3(0, 1, 0));
            }

            Direction normal = Direction.fromDelta((int) Math.round(quadNormal.x), (int) Math.round(quadNormal.y), (int) Math.round(quadNormal.z));
            if (normal == null) {
                continue;
            }

            quads.add(new BakedQuad(transformedVertices, quad.getTintIndex(), normal, quad.getSprite(), !ponder && quad.isShade()));
        }
    }
}
