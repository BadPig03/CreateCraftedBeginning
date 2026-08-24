package net.ty.createcraftedbeginning.content.airtights.airtightpipe;

import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.content.fluids.pipes.IAxisPipe;
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
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelData.Builder;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.common.util.TriState;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeAttachmentTypes.AttachmentTypes;
import net.ty.createcraftedbeginning.content.airtights.airtightpump.AirtightPumpBlock;
import net.ty.createcraftedbeginning.content.airtights.gas.behaviours.GasTransportBehaviour;
import net.ty.createcraftedbeginning.foundation.client.AirtightPipeAttachmentPartial;
import net.ty.createcraftedbeginning.foundation.client.CCBPartialModels;
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

    protected final boolean ambientOcclusion;

    public AirtightPipeAttachmentModel(BakedModel template, boolean ambientOcclusion) {
        super(template);
        this.ambientOcclusion = ambientOcclusion;
    }

    @Contract("_ -> new")
    public static AirtightPipeAttachmentModel withAO(BakedModel template) {
        return new AirtightPipeAttachmentModel(template, true);
    }

    private static void addQuads(List<BakedQuad> quads, @Nullable BlockState state, @Nullable Direction side, RandomSource random, ModelData modelData, PipeModelData pipeData, @Nullable RenderType renderType) {
        BakedModel bracketModel = pipeData.getBracket();
        if (bracketModel != null) {
            quads.addAll(bracketModel.getQuads(state, side, random, modelData, renderType));
        }

        for (Direction direction : Iterate.directions) {
            AttachmentTypes attachmentType = pipeData.getAttachment(direction);
            for (AirtightPipeAttachmentPartial partial : getPartials(attachmentType)) {
                BakedModel attachmentModel = CCBPartialModels.AIRTIGHT_PIPE_ATTACHMENTS.get(partial).get(direction).get();
                quads.addAll(attachmentModel.getQuads(state, side, random, modelData, renderType));
            }
        }
    }

    private static AttachmentTypes getFallbackAttachment(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction direction) {
        BlockPos adjacentPos = pos.relative(direction);
        BlockState adjacentState = level.getBlockState(adjacentPos);
        Block adjacentBlock = adjacentState.getBlock();

        if (state.getBlock() instanceof AirtightPumpBlock) {
            if (adjacentBlock instanceof IAirtightPipeDrain drain && drain.shouldRenderDrain(level, adjacentPos, adjacentState, direction.getOpposite())) {
                return AttachmentTypes.DRAIN;
            }
            return AttachmentTypes.NONE;
        }

        if (!state.hasProperty(BlockStateProperties.AXIS)) {
            return AttachmentTypes.NONE;
        }

        Axis pipeAxis = state.getValue(BlockStateProperties.AXIS);
        if (pipeAxis != direction.getAxis()) {
            return AttachmentTypes.NONE;
        }

        if (adjacentBlock instanceof IAxisPipe axisPipe && axisPipe.getAxis(adjacentState) == pipeAxis) {
            return AttachmentTypes.NONE;
        }

        if (adjacentBlock instanceof IAirtightPipeDrain drain && drain.shouldRenderDrain(level, adjacentPos, adjacentState, direction.getOpposite())) {
            return AttachmentTypes.DRAIN;
        }
        return AttachmentTypes.RIM;
    }

    private static AirtightPipeAttachmentPartial[] getPartials(AttachmentTypes attachmentType) {
        return switch (attachmentType) {
            case NONE -> new AirtightPipeAttachmentPartial[0];
            case RIM -> new AirtightPipeAttachmentPartial[]{AirtightPipeAttachmentPartial.RIM};
            case DRAIN -> new AirtightPipeAttachmentPartial[]{AirtightPipeAttachmentPartial.DRAIN};
        };
    }

    @Override
    protected Builder gatherModelData(Builder builder, BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData blockEntityData) {
        PipeModelData pipeData = new PipeModelData();
        if (state.hasProperty(AirtightPipeBlock.CASED) && state.getValue(AirtightPipeBlock.CASED)) {
            return builder.with(PIPE_PROPERTY, pipeData);
        }

        GasTransportBehaviour transport = BlockEntityBehaviour.get(level, pos, GasTransportBehaviour.TYPE);
        BracketedBlockEntityBehaviour bracket = BlockEntityBehaviour.get(level, pos, BracketedBlockEntityBehaviour.TYPE);
        for (Direction direction : Iterate.directions) {
            AttachmentTypes attachmentType = transport == null ? getFallbackAttachment(level, pos, state, direction) : transport.getRenderedRimAttachment(level, pos, state, direction);
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
        return ambientOcclusion ? TriState.TRUE : TriState.FALSE;
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

        addQuads(quads, state, side, random, modelData, pipeData, renderType);
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
            AttachmentTypes attachmentType = pipeData.getAttachment(direction);
            for (AirtightPipeAttachmentPartial partial : getPartials(attachmentType)) {
                BakedModel attachmentModel = CCBPartialModels.AIRTIGHT_PIPE_ATTACHMENTS.get(partial).get(direction).get();
                renderTypes.add(attachmentModel.getRenderTypes(state, random, modelData));
            }
        }
        return ChunkRenderTypeSet.union(renderTypes);
    }

    protected static class PipeModelData {
        private final AttachmentTypes[] attachments;
        @Nullable
        private BakedModel bracket;

        protected PipeModelData() {
            attachments = new AttachmentTypes[Direction.values().length];
            Arrays.fill(attachments, AttachmentTypes.NONE);
        }

        private void putAttachment(Direction direction, AttachmentTypes attachment) {
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

        @Contract(pure = true)
        public AttachmentTypes getAttachment(Direction direction) {
            return attachments[direction.get3DDataValue()];
        }
    }
}
