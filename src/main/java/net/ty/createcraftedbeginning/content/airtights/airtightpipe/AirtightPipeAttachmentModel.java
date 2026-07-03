package net.ty.createcraftedbeginning.content.airtights.airtightpipe;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
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
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelData.Builder;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.common.util.TriState;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeAttachmentTypes.AttachmentTypes;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeAttachmentTypes.AttachmentTypes.ComponentPartials;
import net.ty.createcraftedbeginning.api.gas.gases.behaviours.GasTransportBehaviour;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightPipeAttachmentModel extends BakedModelWrapperWithData {
    private static final ModelProperty<PipeModelData> PIPE_PROPERTY = new ModelProperty<>();

    private final boolean ambientOcclusion;

    public AirtightPipeAttachmentModel(BakedModel template, boolean ambientOcclusion) {
        super(template);
        this.ambientOcclusion = ambientOcclusion;
    }

    @Contract("_ -> new")
    public static AirtightPipeAttachmentModel withAO(BakedModel template) {
        return new AirtightPipeAttachmentModel(template, true);
    }

    private static void addQuads(List<BakedQuad> quads, @Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, PipeModelData pipeData, @Nullable RenderType renderType) {
        for (Direction direction : Iterate.directions) {
            AttachmentTypes type = pipeData.getAttachment(direction);
            for (ComponentPartials partial : type.partials) {
                quads.addAll(CCBPartialModels.AIRTIGHT_PIPE_ATTACHMENTS.get(partial).get(direction).get().getQuads(state, side, rand, data, renderType));
            }
        }
    }

    @Override
    protected Builder gatherModelData(Builder builder, BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData blockEntityData) {
        PipeModelData data = new PipeModelData();
        GasTransportBehaviour transport = BlockEntityBehaviour.get(level, pos, GasTransportBehaviour.TYPE);
        if (transport == null) {
            return builder.with(PIPE_PROPERTY, data);
        }

        Arrays.stream(Iterate.directions).forEach(direction -> data.putAttachment(direction, transport.getRenderedRimAttachment(level, pos, state, direction)));
        return builder.with(PIPE_PROPERTY, data);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return ambientOcclusion;
    }

    @Override
    public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType) {
        if (ambientOcclusion) {
            return TriState.TRUE;
        }
        return TriState.FALSE;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
        List<BakedQuad> quads = super.getQuads(state, side, rand, data, renderType);
        if (!data.has(PIPE_PROPERTY)) {
            return quads;
        }

        PipeModelData pipeData = data.get(PIPE_PROPERTY);
        quads = new ArrayList<>(quads);
        if (pipeData != null) {
            addQuads(quads, state, side, rand, data, pipeData, renderType);
        }
        return quads;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        List<ChunkRenderTypeSet> set = new ArrayList<>();
        set.add(super.getRenderTypes(state, rand, data));
        if (!data.has(PIPE_PROPERTY)) {
            return ChunkRenderTypeSet.union(set);
        }

        PipeModelData pipeData = data.get(PIPE_PROPERTY);
        for (Direction direction : Iterate.directions) {
            AttachmentTypes type = null;
            if (pipeData != null) {
                type = pipeData.getAttachment(direction);
            }
            if (type == null) {
                continue;
            }

            Arrays.stream(type.partials).map(partial -> CCBPartialModels.AIRTIGHT_PIPE_ATTACHMENTS.get(partial).get(direction).get().getRenderTypes(state, rand, data)).forEach(set::add);
        }
        return ChunkRenderTypeSet.union(set);
    }

    private static class PipeModelData {
        private final AttachmentTypes[] attachments;

        public PipeModelData() {
            attachments = new AttachmentTypes[Direction.values().length];
            Arrays.fill(attachments, AttachmentTypes.NONE);
        }

        public void putAttachment(Direction face, AttachmentTypes rim) {
            attachments[face.get3DDataValue()] = rim;
        }

        @Contract(pure = true)
        public AttachmentTypes getAttachment(Direction face) {
            return attachments[face.get3DDataValue()];
        }
    }
}
