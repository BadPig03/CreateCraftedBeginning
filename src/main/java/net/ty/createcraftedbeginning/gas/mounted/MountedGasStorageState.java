package net.ty.createcraftedbeginning.gas.mounted;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import com.simibubi.create.api.contraption.storage.SyncedMountedStorage;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.ty.createcraftedbeginning.api.CCBAPI;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class MountedGasStorageState {
    private Map<BlockPos, MountedGasStorage> builder;
    private Map<BlockPos, SyncedMountedStorage> syncedBuilder;
    private MountedGasStorageWrapper gases;
    private ImmutableMap<BlockPos, SyncedMountedStorage> synced;
    private ImmutableMap<BlockPos, MountedGasStorage> beforeSync;

    public MountedGasStorageState() {
        reset();
    }

    public MountedGasStorageWrapper getStorage() {
        return Objects.requireNonNull(gases, "Mounted gas storage is not initialized.");
    }

    public void setStorage(MountedGasStorageWrapper storage) {
        gases = storage;
    }

    public void handleSync(MountedGasStorageSyncPacket packet, AbstractContraptionEntity entity) {
        Map<BlockPos, MountedGasStorage> gases = new HashMap<>(getStorage().storages);
        Map<SyncedMountedStorage, BlockPos> syncedStorages = new IdentityHashMap<>();
        try {
            packet.gases().forEach((pos, storage) -> {
                SyncedMountedStorage synced = (SyncedMountedStorage) storage;
                gases.put(pos, storage);
                syncedStorages.put(synced, pos);
            });
            replaceStorages(gases);
        }
        catch (Throwable failure) {
            CCBAPI.LOGGER.error("Failed to synchronize mounted gas storage.", failure);
            return;
        }

        Contraption contraption = entity.getContraption();
        syncedStorages.forEach((storage, pos) -> storage.afterSync(contraption, pos));
    }

    public void initialize() {
        if (builder == null) {
            return;
        }

        ImmutableMap<BlockPos, MountedGasStorage> gases = ImmutableMap.copyOf(builder);
        this.gases = new MountedGasStorageWrapper(gases);
        builder = null;
        synced = ImmutableMap.copyOf(syncedBuilder);
        syncedBuilder = null;
    }

    public void reset() {
        gases = null;
        builder = new HashMap<>();
        syncedBuilder = new HashMap<>();
    }

    public void captureBeforeSync() {
        beforeSync = getStorage().storages;
    }

    public void restoreAfterReset() {
        if (beforeSync == null) {
            return;
        }

        beforeSync.forEach((pos, storage) -> addStorage(storage, pos));
        beforeSync = null;
    }

    public void addBlock(Level level, BlockState state, BlockPos globalPos, BlockPos localPos, BlockEntity blockEntity) {
        MountedGasStorageType<?> gasType = MountedGasStorageType.REGISTRY.get(state.getBlock());
        if (gasType == null) {
            return;
        }

        MountedGasStorage storage = gasType.mount(level, state, globalPos, blockEntity);
        if (storage == null) {
            return;
        }

        addStorage(storage, localPos);
    }

    public void unmount(Level level, StructureBlockInfo info, BlockPos globalPos, BlockEntity blockEntity) {
        BlockPos localPos = info.pos();
        BlockState state = info.state();
        MountedGasStorage gasStorage = getStorage().storages.get(localPos);
        if (gasStorage == null) {
            return;
        }

        MountedGasStorageType<?> expectedType = MountedGasStorageType.REGISTRY.get(state.getBlock());
        if (gasStorage.type != expectedType) {
            return;
        }

        gasStorage.unmount(level, state, globalPos, blockEntity);
    }

    public boolean tick(AbstractContraptionEntity entity) {
        Map<BlockPos, MountedGasStorage> gases = new HashMap<>();
        synced.forEach((pos, storage) -> {
            if (!storage.isDirty()) {
                return;
            }

            gases.put(pos, (MountedGasStorage) storage);
            storage.markClean();
        });
        if (gases.isEmpty()) {
            return false;
        }

        MountedGasStorageSyncPacket packet = new MountedGasStorageSyncPacket(entity.getId(), new HashMap<>(), new HashMap<>(), gases);
        CatnipServices.NETWORK.sendToClientsTrackingEntity(entity, packet);
        return true;
    }

    public void write(CompoundTag compoundTag, boolean clientPacket) {
        ListTag gases = new ListTag();
        ImmutableMap<BlockPos, MountedGasStorage> storages = getStorage().storages;
        storages.forEach((pos, storage) -> {
            if (clientPacket && !(storage instanceof SyncedMountedStorage)) {
                return;
            }

            MountedGasStorage.CODEC.encodeStart(NbtOps.INSTANCE, storage).resultOrPartial(err -> CCBAPI.LOGGER.error("Failed to serialize mounted gas storage: {}", err)).ifPresent(encoded -> {
                CompoundTag tag = new CompoundTag();
                tag.put("pos", NbtUtils.writeBlockPos(pos));
                tag.put("storage", encoded);
                gases.add(tag);
            });
        });
        if (!gases.isEmpty()) {
            compoundTag.put("gases", gases);
        }
        if (!clientPacket) {
            return;
        }

        ListTag list = compoundTag.getList("interactable_positions", Tag.TAG_COMPOUND);
        Set<BlockPos> positions = new HashSet<>();
        NBTHelper.iterateCompoundList(list, tag -> positions.add(new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z"))));
        for (BlockPos pos : storages.keySet()) {
            if (!positions.add(pos)) {
                continue;
            }

            CompoundTag tag = new CompoundTag();
            tag.putInt("X", pos.getX());
            tag.putInt("Y", pos.getY());
            tag.putInt("Z", pos.getZ());
            list.add(tag);
        }
        compoundTag.put("interactable_positions", list);
    }

    public void read(CompoundTag nbt) {
        try {
            if (!nbt.contains("gases")) {
                return;
            }

            NBTHelper.iterateCompoundList(nbt.getList("gases", Tag.TAG_COMPOUND), tag -> {
                BlockPos pos = NBTHelper.readBlockPos(tag, "pos");
                CompoundTag data = tag.getCompound("storage");
                MountedGasStorage.CODEC.decode(NbtOps.INSTANCE, data).resultOrPartial(err -> CCBAPI.LOGGER.error("Failed to deserialize mounted gas storage: {}", err)).map(Pair::getFirst).ifPresent(storage -> addStorage(storage, pos));
            });
        }
        catch (Throwable failure) {
            CCBAPI.LOGGER.error("Failed to deserialize mounted gas storage.", failure);
        }
    }

    public void afterSync(Contraption contraption) {
        getStorage().storages.forEach((pos, storage) -> {
            if (!(storage instanceof SyncedMountedStorage syncedMountedStorage)) {
                return;
            }

            syncedMountedStorage.afterSync(contraption, pos);
        });
    }

    private void addStorage(MountedGasStorage storage, BlockPos pos) {
        builder.put(pos, storage);
        if (!(storage instanceof SyncedMountedStorage syncedMountedStorage)) {
            return;
        }

        syncedBuilder.put(pos, syncedMountedStorage);
    }

    private void replaceStorages(Map<BlockPos, MountedGasStorage> storages) {
        ImmutableMap<BlockPos, MountedGasStorage> gases = ImmutableMap.copyOf(storages);
        Map<BlockPos, SyncedMountedStorage> syncedGases = new HashMap<>();
        gases.forEach((pos, storage) -> {
            if (!(storage instanceof SyncedMountedStorage syncedMountedStorage)) {
                return;
            }

            syncedGases.put(pos, syncedMountedStorage);
        });

        this.gases = new MountedGasStorageWrapper(gases);
        synced = ImmutableMap.copyOf(syncedGases);
    }
}
