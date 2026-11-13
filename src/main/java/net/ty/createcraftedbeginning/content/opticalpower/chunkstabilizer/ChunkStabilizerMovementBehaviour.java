package net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorVisual;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkContraptions;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData;
import net.ty.createcraftedbeginning.mixin.common.accessor.ContraptionAccessor;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.apache.commons.lang3.tuple.MutablePair;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ChunkStabilizerMovementBehaviour implements MovementBehaviour {
    static boolean supports(AbstractContraptionEntity entity) {
        return entity.getContraption() != null && !SubLevelBridge.resolve(entity.level(), entity.position()).inSubLevel();
    }

    static ChunkStabilizerAnimation animation(MovementContext context) {
        if (context.temporaryData instanceof ChunkStabilizerAnimation animation) {
            return animation;
        }

        ChunkStabilizerAnimation animation = new ChunkStabilizerAnimation();
        context.temporaryData = animation;
        return animation;
    }

    static void syncActive(AbstractContraptionEntity entity, Set<BlockPos> active) {
        ServerLevel level = (ServerLevel) entity.level();
        LoadingNetworkSavedData data = LoadingNetworkSavedData.get(level);
        Contraption contraption = entity.getContraption();
        boolean train = contraption instanceof CarriageContraption;
        Map<BlockPos, StructureBlockInfo> blocks = contraption.getBlocks();
        Map<BlockPos, CompoundTag> updates = ((ContraptionAccessor) contraption).ccb$getUpdateTags();
        for (MutablePair<StructureBlockInfo, MovementContext> actor : contraption.getActors()) {
            BlockPos pos = actor.getLeft().pos();
            StructureBlockInfo info = blocks.get(pos);
            if (info == null) {
                continue;
            }

            BlockState state = info.state();
            if (!state.is(CCBBlocks.CHUNK_STABILIZER_BLOCK.get())) {
                continue;
            }

            boolean running = active.contains(pos);
            CompoundTag tag = info.nbt();
            UUID identity = ChunkStabilizerSerialization.readNetworkNodeId(tag);
            boolean beamEnabled = data.contraptionBeamEnabled(identity, train);
            CompoundTag update = updates.computeIfAbsent(pos, ignored -> new CompoundTag());
            int color = data.color(GlobalPos.of(level.dimension(), BlockPos.containing(entity.toGlobalVector(pos.getCenter(), 1))), identity);
            if (ChunkStabilizerSerialization.hasMovingActive(tag, running) && ChunkStabilizerSerialization.hasMovingActive(update, running) && ChunkStabilizerSerialization.readBeamColor(tag) == color && ChunkStabilizerSerialization.readBeamColor(update) == color && ChunkStabilizerSerialization.readBeamEnabled(tag) == beamEnabled && ChunkStabilizerSerialization.readBeamEnabled(update) == beamEnabled) {
                continue;
            }

            tag = tag == null ? new CompoundTag() : tag.copy();
            ChunkStabilizerSerialization.writeMovingActive(tag, running);
            ChunkStabilizerSerialization.writeBeamEnabled(tag, beamEnabled);
            ChunkStabilizerSerialization.writeBeamColor(tag, color);
            blocks.put(pos, new StructureBlockInfo(pos, state, tag));
            ChunkStabilizerSerialization.writeMovingActive(update, running);
            ChunkStabilizerSerialization.writeBeamEnabled(update, beamEnabled);
            ChunkStabilizerSerialization.writeBeamColor(update, color);
            CatnipServices.NETWORK.sendToClientsTrackingEntity(entity, new ChunkStabilizerStatePacket(entity.getId(), pos, running, color, beamEnabled));
        }
    }

    static void clearTrainActive(CarriageContraptionEntity entity) {
        if (!(entity.getContraption() instanceof CarriageContraption contraption)) {
            return;
        }

        Map<BlockPos, CompoundTag> updates = ((ContraptionAccessor) contraption).ccb$getUpdateTags();
        for (Entry<BlockPos, StructureBlockInfo> entry : contraption.getBlocks().entrySet()) {
            StructureBlockInfo info = entry.getValue();
            BlockState state = info.state();
            if (!state.is(CCBBlocks.CHUNK_STABILIZER_BLOCK.get())) {
                continue;
            }

            BlockPos pos = entry.getKey();
            CompoundTag tag = info.nbt();
            CompoundTag update = updates.get(pos);
            if (!ChunkStabilizerSerialization.readMovingActive(tag) && !ChunkStabilizerSerialization.readMovingActive(update)) {
                continue;
            }

            CompoundTag inactive = tag == null ? new CompoundTag() : tag.copy();
            ChunkStabilizerSerialization.writeMovingActive(inactive, false);
            entry.setValue(new StructureBlockInfo(info.pos(), state, inactive));
            if (update != null) {
                ChunkStabilizerSerialization.writeMovingActive(update, false);
            }
            CatnipServices.NETWORK.sendToClientsTrackingEntity(entity, new ChunkStabilizerStatePacket(entity.getId(), pos, false, ChunkStabilizerSerialization.readBeamColor(inactive), ChunkStabilizerSerialization.readBeamEnabled(inactive)));
        }
    }

    @Override
    public @Nullable ItemStack canBeDisabledVia(MovementContext context) {
        return null;
    }

    @Override
    public void stopMoving(MovementContext context) {
        if (!(context.world instanceof ServerLevel level)) {
            return;
        }

        LoadingNetworkContraptions.preserveMovingChoice(level, context.contraption, context.localPos);
    }

    @Override
    public boolean disableBlockEntityRendering() {
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices, MultiBufferSource buffers) {
        if (VisualizationManager.supportsVisualization(context.world)) {
            return;
        }

        float angle = animation(context).angle(AnimationTickHolder.getPartialTicks(context.world));
        ChunkStabilizerRenderer.renderInContraption(context, renderWorld, matrices, buffers, angle);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public ActorVisual createVisual(VisualizationContext visualizationContext, VirtualRenderWorld renderWorld, MovementContext context) {
        return new ChunkStabilizerActorVisual(visualizationContext, renderWorld, context);
    }
}
