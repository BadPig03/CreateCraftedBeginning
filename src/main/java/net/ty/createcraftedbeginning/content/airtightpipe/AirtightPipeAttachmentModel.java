package net.ty.createcraftedbeginning.content.airtightpipe;

import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.model.BakedModelWrapperWithData;
import net.createmod.catnip.data.Iterate;
import net.minecraft.client.Minecraft;
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
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.common.util.TriState;
import net.ty.createcraftedbeginning.registry.CCBPartialModels;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AirtightPipeAttachmentModel extends BakedModelWrapperWithData {
    private static final ModelProperty<PipeModelData> PIPE_PROPERTY = new ModelProperty<>();
    private final boolean ao;

    public AirtightPipeAttachmentModel(BakedModel template, boolean ao) {
        super(template);
        this.ao = ao;
    }

    public static AirtightPipeAttachmentModel withAO(BakedModel template) {
        return new AirtightPipeAttachmentModel(template, true);
    }

    @Override
    protected ModelData.Builder gatherModelData(ModelData.Builder builder, BlockAndTintGetter world, BlockPos pos, BlockState state, ModelData blockEntityData) {
        PipeModelData data = new PipeModelData();
        FluidTransportBehaviour transport = BlockEntityBehaviour.get(world, pos, FluidTransportBehaviour.TYPE);
        BracketedBlockEntityBehaviour bracket = BlockEntityBehaviour.get(world, pos, BracketedBlockEntityBehaviour.TYPE);

        if (transport != null) {
            for (Direction d : Iterate.directions) {
                data.putAttachment(d, transport.getRenderedRimAttachment(world, pos, state, d));
            }
        }
        if (bracket != null) {
            data.putBracket(bracket.getBracket());
        }

        return builder.with(PIPE_PROPERTY, data);
    }

    @Override
    public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        List<ChunkRenderTypeSet> set = new ArrayList<>();

        set.add(super.getRenderTypes(state, rand, data));

        if (data.has(PIPE_PROPERTY)) {
            PipeModelData pipeData = data.get(PIPE_PROPERTY);
            for (Direction d : Iterate.directions) {
                FluidTransportBehaviour.AttachmentTypes type = null;
                if (pipeData != null) {
                    type = pipeData.getAttachment(d);
                }
                if (type != null) {
                    for (FluidTransportBehaviour.AttachmentTypes.ComponentPartials partial : type.partials) {
                        ChunkRenderTypeSet attachmentRenderTypeSet = CCBPartialModels.AIRTIGHT_PIPE_ATTACHMENTS.get(partial).get(d).get().getRenderTypes(state, rand, data);
                        set.add(attachmentRenderTypeSet);
                    }
                }
            }
        }

        return ChunkRenderTypeSet.union(set);
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(BlockState state, Direction side, @NotNull RandomSource rand, @NotNull ModelData data, RenderType renderType) {
        List<BakedQuad> quads = super.getQuads(state, side, rand, data, renderType);
        if (data.has(PIPE_PROPERTY)) {
            PipeModelData pipeData = data.get(PIPE_PROPERTY);
            quads = new ArrayList<>(quads);
            if (pipeData != null) {
                addQuads(quads, state, side, rand, data, pipeData, renderType);
            }
        }
        return quads;
    }

    @Override
    public @NotNull TriState useAmbientOcclusion(@NotNull BlockState state, @NotNull ModelData data, @NotNull RenderType renderType) {
        if (ao) {
            return TriState.TRUE;
        } else {
            return TriState.FALSE;
        }
    }

    @Override
    public boolean useAmbientOcclusion() {
        return ao;
    }

    private void addQuads(List<BakedQuad> quads, BlockState state, Direction side, RandomSource rand, ModelData data, PipeModelData pipeData, RenderType renderType) {
        BakedModel bracket = pipeData.getBracket();
        if (bracket != null) {
            quads.addAll(bracket.getQuads(state, side, rand, data, renderType));
        }
        for (Direction d : Iterate.directions) {
            FluidTransportBehaviour.AttachmentTypes type = pipeData.getAttachment(d);
            for (FluidTransportBehaviour.AttachmentTypes.ComponentPartials partial : type.partials) {
                quads.addAll(CCBPartialModels.AIRTIGHT_PIPE_ATTACHMENTS.get(partial).get(d).get().getQuads(state, side, rand, data, renderType));
            }
        }
    }

    private static class PipeModelData {
        private final FluidTransportBehaviour.AttachmentTypes[] attachments;
        private BakedModel bracket;

        public PipeModelData() {
            attachments = new FluidTransportBehaviour.AttachmentTypes[6];
            Arrays.fill(attachments, FluidTransportBehaviour.AttachmentTypes.NONE);
        }

        public void putBracket(BlockState state) {
            if (state != null) {
                this.bracket = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
            }
        }

        public BakedModel getBracket() {
            return bracket;
        }

        public void putAttachment(Direction face, FluidTransportBehaviour.AttachmentTypes rim) {
            attachments[face.get3DDataValue()] = rim;
        }

        public FluidTransportBehaviour.AttachmentTypes getAttachment(Direction face) {
            return attachments[face.get3DDataValue()];
        }
    }
}
