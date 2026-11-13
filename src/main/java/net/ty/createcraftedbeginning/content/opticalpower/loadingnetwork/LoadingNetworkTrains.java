package net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork;

import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer.ChunkStabilizerTrainCarriage;
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData.Node;
import net.ty.createcraftedbeginning.mixin.common.accessor.CarriageAccessor;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class LoadingNetworkTrains {
    private final Map<Carriage, Layout> layouts = new IdentityHashMap<>();

    private static @Nullable Layout readMembers(MinecraftServer server, UUID train, Carriage carriage, @Nullable Layout previous) {
        if (ChunkStabilizerTrainCarriage.waitingToRetry(server, carriage)) {
            return null;
        }

        CarriageContraptionEntity entity = carriage.anyAvailableEntity();
        if (entity != null && entity.isAlive() && entity.getContraption() instanceof CarriageContraption contraption) {
            if (contraption.disassembled) {
                return new Layout(null, List.of());
            }

            return new Layout(null, List.copyOf(LoadingNetworkContraptions.members(contraption).values()));
        }

        CompoundTag source = ((CarriageAccessor) carriage).ccb$getSerialisedEntity();
        ChunkStabilizerTrainCarriage geometry = ChunkStabilizerTrainCarriage.readSnapshot(server, train, carriage, source);
        if (geometry == null) {
            return null;
        }

        if (previous != null && previous.geometry() == geometry) {
            return previous;
        }

        return new Layout(geometry, LoadingNetworkTrainSerialization.read(source, server.overworld().holderLookup(Registries.BLOCK)));
    }

    void refresh(MinecraftServer server, LoadingNetworkSavedData data) {
        Map<UUID, Node> members = new HashMap<>();
        Set<Carriage> present = new HashSet<>();
        boolean unknownMembership = false;
        for (Train train : Create.RAILWAYS.trains.values()) {
            if (train.invalid) {
                continue;
            }

            for (Carriage carriage : train.carriages) {
                present.add(carriage);
                Layout previous = layouts.get(carriage);
                Layout layout = null;
                try {
                    layout = readMembers(server, train.id, carriage, previous);
                }
                catch (RuntimeException failure) {
                    ChunkStabilizerTrainCarriage.suspendWork(server, train.id, carriage, "read the loader membership", failure);
                }
                if (layout != null) {
                    layouts.put(carriage, layout);
                }
                else {
                    layout = previous;
                }
                if (layout == null) {
                    unknownMembership = true;
                    continue;
                }

                for (Node node : layout.members()) {
                    members.put(node.loaderId(), node);
                }
            }
        }
        layouts.keySet().retainAll(present);
        if (unknownMembership) {
            for (Node node : data.trainMembers()) {
                members.putIfAbsent(node.loaderId(), node);
            }
        }
        data.updateTrainMembers(members);
    }

    private record Layout(@Nullable ChunkStabilizerTrainCarriage geometry, List<Node> members) {}
}
