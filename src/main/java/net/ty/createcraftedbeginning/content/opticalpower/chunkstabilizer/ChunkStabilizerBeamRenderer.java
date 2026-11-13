package net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionHandler;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.apache.commons.lang3.tuple.MutablePair;
import org.joml.Vector3f;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID, value = Dist.CLIENT)
final class ChunkStabilizerBeamRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");
    private static final int HEIGHT = 1024;
    private static final double VIEW_DISTANCE_SQR = 65536;
    private static final Set<ChunkStabilizerBlockEntity> LOADERS = Collections.newSetFromMap(new WeakHashMap<>());

    private ChunkStabilizerBeamRenderer() {
    }

    static void track(ChunkStabilizerBlockEntity loader) {
        if (loader.isVirtual()) {
            return;
        }

        LOADERS.add(loader);
    }

    static void renderBeam(PoseStack pose, MultiBufferSource buffers, long time, float partialTick, int color) {
        BeaconRenderer.renderBeaconBeam(pose, buffers, TEXTURE, partialTick, 1, time, -HEIGHT, HEIGHT * 2, color, 0.12F, 0.2F);
    }

    static void collectMovingRequest(List<ChunkStabilizerBeamRequest> requests, Vec3 position, int color) {
        requests.add(new ChunkStabilizerBeamRequest(position, color, false));
    }

    static void renderRequests(PoseStack pose, MultiBufferSource buffers, Vec3 camera, long time, float partialTick, List<ChunkStabilizerBeamRequest> requests) {
        Map<BeamColumn, List<ChunkStabilizerBeamRequest>> staticColumns = new LinkedHashMap<>();
        for (ChunkStabilizerBeamRequest request : requests) {
            if (request.mergeStaticColumn()) {
                staticColumns.computeIfAbsent(BeamColumn.of(request.position()), ignored -> new ArrayList<>()).add(request);
            }
            else {
                renderAt(pose, buffers, request.position(), camera, time, partialTick, request.color());
            }
        }

        for (List<ChunkStabilizerBeamRequest> column : staticColumns.values()) {
            renderAt(pose, buffers, averagePosition(column), camera, time, partialTick, averageColor(column));
        }
    }

    @SubscribeEvent
    private static void render(RenderLevelStageEvent event) {
        if (event.getStage() != Stage.AFTER_BLOCK_ENTITIES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LOADERS.removeIf(loader -> loader.isRemoved() || loader.getLevel() != level);
        if (level == null || !CCBConfig.server().opticalPower.chunkStabilizer.enableChunkLoading.get()) {
            return;
        }

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        BufferSource buffers = minecraft.renderBuffers().bufferSource();
        List<ChunkStabilizerBeamRequest> requests = new ArrayList<>();
        for (ChunkStabilizerBlockEntity loader : LOADERS) {
            if (!loader.isLoadingActive() || !loader.beamEnabled()) {
                continue;
            }

            BlockPos pos = loader.getBlockPos();
            Vec3 center = pos.getCenter();
            boolean physical = SubLevelBridge.subLevelId(level, pos) != null;
            Vec3 world = center;
            if (physical) {
                world = SubLevelBridge.createRenderTransform(level, center, partialTick).transformPosition(center);
            }
            requests.add(new ChunkStabilizerBeamRequest(world, loader.beamColor(), !physical));
        }
        for (WeakReference<AbstractContraptionEntity> reference : ContraptionHandler.loadedContraptions.get(level).values()) {
            AbstractContraptionEntity entity = reference.get();
            if (entity == null || !entity.isAlive() || !ChunkStabilizerMovementBehaviour.supports(entity)) {
                continue;
            }

            Contraption contraption = entity.getContraption();
            Map<BlockPos, StructureBlockInfo> blocks = contraption.getBlocks();
            PoseStack model = new PoseStack();
            entity.applyLocalTransforms(model, partialTick);
            Vec3 anchor = new Vec3(Mth.lerp(partialTick, entity.xOld, entity.getX()), Mth.lerp(partialTick, entity.yOld, entity.getY()), Mth.lerp(partialTick, entity.zOld, entity.getZ()));
            for (MutablePair<StructureBlockInfo, MovementContext> actor : contraption.getActors()) {
                StructureBlockInfo info = blocks.get(actor.getLeft().pos());
                if (info == null || !info.state().is(CCBBlocks.CHUNK_STABILIZER_BLOCK.get())) {
                    continue;
                }

                BlockPos localPos = info.pos();
                CompoundTag tag = info.nbt();
                if (contraption.isHiddenInPortal(localPos) || !ChunkStabilizerSerialization.readMovingActive(tag) || !ChunkStabilizerSerialization.readBeamEnabled(tag)) {
                    continue;
                }

                Vector3f center = model.last().pose().transformPosition(localPos.getCenter().toVector3f());
                Vec3 world = new Vec3(center.x, center.y, center.z).add(anchor);
                collectMovingRequest(requests, world, ChunkStabilizerSerialization.readBeamColor(tag));
            }
        }
        renderRequests(pose, buffers, camera, level.getGameTime(), partialTick, requests);
        buffers.endBatch(RenderType.beaconBeam(TEXTURE, false));
        buffers.endBatch(RenderType.beaconBeam(TEXTURE, true));
    }

    private static Vec3 averagePosition(List<ChunkStabilizerBeamRequest> requests) {
        ChunkStabilizerBeamRequest first = requests.getFirst();
        double y = 0;
        for (ChunkStabilizerBeamRequest request : requests) {
            y += request.position().y;
        }
        return new Vec3(first.position().x, y / requests.size(), first.position().z);
    }

    private static int averageColor(List<ChunkStabilizerBeamRequest> requests) {
        long red = 0;
        long green = 0;
        long blue = 0;
        for (ChunkStabilizerBeamRequest request : requests) {
            int color = request.color();
            red += color >> 16 & 0xFF;
            green += color >> 8 & 0xFF;
            blue += color & 0xFF;
        }
        int count = requests.size();
        int averagedRed = (int) ((red + count / 2) / count);
        int averagedGreen = (int) ((green + count / 2) / count);
        int averagedBlue = (int) ((blue + count / 2) / count);
        return 0xFF000000 | averagedRed << 16 | averagedGreen << 8 | averagedBlue;
    }

    private static void renderAt(PoseStack pose, MultiBufferSource buffers, Vec3 position, Vec3 camera, long time, float partialTick, int color) {
        double dx = position.x - camera.x;
        double dz = position.z - camera.z;
        if (dx * dx + dz * dz > VIEW_DISTANCE_SQR) {
            return;
        }

        pose.pushPose();
        try {
            pose.translate(dx - 0.5, position.y - camera.y, dz - 0.5);
            renderBeam(pose, buffers, time, partialTick, color);
        }
        finally {
            pose.popPose();
        }
    }

    private record BeamColumn(int x, int z) {
        private static BeamColumn of(Vec3 position) {
            return new BeamColumn(Mth.floor(position.x), Mth.floor(position.z));
        }
    }
}
