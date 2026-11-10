package net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.content.opticalpower.amethystantenna.AmethystAntennaBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.amethystantenna.AmethystAntennaSerialization;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.AmethystLoaderBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.AmethystLoaderSerialization;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData.Node;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.apache.commons.lang3.tuple.MutablePair;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@EventBusSubscriber(modid = CCBAPI.MOD_ID)
public final class LoadingNetworkContraptions {
    private LoadingNetworkContraptions() {
    }

    public static void assemble(ServerLevel level, Contraption contraption, BlockPos offset) {
        if (SubLevelBridge.resolve(level, contraption.anchor).inSubLevel()) {
            return;
        }

        LoadingNetworkSavedData data = LoadingNetworkSavedData.get(level);
        BlockPos origin = contraption.anchor.offset(offset);
        Map<BlockPos, StructureBlockInfo> blocks = contraption.getBlocks();
        for (Node node : data.nodes()) {
            GlobalPos loaderPos = node.loader();
            GlobalPos antennaPos = node.antenna();
            if (loaderPos.dimension() != level.dimension()) {
                continue;
            }

            BlockPos local = loaderPos.pos().subtract(origin);
            StructureBlockInfo loader = blocks.get(local);
            StructureBlockInfo antenna = blocks.get(antennaPos.pos().subtract(origin));
            boolean hasLoader = loader != null && loader.state().is(CCBBlocks.AMETHYST_LOADER_BLOCK.get());
            boolean hasAntenna = antenna != null && antenna.state().is(CCBBlocks.AMETHYST_ANTENNA_BLOCK.get());
            if (!hasLoader && !hasAntenna) {
                continue;
            }

            data.runtime().discard(level.getServer(), loaderPos);
            if (!hasLoader) {
                data.removeAntenna(antennaPos, node.antennaId());
                continue;
            }

            data.removeLoader(loaderPos);
            if (!hasAntenna || !matches(blocks, local, loader, node)) {
                continue;
            }

            CompoundTag tag = loader.nbt();
            if (tag == null) {
                continue;
            }

            LoadingNetworkSerialization.writeTransfer(tag, node);
            if (contraption instanceof CarriageContraption) {
                continue;
            }

            data.rememberAssembly(node);
        }
    }

    public static void preserveMovingChoice(ServerLevel level, Contraption contraption, BlockPos local) {
        StructureBlockInfo info = contraption.getBlocks().get(local);
        CompoundTag tag = info == null ? null : info.nbt();
        if (contraption instanceof CarriageContraption || tag == null) {
            return;
        }

        Node captured = LoadingNetworkSerialization.readTransfer(tag);
        if (captured == null) {
            return;
        }

        Node current = LoadingNetworkSavedData.get(level).assemblyNode(captured.loaderId());
        if (current == null || !current.antennaId().equals(captured.antennaId()) || !Objects.equals(current.networkId(), captured.networkId())) {
            return;
        }

        LoadingNetworkSerialization.writeTransfer(tag, current);
    }

    public static Map<GlobalPos, Node> preparePlacement(ServerLevel level, Contraption contraption, StructureTransform transform) {
        Map<GlobalPos, Node> placements = new HashMap<>();
        if (contraption.disassembled || SubLevelBridge.resolve(level, transform.apply(BlockPos.ZERO)).inSubLevel()) {
            return placements;
        }

        LoadingNetworkSavedData data = LoadingNetworkSavedData.get(level);
        Map<BlockPos, StructureBlockInfo> blocks = contraption.getBlocks();
        for (Entry<BlockPos, Node> entry : transfers(blocks).entrySet()) {
            BlockPos local = entry.getKey();
            Node node = entry.getValue();
            Node current = contraption instanceof CarriageContraption ? data.trainNode(node.loaderId()) : data.assemblyNode(node.loaderId());
            if (current != null && current.antennaId().equals(node.antennaId()) && Objects.equals(current.networkId(), node.networkId())) {
                node = current;
            }
            data.forgetAssembly(node.loaderId());
            if (!matches(blocks, local, blocks.get(local), node)) {
                continue;
            }

            BlockPos antennaLocal = local.offset(node.antenna().pos().subtract(node.loader().pos()));
            ResourceKey<Level> dimension = level.dimension();
            GlobalPos loader = GlobalPos.of(dimension, transform.apply(local));
            GlobalPos antenna = GlobalPos.of(dimension, transform.apply(antennaLocal));
            Node placed = new Node(node.loaderId(), loader, node.antennaId(), antenna, node.networkId(), node.enabled(), node.beamEnabled(), node.name());
            placements.put(loader, placed);
            data.beginPlacement(loader);
        }
        return placements;
    }

    public static void finishPlacement(ServerLevel level, Map<GlobalPos, Node> placements) {
        LoadingNetworkSavedData data = LoadingNetworkSavedData.get(level);
        for (Node node : placements.values()) {
            GlobalPos pos = node.loader();
            BlockPos loaderPos = pos.pos();
            BlockPos antennaPos = node.antenna().pos();
            data.endPlacement(pos);
            if (!level.isLoaded(loaderPos) || !(level.getBlockEntity(loaderPos) instanceof AmethystLoaderBlockEntity loader) || !node.loaderId().equals(loader.networkNodeId())) {
                continue;
            }

            boolean valid = !level.isOutsideBuildHeight(antennaPos);
            if (valid && level.isLoaded(antennaPos)) {
                valid = false;
                if (level.getBlockEntity(antennaPos) instanceof AmethystAntennaBlockEntity antenna) {
                    LoadingNetworkBinding binding = antenna.binding();
                    valid = node.antennaId().equals(antenna.identity()) && loaderPos.equals(antennaPos.relative(antenna.getBlockState().getValue(DirectionalBlock.FACING).getOpposite())) && Objects.equals(node.networkId(), binding == null ? null : binding.id());
                }
            }
            if (valid) {
                data.register(node);
            }
            loader.refreshLoading();
        }
    }

    static Map<BlockPos, Node> members(Map<BlockPos, StructureBlockInfo> blocks) {
        Map<BlockPos, Node> members = transfers(blocks);
        members.entrySet().removeIf(entry -> {
            BlockPos local = entry.getKey();
            return !matches(blocks, local, blocks.get(local), entry.getValue());
        });
        return members;
    }

    static Map<BlockPos, Node> members(Contraption contraption) {
        Map<BlockPos, StructureBlockInfo> blocks = contraption.getBlocks();
        Map<BlockPos, Node> members = new HashMap<>();
        for (MutablePair<StructureBlockInfo, MovementContext> actor : contraption.getActors()) {
            BlockPos local = actor.getLeft().pos();
            StructureBlockInfo loader = blocks.get(local);
            Node node = readTransfer(loader);
            if (node == null || !matches(blocks, local, loader, node)) {
                continue;
            }

            members.put(local, node);
        }
        return members;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    private static void onJoin(EntityJoinLevelEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof AbstractContraptionEntity entity)) {
            return;
        }

        Contraption contraption = entity.getContraption();
        if (contraption == null || contraption instanceof CarriageContraption || SubLevelBridge.resolve(level, entity.position()).inSubLevel()) {
            return;
        }

        LoadingNetworkSavedData data = LoadingNetworkSavedData.get(level);
        Map<BlockPos, StructureBlockInfo> blocks = contraption.getBlocks();
        for (Entry<BlockPos, Node> entry : transfers(blocks).entrySet()) {
            BlockPos local = entry.getKey();
            Node node = entry.getValue();
            if (!matches(blocks, local, blocks.get(local), node)) {
                data.forgetAssembly(node.loaderId());
                continue;
            }

            data.rememberAssembly(node);
        }
    }

    @SubscribeEvent
    private static void onLeave(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof AbstractContraptionEntity entity)) {
            return;
        }

        RemovalReason reason = entity.getRemovalReason();
        Contraption contraption = entity.getContraption();
        if (reason == null || !reason.shouldDestroy() || contraption == null || contraption instanceof CarriageContraption) {
            return;
        }

        LoadingNetworkSavedData data = LoadingNetworkSavedData.get(level);
        for (Node node : transfers(contraption.getBlocks()).values()) {
            data.forgetAssembly(node.loaderId());
        }
    }

    private static Map<BlockPos, Node> transfers(Map<BlockPos, StructureBlockInfo> blocks) {
        Map<BlockPos, Node> transfers = new HashMap<>();
        for (StructureBlockInfo info : blocks.values()) {
            Node node = readTransfer(info);
            if (node == null) {
                continue;
            }

            transfers.put(info.pos(), node);
        }
        return transfers;
    }

    private static @Nullable Node readTransfer(@Nullable StructureBlockInfo loader) {
        if (loader == null || !loader.state().is(CCBBlocks.AMETHYST_LOADER_BLOCK.get())) {
            return null;
        }

        return LoadingNetworkSerialization.readTransfer(loader.nbt());
    }

    private static boolean matches(Map<BlockPos, StructureBlockInfo> blocks, BlockPos local, @Nullable StructureBlockInfo loader, Node node) {
        StructureBlockInfo antenna = blocks.get(local.offset(node.antenna().pos().subtract(node.loader().pos())));
        if (loader == null || antenna == null) {
            return false;
        }

        BlockState antennaState = antenna.state();
        if (!loader.state().is(CCBBlocks.AMETHYST_LOADER_BLOCK.get()) || !antennaState.is(CCBBlocks.AMETHYST_ANTENNA_BLOCK.get()) || !local.equals(antenna.pos().relative(antennaState.getValue(DirectionalBlock.FACING).getOpposite()))) {
            return false;
        }

        UUID loaderId = AmethystLoaderSerialization.readNetworkNodeId(loader.nbt());
        CompoundTag antennaTag = antenna.nbt();
        if (loaderId == null || antennaTag == null) {
            return false;
        }

        UUID antennaId = AmethystAntennaSerialization.readIdentity(antennaTag);
        if (antennaId == null) {
            return false;
        }

        LoadingNetworkBinding binding = LoadingNetworkBinding.read(antennaTag);
        if (!node.loaderId().equals(loaderId) || !node.antennaId().equals(antennaId)) {
            return false;
        }

        if (binding == null) {
            return node.networkId() == null;
        }

        return Objects.equals(node.networkId(), binding.id());
    }
}
