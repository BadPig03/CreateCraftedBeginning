package net.ty.createcraftedbeginning.content.opticalpower.amethystantenna;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.ChunkStabilizerBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkBinding;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData.Node;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystAntennaAdvancements {
    private static final int REQUIRED_LOADERS = 2;

    private AmethystAntennaAdvancements() {
    }

    static void awardPlacement(AmethystAntennaBlockEntity antenna, ServerLevel level, @Nullable UUID owner, @Nullable LoadingNetworkBinding binding) {
        if (owner == null || binding == null || !(level.getPlayerByUUID(owner) instanceof ServerPlayer player) || player instanceof FakePlayer || CCBAdvancements.CLOUD_SERVICE_ONLINE.isAlreadyAwardedTo(player)) {
            return;
        }

        BlockPos antennaPos = antenna.getBlockPos();
        BlockPos loaderPos = antennaPos.relative(antenna.getBlockState().getValue(AmethystAntennaBlock.FACING).getOpposite());
        if (!level.isLoaded(loaderPos) || !(level.getBlockEntity(loaderPos) instanceof ChunkStabilizerBlockEntity loader)) {
            return;
        }

        LoadingNetworkSavedData data = LoadingNetworkSavedData.get(level);
        UUID network = binding.id();
        Node joined = data.member(network, loader.getOrCreateNetworkNodeId());
        if (!data.owns(network, owner) || joined == null || !joined.antennaId().equals(antenna.identity()) || !joined.antenna().equals(GlobalPos.of(level.dimension(), antennaPos))) {
            return;
        }

        Set<UUID> identities = new HashSet<>();
        for (Node member : data.members(network)) {
            identities.add(member.loaderId());
        }
        for (Node member : data.assembledMembers(network)) {
            identities.add(member.loaderId());
        }
        if (identities.size() < REQUIRED_LOADERS) {
            return;
        }

        CCBAdvancements.CLOUD_SERVICE_ONLINE.awardTo(player);
    }
}
