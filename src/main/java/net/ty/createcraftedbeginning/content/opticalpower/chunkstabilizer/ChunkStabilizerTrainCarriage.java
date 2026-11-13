package net.ty.createcraftedbeginning.content.opticalpower.chunkstabilizer;

import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.mixin.common.accessor.CarriageAccessor;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.apache.commons.lang3.tuple.MutablePair;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record ChunkStabilizerTrainCarriage(Direction direction, List<BlockPos> loaders, Map<BlockPos, UUID> identities) {
    private static final String COMPOUND_KEY_CONTRAPTION = "Contraption";
    private static final String COMPOUND_KEY_TYPE = "Type";
    private static final int RETRY_DELAY_TICKS = 20;
    private static final Map<Carriage, Snapshot> SNAPSHOTS = new IdentityHashMap<>();
    private static final Map<Carriage, Integer> WORK_RETRIES = new IdentityHashMap<>();

    public static @Nullable ChunkStabilizerTrainCarriage readSnapshot(MinecraftServer server, UUID train, Carriage carriage, CompoundTag source) {
        Tag encoded = source.get(COMPOUND_KEY_CONTRAPTION);
        String type = encoded instanceof CompoundTag content ? content.getString(COMPOUND_KEY_TYPE) : "";
        Snapshot previous = SNAPSHOTS.get(carriage);
        int tick = server.getTickCount();
        if (previous != null && previous.source() == source && previous.encoded() == encoded && previous.type().equals(type)) {
            ChunkStabilizerTrainCarriage layout = previous.layout();
            if (layout != null || tick - previous.retryAt() < 0) {
                return layout;
            }
        }

        ChunkStabilizerTrainCarriage layout = null;
        RuntimeException failure = null;
        try {
            if (encoded instanceof CompoundTag content) {
                layout = read(server.overworld(), content);
            }
        }
        catch (RuntimeException exception) {
            failure = exception;
        }

        if (layout != null) {
            SNAPSHOTS.put(carriage, new Snapshot(source, encoded, type, layout, tick));
            return layout;
        }

        SNAPSHOTS.put(carriage, new Snapshot(source, encoded, type, null, tick + RETRY_DELAY_TICKS));
        if (previous != null && previous.layout() == null) {
            return null;
        }

        if (failure != null) {
            CCBAPI.LOGGER.warn("Failed to read the snapshot for carriage {} in train '{}'; retrying after {} ticks.", carriage.id, train, RETRY_DELAY_TICKS, failure);
            return null;
        }

        CCBAPI.LOGGER.warn("Failed to read the snapshot for carriage {} in train '{}'; expected nonempty native carriage geometry with a horizontal assembly direction. Retrying after {} ticks.", carriage.id, train, RETRY_DELAY_TICKS);
        return null;
    }

    public static boolean waitingToRetry(MinecraftServer server, Carriage carriage) {
        Integer retry = WORK_RETRIES.get(carriage);
        return retry != null && server.getTickCount() - retry < 0;
    }

    public static void suspendWork(MinecraftServer server, UUID train, Carriage carriage, String operation, RuntimeException failure) {
        int tick = server.getTickCount();
        Integer previous = WORK_RETRIES.get(carriage);
        if (previous != null && tick - previous < 0) {
            return;
        }

        WORK_RETRIES.put(carriage, tick + RETRY_DELAY_TICKS);
        if (previous != null) {
            return;
        }

        CCBAPI.LOGGER.warn("Failed to {} for carriage {} in train '{}'; retrying after {} ticks.", operation, carriage.id, train, RETRY_DELAY_TICKS, failure);
    }

    static @Nullable ChunkStabilizerTrainCarriage capture(MinecraftServer server, UUID train, Carriage carriage) {
        if (waitingToRetry(server, carriage)) {
            return null;
        }

        CarriageContraptionEntity entity = carriage.anyAvailableEntity();
        if (entity == null || !entity.isAlive() || !(entity.getContraption() instanceof CarriageContraption contraption)) {
            return readSnapshot(server, train, carriage, ((CarriageAccessor) carriage).ccb$getSerialisedEntity());
        }

        SNAPSHOTS.remove(carriage);
        if (contraption.disassembled) {
            return null;
        }

        try {
            Direction direction = contraption.getAssemblyDirection();
            if (direction == null || !direction.getAxis().isHorizontal()) {
                throw new IllegalStateException("Train carriage assembly direction must be horizontal; got " + direction + '.');
            }

            return create(contraption, direction);
        }
        catch (RuntimeException failure) {
            suspendWork(server, train, carriage, "capture the loader layout", failure);
            return null;
        }
    }

    static void completeWork(Carriage carriage) {
        WORK_RETRIES.remove(carriage);
    }

    static void retainCarriages(Set<Carriage> present) {
        SNAPSHOTS.keySet().retainAll(present);
        WORK_RETRIES.keySet().retainAll(present);
    }

    static void clear() {
        SNAPSHOTS.clear();
        WORK_RETRIES.clear();
    }

    private static @Nullable ChunkStabilizerTrainCarriage read(ServerLevel level, CompoundTag content) {
        if (!(Contraption.fromNBT(level, content, false) instanceof CarriageContraption contraption) || contraption.getBlocks().isEmpty()) {
            return null;
        }

        Direction direction = contraption.getAssemblyDirection();
        if (direction == null || !direction.getAxis().isHorizontal()) {
            return null;
        }

        return create(contraption, direction);
    }

    private static ChunkStabilizerTrainCarriage create(CarriageContraption contraption, Direction direction) {
        List<BlockPos> loaders = new ArrayList<>();
        Map<BlockPos, UUID> identities = new HashMap<>();
        for (MutablePair<StructureBlockInfo, MovementContext> actor : contraption.getActors()) {
            BlockPos localPos = actor.getLeft().pos();
            StructureBlockInfo info = contraption.getBlocks().get(localPos);
            if (info == null || !info.state().is(CCBBlocks.CHUNK_STABILIZER_BLOCK.get())) {
                continue;
            }

            loaders.add(localPos);
            UUID identity = ChunkStabilizerSerialization.readNetworkNodeId(info.nbt());
            if (identity == null) {
                continue;
            }

            identities.put(localPos, identity);
        }
        return new ChunkStabilizerTrainCarriage(direction, List.copyOf(loaders), Map.copyOf(identities));
    }

    private record Snapshot(CompoundTag source, @Nullable Tag encoded, String type, @Nullable ChunkStabilizerTrainCarriage layout, int retryAt) {}
}
