package net.ty.createcraftedbeginning.content.opticalpower.amethystloader.management;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.AmethystLoaderBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.AmethystLoaderNetwork;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.management.AmethystLoaderSnapshot.Member;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkBinding;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkRuntime.Status;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData.Node;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData.PhysicalNode;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystLoaderMembers {
    private AmethystLoaderMembers() {
    }

    static AmethystLoaderSnapshot create(AmethystLoaderBlockEntity loader, UUID player) {
        LoadingNetworkBinding binding = AmethystLoaderNetwork.binding(loader);
        if (binding != null && loader.getLevel() instanceof ServerLevel level) {
            return create(level, binding.id(), player);
        }

        return new AmethystLoaderSnapshot(null, "", false, false, LoadingNetworkSavedData.UNBOUND_COLOR, LoadingNetworkSavedData.UNBOUND_COLOR, false, 0, 0, List.of());
    }

    static AmethystLoaderSnapshot create(ServerLevel level, UUID network, UUID player) {
        LoadingNetworkSavedData data = LoadingNetworkSavedData.get(level);
        LoadingNetworkBinding binding = data.binding(network);
        List<Member> all = new ArrayList<>();
        for (Node node : data.members(network)) {
            all.add(new Member(node.loaderId(), node.loader(), node.enabled(), node.beamEnabled(), data.runtime().status(node), false, false, false, node.name()));
        }
        List<Node> assembled = data.assembledMembers(network);
        for (Node node : assembled) {
            all.add(assembled(data, node));
        }
        String name = binding == null ? "" : binding.name();
        return new AmethystLoaderSnapshot(network, name, data.owns(network, player), data.networkEnabled(network), data.color(network), data.defaultColor(network), data.customColor(network) != null, all.size(), assembled.size(), all);
    }

    static @Nullable Member find(ServerLevel level, UUID network, UUID identity) {
        LoadingNetworkSavedData data = LoadingNetworkSavedData.get(level);
        Node node = data.member(network, identity);
        if (node == null) {
            return null;
        }

        GlobalPos position = node.loader();
        if (node.equals(data.nodeAt(position))) {
            return new Member(identity, position, node.enabled(), node.beamEnabled(), data.runtime().status(node), false, false, false, node.name());
        }

        return assembled(data, node);
    }

    static @Nullable AmethystLoaderBlockEntity findLoaded(ServerLevel level, LoadingNetworkSavedData data, Member member) {
        GlobalPos position = member.position();
        BlockPos pos = position.pos();
        if (member.train() || member.moving() || position.dimension() != level.dimension() || !level.isLoaded(pos)) {
            return null;
        }

        UUID identity = member.identity();
        PhysicalNode physical = data.physicalNode(identity);
        UUID expectedHost = physical == null ? null : physical.host();
        if (!Objects.equals(expectedHost, SubLevelBridge.subLevelId(level, pos)) || !(level.getBlockEntity(pos) instanceof AmethystLoaderBlockEntity loader) || !identity.equals(loader.networkNodeId())) {
            return null;
        }

        return loader;
    }

    private static Member assembled(LoadingNetworkSavedData data, Node node) {
        UUID identity = node.loaderId();
        boolean train = data.trainNode(identity) != null;
        boolean moving = data.moving().contains(identity);
        boolean physical = data.physicalNode(identity) != null;
        Status status = train || moving || physical ? data.runtime().status(node) : Status.ASSEMBLED;
        return new Member(identity, node.loader(), node.enabled(), node.beamEnabled(), status, train, moving, physical, node.name());
    }
}
