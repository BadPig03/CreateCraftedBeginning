package net.ty.createcraftedbeginning.content.opticalpower.amethystantenna;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.ChunkStabilizerBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkBinding;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData.Node;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData.PhysicalNode;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystAntennaConnection {
    private AmethystAntennaConnection() {
    }

    static boolean canAttach(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockPos target = pos.relative(context.getClickedFace().getOpposite());
        if (!level.isLoaded(target)) {
            return false;
        }

        if (!CCBBlocks.CHUNK_STABILIZER_BLOCK.has(level.getBlockState(target))) {
            return false;
        }

        if (!Objects.equals(SubLevelBridge.subLevelId(level, pos), SubLevelBridge.subLevelId(level, target))) {
            return false;
        }

        if (level instanceof ServerLevel serverLevel) {
            LoadingNetworkSavedData data = LoadingNetworkSavedData.get(serverLevel);
            GlobalPos loader = GlobalPos.of(level.dimension(), target);
            if (data.nodeAt(loader) != null || data.physicalNodes().stream().anyMatch(member -> member.node().loader().equals(loader))) {
                return false;
            }
        }

        for (Direction side : Iterate.directions) {
            BlockPos neighbor = target.relative(side);
            if (!level.isLoaded(neighbor)) {
                continue;
            }

            BlockState state = level.getBlockState(neighbor);
            if (!(state.getBlock() instanceof AmethystAntennaBlock) || state.getValue(DirectionalBlock.FACING) != side) {
                continue;
            }

            return false;
        }
        return true;
    }

    static boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos target = pos.relative(state.getValue(DirectionalBlock.FACING).getOpposite());
        if (level instanceof Level world) {
            if (!Objects.equals(SubLevelBridge.subLevelId(world, pos), SubLevelBridge.subLevelId(world, target))) {
                return false;
            }

            if (!world.isLoaded(target)) {
                return true;
            }
        }

        return CCBBlocks.CHUNK_STABILIZER_BLOCK.has(level.getBlockState(target));
    }

    static void reconcile(AmethystAntennaBlockEntity antenna, @Nullable LoadingNetworkBinding binding) {
        if (antenna.isRemoved() || !(antenna.getLevel() instanceof ServerLevel level)) {
            return;
        }

        BlockPos pos = antenna.getBlockPos();
        BlockPos target = pos.relative(antenna.getBlockState().getValue(DirectionalBlock.FACING).getOpposite());
        if (!level.isLoaded(target)) {
            return;
        }

        LoadingNetworkSavedData data = LoadingNetworkSavedData.get(level);
        ResourceKey<Level> dimension = level.dimension();
        GlobalPos antennaPos = GlobalPos.of(dimension, pos);
        GlobalPos loaderPos = GlobalPos.of(dimension, target);
        if (data.isPlacing(loaderPos)) {
            return;
        }

        UUID host = SubLevelBridge.subLevelId(level, pos);
        if (!Objects.equals(host, SubLevelBridge.subLevelId(level, target)) || !(level.getBlockEntity(target) instanceof ChunkStabilizerBlockEntity loader)) {
            data.removeAntenna(antennaPos, antenna.identity());
            return;
        }

        UUID network = binding == null ? null : binding.id();
        Node node = new Node(loader.getOrCreateNetworkNodeId(), loaderPos, antenna.identity(), antennaPos, network, true, loader.beamEnabled(), loader.displayName());
        if (host == null) {
            data.register(node);
        }
        else {
            data.register(new PhysicalNode(host, node));
        }
        loader.refreshLoading();
    }

    static void remove(ServerLevel level, BlockPos pos, BlockState state, UUID identity) {
        LoadingNetworkSavedData.get(level).removeAntenna(GlobalPos.of(level.dimension(), pos), identity);
        BlockPos target = pos.relative(state.getValue(DirectionalBlock.FACING).getOpposite());
        if (SubLevelBridge.subLevelId(level, target) == null || !level.isLoaded(target) || !(level.getBlockEntity(target) instanceof ChunkStabilizerBlockEntity loader)) {
            return;
        }

        loader.refreshLoading();
    }
}
