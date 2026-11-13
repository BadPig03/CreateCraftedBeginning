package net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer;

import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionHandler;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.ponder.api.PonderPalette;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.Projection;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.apache.commons.lang3.tuple.MutablePair;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID, value = Dist.CLIENT)
public final class ChunkStabilizerClient {
    private static final double OUTLINE_VIEW_DISTANCE_SQR = 9216;

    private ChunkStabilizerClient() {
    }

    public static void initialize() {
        ChunkStabilizerBlockEntity.setClientTicker(ChunkStabilizerClient::tick);
    }

    static void updateOutline(LocalPlayer player, ChunkStabilizerOutlinePacket packet) {
        if (!(player.level().getEntity(packet.entityId()) instanceof AbstractContraptionEntity entity)) {
            return;
        }

        Contraption contraption = entity.getContraption();
        Map<BlockPos, StructureBlockInfo> blocks = contraption.getBlocks();
        BlockPos localPos = packet.localPos();
        StructureBlockInfo info = blocks.get(localPos);
        if (info == null) {
            return;
        }

        BlockState state = info.state();
        if (!state.is(CCBBlocks.CHUNK_STABILIZER_BLOCK.get())) {
            return;
        }

        CompoundTag tag = info.nbt();
        tag = tag == null ? new CompoundTag() : tag.copy();
        ChunkStabilizerSerialization.writeShowOutline(tag, packet.showOutline());
        blocks.put(localPos, new StructureBlockInfo(localPos, state, tag));
    }

    static void updateMovingState(LocalPlayer player, ChunkStabilizerStatePacket packet) {
        if (!(player.level().getEntity(packet.entityId()) instanceof AbstractContraptionEntity entity)) {
            return;
        }

        Contraption contraption = entity.getContraption();
        Map<BlockPos, StructureBlockInfo> blocks = contraption.getBlocks();
        BlockPos localPos = packet.localPos();
        StructureBlockInfo info = blocks.get(localPos);
        if (info == null) {
            return;
        }

        BlockState state = info.state();
        if (!state.is(CCBBlocks.CHUNK_STABILIZER_BLOCK.get())) {
            return;
        }

        CompoundTag tag = info.nbt();
        tag = tag == null ? new CompoundTag() : tag.copy();
        ChunkStabilizerSerialization.writeMovingActive(tag, packet.active());
        ChunkStabilizerSerialization.writeBeamEnabled(tag, packet.beamEnabled());
        ChunkStabilizerSerialization.writeBeamColor(tag, packet.color());
        blocks.put(localPos, new StructureBlockInfo(localPos, state, tag));
    }

    @SubscribeEvent
    private static void tickMoving(Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        Level level = minecraft.level;
        if (player == null || level == null) {
            return;
        }

        boolean enabled = CCBConfig.server().opticalPower.chunkStabilizer.enableChunkLoading.get();
        boolean showOutlines = enabled && GogglesItem.isWearingGoggles(player) && CCBConfig.client().outlines.showChunkStabilizerChunks.get();
        boolean animate = !minecraft.isPaused();
        for (WeakReference<AbstractContraptionEntity> reference : ContraptionHandler.loadedContraptions.get(level).values()) {
            AbstractContraptionEntity entity = reference.get();
            if (entity == null || !entity.isAlive() || !ChunkStabilizerMovementBehaviour.supports(entity)) {
                continue;
            }

            Contraption contraption = entity.getContraption();
            for (MutablePair<StructureBlockInfo, MovementContext> actor : contraption.getActors()) {
                StructureBlockInfo info = contraption.getBlocks().get(actor.getLeft().pos());
                if (info == null || !info.state().is(CCBBlocks.CHUNK_STABILIZER_BLOCK.get())) {
                    continue;
                }

                MovementContext context = actor.getRight();
                BlockPos localPos = info.pos();
                CompoundTag tag = info.nbt();
                boolean active = ChunkStabilizerSerialization.readMovingActive(tag);
                if (animate) {
                    ChunkStabilizerMovementBehaviour.animation(context).tick(enabled && active);
                }
                if (!showOutlines || !active || contraption.isHiddenInPortal(localPos) || !ChunkStabilizerSerialization.readShowOutline(tag)) {
                    continue;
                }

                Vec3 position = entity.toGlobalVector(localPos.getCenter(), 1);
                if (player.distanceToSqr(position) > OUTLINE_VIEW_DISTANCE_SQR) {
                    continue;
                }

                showOutline(context, level, new ChunkPos(BlockPos.containing(position)));
            }
        }
    }

    private static void tick(ChunkStabilizerBlockEntity loader) {
        ChunkStabilizerBeamRenderer.track(loader);
        LocalPlayer player = Minecraft.getInstance().player;
        Level level = loader.getLevel();
        if (player == null || level == null || !loader.isShowingOutline() || !GogglesItem.isWearingGoggles(player) || !CCBConfig.client().outlines.showChunkStabilizerChunks.get()) {
            return;
        }

        Projection projection = SubLevelBridge.resolve(level, loader.getBlockPos());
        Vec3 position = projection.worldPosition();
        if (player.distanceToSqr(position) > OUTLINE_VIEW_DISTANCE_SQR) {
            return;
        }

        showOutline(loader, level, new ChunkPos(BlockPos.containing(position)));
    }

    private static void showOutline(Object key, Level level, ChunkPos chunk) {
        int minX = chunk.getMinBlockX();
        int minZ = chunk.getMinBlockZ();
        AABB area = new AABB(minX, level.getMinBuildHeight(), minZ, minX + 16, level.getMaxBuildHeight(), minZ + 16);
        Outliner.getInstance().showAABB(key, area).colored(PonderPalette.INPUT.getColor()).withFaceTexture(AllSpecialTextures.CHECKERED).lineWidth(0.0625F);
    }
}
