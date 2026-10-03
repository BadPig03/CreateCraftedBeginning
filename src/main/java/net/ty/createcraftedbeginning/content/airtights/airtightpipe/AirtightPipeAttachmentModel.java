package net.ty.createcraftedbeginning.content.airtights.airtightpipe;

import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.model.BakedModelWrapperWithData;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
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
import net.neoforged.neoforge.client.model.data.ModelData.Builder;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.common.util.TriState;
import net.ty.createcraftedbeginning.client.render.CCBPartialModels;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightPipeAttachmentModel extends BakedModelWrapperWithData {
    private static final ModelProperty<PipeModelData> PIPE_PROPERTY = new ModelProperty<>();

    private final boolean ambientOcclusion;

    private AirtightPipeAttachmentModel(BakedModel template, boolean ambientOcclusion) {
        super(template);
        this.ambientOcclusion = ambientOcclusion;
    }

    @Contract("_ -> new")
    public static AirtightPipeAttachmentModel withAO(BakedModel template) {
        return new AirtightPipeAttachmentModel(template, true);
    }

    private static AirtightPipeAttachmentPartial[] getPartials(AirtightPipeAttachmentTypes attachmentType) {
        return switch (attachmentType) {
            case NONE -> new AirtightPipeAttachmentPartial[0];
            case RIM -> new AirtightPipeAttachmentPartial[]{AirtightPipeAttachmentPartial.RIM};
            case DRAIN -> new AirtightPipeAttachmentPartial[]{AirtightPipeAttachmentPartial.DRAIN};
            case INLET_RIM -> new AirtightPipeAttachmentPartial[]{AirtightPipeAttachmentPartial.INLET_RIM};
            case INLET_DRAIN -> new AirtightPipeAttachmentPartial[]{AirtightPipeAttachmentPartial.INLET_DRAIN};
            case OUTLET_RIM -> new AirtightPipeAttachmentPartial[]{AirtightPipeAttachmentPartial.OUTLET_RIM};
            case OUTLET_DRAIN -> new AirtightPipeAttachmentPartial[]{AirtightPipeAttachmentPartial.OUTLET_DRAIN};
        };
    }

    private static Direction getAttachmentModelDirection(AirtightPipeAttachmentTypes attachmentType, Direction connectionDirection) {
        if (attachmentType != AirtightPipeAttachmentTypes.INLET_RIM && attachmentType != AirtightPipeAttachmentTypes.INLET_DRAIN) {
            return connectionDirection;
        }

        return connectionDirection.getOpposite();
    }

    @Override
    protected Builder gatherModelData(Builder builder, BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData blockEntityData) {
        PipeModelData pipeData = new PipeModelData();
        boolean cased = state.hasProperty(AirtightPipeBlock.CASED) && state.getValue(AirtightPipeBlock.CASED);
        GasTransportBehaviour transport = BlockEntityBehaviour.get(level, pos, GasTransportBehaviour.TYPE);
        BracketedBlockEntityBehaviour bracket = cased ? null : BlockEntityBehaviour.get(level, pos, BracketedBlockEntityBehaviour.TYPE);
        for (Direction direction : Iterate.directions) {
            AirtightPipeAttachmentTypes attachmentType = AirtightPipeAttachments.resolve(level, pos, state, direction, transport);
            if (cased && attachmentType == AirtightPipeAttachmentTypes.RIM) {
                attachmentType = AirtightPipeAttachmentTypes.NONE;
            }

            pipeData.putAttachment(direction, attachmentType);
        }

        if (bracket != null) {
            pipeData.putBracket(bracket.getBracket());
        }

        return builder.with(PIPE_PROPERTY, pipeData);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return ambientOcclusion;
    }

    @Override
    public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType) {
        if (!ambientOcclusion) {
            return TriState.FALSE;
        }

        return TriState.TRUE;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random, ModelData modelData, @Nullable RenderType renderType) {
        List<BakedQuad> quads = super.getQuads(state, side, random, modelData, renderType);
        if (!modelData.has(PIPE_PROPERTY)) {
            return quads;
        }

        PipeModelData pipeData = modelData.get(PIPE_PROPERTY);
        quads = new ArrayList<>(quads);
        if (pipeData == null) {
            return quads;
        }

        BakedModel bracketModel = pipeData.getBracket();
        if (bracketModel != null) {
            quads.addAll(bracketModel.getQuads(state, side, random, modelData, renderType));
        }

        for (Direction direction : Iterate.directions) {
            AirtightPipeAttachmentTypes attachmentType = pipeData.getAttachment(direction);
            Direction modelDirection = getAttachmentModelDirection(attachmentType, direction);
            for (AirtightPipeAttachmentPartial partial : getPartials(attachmentType)) {
                BakedModel attachmentModel = CCBPartialModels.AIRTIGHT_PIPE_ATTACHMENTS.get(partial).get(modelDirection).get();
                quads.addAll(attachmentModel.getQuads(state, side, random, modelData, renderType));
            }
        }

        return quads;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData modelData) {
        List<ChunkRenderTypeSet> renderTypes = new ArrayList<>();
        renderTypes.add(super.getRenderTypes(state, random, modelData));
        if (!modelData.has(PIPE_PROPERTY)) {
            return ChunkRenderTypeSet.union(renderTypes);
        }

        PipeModelData pipeData = modelData.get(PIPE_PROPERTY);
        if (pipeData == null) {
            return ChunkRenderTypeSet.union(renderTypes);
        }

        for (Direction direction : Iterate.directions) {
            AirtightPipeAttachmentTypes attachmentType = pipeData.getAttachment(direction);
            Direction modelDirection = getAttachmentModelDirection(attachmentType, direction);
            for (AirtightPipeAttachmentPartial partial : getPartials(attachmentType)) {
                BakedModel attachmentModel = CCBPartialModels.AIRTIGHT_PIPE_ATTACHMENTS.get(partial).get(modelDirection).get();
                renderTypes.add(attachmentModel.getRenderTypes(state, random, modelData));
            }
        }

        return ChunkRenderTypeSet.union(renderTypes);
    }

    private static final class PipeModelData {
        private final AirtightPipeAttachmentTypes[] attachments;
        @Nullable
        private BakedModel bracket;

        private PipeModelData() {
            attachments = new AirtightPipeAttachmentTypes[Direction.values().length];
            Arrays.fill(attachments, AirtightPipeAttachmentTypes.NONE);
        }

        private void putAttachment(Direction direction, AirtightPipeAttachmentTypes attachment) {
            attachments[direction.get3DDataValue()] = attachment;
        }

        private void putBracket(@Nullable BlockState bracketState) {
            if (bracketState == null) {
                return;
            }

            bracket = Minecraft.getInstance().getBlockRenderer().getBlockModel(bracketState);
        }

        @Nullable
        private BakedModel getBracket() {
            return bracket;
        }

        private AirtightPipeAttachmentTypes getAttachment(Direction direction) {
            return attachments[direction.get3DDataValue()];
        }
    }
}
