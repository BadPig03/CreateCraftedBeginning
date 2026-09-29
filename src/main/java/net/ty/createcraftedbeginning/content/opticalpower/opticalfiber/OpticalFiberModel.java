package net.ty.createcraftedbeginning.content.opticalpower.opticalfiber;

import com.simibubi.create.foundation.model.BakedModelWrapperWithData;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelData.Builder;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.common.util.TriState;
import net.ty.createcraftedbeginning.client.render.CCBPartialModels;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class OpticalFiberModel extends BakedModelWrapperWithData {
    private static final ModelProperty<Integer> DEVICE_PORTS = new ModelProperty<>();

    public OpticalFiberModel(BakedModel originalModel) {
        super(originalModel);
    }

    @Override
    protected Builder gatherModelData(Builder builder, BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData blockEntityData) {
        return builder.with(DEVICE_PORTS, OpticalFiberBlock.getDeviceConnections(level, pos, state));
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType) {
        return TriState.FALSE;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random, ModelData data, @Nullable RenderType renderType) {
        List<BakedQuad> base = super.getQuads(state, side, random, data, renderType);
        Integer ports = data.get(DEVICE_PORTS);
        if (state == null || side != null || ports == null || ports == 0) {
            return base;
        }
        if (renderType != null && !getRenderTypes(state, random, data).contains(renderType)) {
            return base;
        }

        List<BakedQuad> quads = new ArrayList<>(base);
        for (Direction direction : Iterate.directions) {
            if ((ports & 1 << direction.get3DDataValue()) == 0) {
                continue;
            }

            BakedModel port = CCBPartialModels.OPTICAL_FIBER_RIMS.get(direction).get();
            quads.addAll(port.getQuads(state, null, random, data, renderType));
        }
        return quads;
    }
}
