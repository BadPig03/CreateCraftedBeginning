package net.ty.createcraftedbeginning.compat.sable;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DirectionalBlock;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent.Post;
import net.ty.createcraftedbeginning.content.opticalpower.amethystantenna.AmethystAntennaBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.ChunkStabilizerBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkBinding;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData.Node;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData.PhysicalNode;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class SableLoadingNetwork {
    private static final int AUDIT_INTERVAL = 20;

    private SableLoadingNetwork() {
    }

    public static List<Move> prepareMove(ServerLevel source, AssemblyTransformAccess transform, Iterable<BlockPos> positions) {
        Set<BlockPos> selected = new HashSet<>();
        for (BlockPos pos : positions) {
            selected.add(pos.immutable());
        }
        LoadingNetworkSavedData data = LoadingNetworkSavedData.get(source);
        List<Node> members = new ArrayList<>(data.nodes());
        for (PhysicalNode member : data.physicalNodes()) {
            members.add(member.node());
        }
        ServerLevel target = transform.ccb$getResultingLevel();
        List<Move> moves = new ArrayList<>();
        for (Node node : members) {
            if (node.loader().dimension() != source.dimension()) {
                continue;
            }

            boolean loaderMoved = selected.contains(node.loader().pos());
            boolean antennaMoved = selected.contains(node.antenna().pos());
            if (!loaderMoved && !antennaMoved) {
                continue;
            }

            boolean together = loaderMoved && antennaMoved && matches(source, node, false);
            data.runtime().discard(source.getServer(), node.loader());
            if (!loaderMoved) {
                data.removeAntenna(node.antenna(), node.antennaId());
                continue;
            }

            data.removeLoader(node.loader());
            if (!together) {
                continue;
            }

            GlobalPos loader = GlobalPos.of(target.dimension(), transform.ccb$transform(node.loader().pos()));
            GlobalPos antenna = GlobalPos.of(target.dimension(), transform.ccb$transform(node.antenna().pos()));
            Node moved = new Node(node.loaderId(), loader, node.antennaId(), antenna, node.networkId(), node.enabled(), node.beamEnabled(), node.name());
            data.beginPlacement(loader);
            moves.add(new Move(node, moved));
        }
        return moves;
    }

    public static void finishMove(ServerLevel source, ServerLevel target, List<Move> moves) {
        LoadingNetworkSavedData data = LoadingNetworkSavedData.get(source);
        for (Move move : moves) {
            data.endPlacement(move.target().loader());
        }
        for (Move move : moves) {
            if (restore(target, move.target(), data)) {
                continue;
            }

            restore(source, move.source(), data);
        }
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(SableLoadingNetwork::onTick);
        SableLoadingNetworkControl.register();
    }

    static boolean matches(ServerLevel level, Node node, boolean allowUnloadedAntenna) {
        BlockPos pos = node.loader().pos();
        BlockPos antennaPos = node.antenna().pos();
        if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof ChunkStabilizerBlockEntity loader) || !node.loaderId().equals(loader.networkNodeId()) || level.isOutsideBuildHeight(antennaPos)) {
            return false;
        }

        if (SableCompanion.INSTANCE.getContaining(level, pos) != SableCompanion.INSTANCE.getContaining(level, antennaPos)) {
            return false;
        }

        if (!level.isLoaded(antennaPos)) {
            return allowUnloadedAntenna;
        }

        if (!(level.getBlockEntity(antennaPos) instanceof AmethystAntennaBlockEntity antenna) || !node.antennaId().equals(antenna.identity()) || !antennaPos.relative(antenna.getBlockState().getValue(DirectionalBlock.FACING).getOpposite()).equals(pos)) {
            return false;
        }

        LoadingNetworkBinding binding = antenna.binding();
        if (binding == null) {
            return node.networkId() == null;
        }

        return Objects.equals(node.networkId(), binding.id());
    }

    private static boolean restore(ServerLevel level, Node node, LoadingNetworkSavedData data) {
        BlockPos pos = node.loader().pos();
        if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof ChunkStabilizerBlockEntity loader) || !node.loaderId().equals(loader.networkNodeId())) {
            return false;
        }

        SubLevelAccess host = SableCompanion.INSTANCE.getContaining(level, pos);
        if (matches(level, node, true)) {
            if (host == null) {
                data.register(node);
            }
            else {
                data.rememberPhysical(new PhysicalNode(host.getUniqueId(), node));
            }
        }
        loader.refreshLoading();
        return true;
    }

    private static void onTick(Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % AUDIT_INTERVAL != 0) {
            return;
        }

        LoadingNetworkSavedData data = LoadingNetworkSavedData.get(level);
        for (PhysicalNode member : data.physicalNodes()) {
            Node node = member.node();
            if (node.loader().dimension() != level.dimension()) {
                continue;
            }

            SubLevelAccess host = SableCompanion.INSTANCE.getContaining(level, node.loader().pos());
            if (host == null) {
                continue;
            }

            if (!host.getUniqueId().equals(member.host())) {
                data.forgetPhysical(node.loaderId());
                continue;
            }

            if (!level.isLoaded(node.loader().pos()) || !level.isLoaded(node.antenna().pos()) || matches(level, node, false)) {
                continue;
            }

            data.forgetPhysical(node.loaderId());
        }
    }

    public record Move(Node source, Node target) {}
}
