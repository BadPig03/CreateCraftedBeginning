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
import net.ty.createcraftedbeginning.content.opticalpower.loadingnetwork.LoadingNetworkSavedData.Node;
import net.ty.createcraftedbeginning.mixin.common.accessor.CarriageAccessor;

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

    void refresh(MinecraftServer server, LoadingNetworkSavedData data) {
        Map<UUID, Node> members = new HashMap<>();
        Set<Carriage> present = new HashSet<>();
        for (Train train : Create.RAILWAYS.trains.values()) {
            if (train.invalid) {
                continue;
            }

            for (Carriage carriage : train.carriages) {
                present.add(carriage);
                CarriageContraptionEntity entity = carriage.anyAvailableEntity();
                if (entity != null && entity.isAlive() && entity.getContraption() instanceof CarriageContraption contraption) {
                    if (contraption.disassembled) {
                        continue;
                    }

                    for (Node node : LoadingNetworkContraptions.members(contraption).values()) {
                        members.put(node.loaderId(), node);
                    }
                    continue;
                }

                CompoundTag source = ((CarriageAccessor) carriage).ccb$getSerialisedEntity();
                Layout layout = layouts.get(carriage);
                if (layout == null || layout.source() != source) {
                    layout = new Layout(source, LoadingNetworkTrainSerialization.read(source, server.overworld().holderLookup(Registries.BLOCK)));
                    layouts.put(carriage, layout);
                }
                for (Node node : layout.members()) {
                    members.put(node.loaderId(), node);
                }
            }
        }
        layouts.keySet().retainAll(present);
        data.updateTrainMembers(members);
    }

    private record Layout(CompoundTag source, List<Node> members) {}
}
