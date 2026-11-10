package net.ty.createcraftedbeginning.content.opticalpower.amethystloader.management;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.AmethystLoaderBlockEntity;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.AmethystLoaderLocation;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.AmethystLoaderTrainPositions;
import net.ty.createcraftedbeginning.content.opticalpower.amethystloader.management.AmethystLoaderSnapshot.Member;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkRuntime.Status;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData.PhysicalNode;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.Projection;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystLoaderMapTarget {
    private AmethystLoaderMapTarget() {
    }

    static @Nullable AmethystLoaderLocation resolve(MinecraftServer server, Member member) {
        LoadingNetworkSavedData data = LoadingNetworkSavedData.get(server.overworld());
        UUID identity = member.identity();
        if (member.train()) {
            return AmethystLoaderTrainPositions.resolve(server, identity);
        }

        if (member.moving()) {
            return data.moving().position(server, identity);
        }

        if (!member.physical()) {
            if (member.status() == Status.ASSEMBLED) {
                return null;
            }

            GlobalPos position = member.position();
            return new AmethystLoaderLocation(position.dimension(), position.pos().getCenter(), identity, 0);
        }

        PhysicalNode physical = data.physicalNode(identity);
        if (physical == null) {
            return null;
        }

        GlobalPos stored = physical.node().loader();
        BlockPos pos = stored.pos();
        UUID host = physical.host();
        ServerLevel level = server.getLevel(stored.dimension());
        if (level == null || !level.isLoaded(pos) || !host.equals(SubLevelBridge.subLevelId(level, pos)) || !(level.getBlockEntity(pos) instanceof AmethystLoaderBlockEntity loader) || !identity.equals(loader.networkNodeId())) {
            return null;
        }

        Projection projection = SubLevelBridge.resolve(level, pos);
        Vec3 position = projection.worldPosition();
        if (!projection.inSubLevel() || !Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)) {
            return null;
        }

        return new AmethystLoaderLocation(level.dimension(), position, host, 0);
    }
}
