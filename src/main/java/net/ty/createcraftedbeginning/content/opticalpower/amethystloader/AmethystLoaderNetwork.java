package net.ty.createcraftedbeginning.content.opticalpower.amethystloader;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.opticalpower.amethystantenna.AmethystAntennaBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkBinding;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData.Node;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData.PhysicalNode;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.UUID;

import static net.minecraft.world.level.block.DirectionalBlock.FACING;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystLoaderNetwork {
    private AmethystLoaderNetwork() {
    }

    public static @Nullable LoadingNetworkBinding binding(AmethystLoaderBlockEntity loader) {
        if (!(loader.getLevel() instanceof ServerLevel level)) {
            AmethystAntennaBlockEntity antenna = antenna(loader);
            if (antenna == null) {
                return null;
            }

            return antenna.binding();
        }

        LoadingNetworkSavedData data = LoadingNetworkSavedData.get(level);
        BlockPos pos = loader.getBlockPos();
        UUID identity = loader.networkNodeId();
        GlobalPos position = GlobalPos.of(level.dimension(), pos);
        UUID host = SubLevelBridge.subLevelId(level, pos);
        Node node = data.nodeAt(position);
        if (host != null) {
            PhysicalNode physical = data.physicalNode(identity);
            node = physical != null && host.equals(physical.host()) ? physical.node() : null;
        }
        if (node == null || !node.loader().equals(position) || !node.loaderId().equals(identity) || node.networkId() == null) {
            return null;
        }

        return data.binding(node.networkId());
    }

    @Internal
    public static @Nullable AmethystAntennaBlockEntity antenna(AmethystLoaderBlockEntity loader) {
        Level level = loader.getLevel();
        if (level == null) {
            return null;
        }

        BlockPos position = loader.getBlockPos();
        UUID host = SubLevelBridge.subLevelId(level, position);
        for (Direction direction : Iterate.directions) {
            BlockPos adjacent = position.relative(direction);
            if (!level.isLoaded(adjacent) || !Objects.equals(host, SubLevelBridge.subLevelId(level, adjacent))) {
                continue;
            }

            if (level.getBlockEntity(adjacent) instanceof AmethystAntennaBlockEntity antenna && antenna.getBlockState().getValue(FACING) == direction) {
                return antenna;
            }
        }
        return null;
    }
}
