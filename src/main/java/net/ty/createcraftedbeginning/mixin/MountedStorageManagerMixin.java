package net.ty.createcraftedbeginning.mixin;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.simibubi.create.api.contraption.storage.SyncedMountedStorage;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.MountedStorageManager;
import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.MountedGasStorage;
import net.ty.createcraftedbeginning.api.gas.MountedGasStorageType;
import net.ty.createcraftedbeginning.api.gas.MountedGasStorageWrapper;
import net.ty.createcraftedbeginning.api.gas.MountedStorageSyncPacketWithGas;
import net.ty.createcraftedbeginning.api.gas.interfaces.IMountedStorageManagerWithGas;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

@SuppressWarnings("LoggingSimilarMessage")
@Mixin(MountedStorageManager.class)
public abstract class MountedStorageManagerMixin implements IMountedStorageManagerWithGas {
    @Unique
    private Map<BlockPos, MountedGasStorage> gasesBuilder;
    @Unique
    private Map<BlockPos, SyncedMountedStorage> syncedGasesBuilder;
    @Unique
    private MountedGasStorageWrapper gases;
    @Unique
    private ImmutableMap<BlockPos, SyncedMountedStorage> syncedGases;
    @Shadow
    private int syncCooldown;

    @Unique
    private static <K, V> @NotNull ImmutableMap<K, V> subMap(@NotNull Map<K, V> map, Predicate<V> predicate) {
        Builder<K, V> builder = ImmutableMap.builder();
        map.forEach((key, value) -> {
            if (predicate.test(value)) {
                builder.put(key, value);
            }
        });
        return builder.build();
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        gases = null;
        gasesBuilder = new HashMap<>();
        syncedGasesBuilder = new HashMap<>();
    }

    @Inject(method = "initialize", at = @At("TAIL"))
    private void onInitialize(CallbackInfo ci) {
        ImmutableMap<BlockPos, MountedGasStorage> gases = ImmutableMap.copyOf(gasesBuilder);
        this.gases = new MountedGasStorageWrapper(gases);
        gasesBuilder = null;
        syncedGases = ImmutableMap.copyOf(syncedGasesBuilder);
        syncedGasesBuilder = null;
    }

    @Inject(method = "reset", at = @At("TAIL"))
    private void onReset(CallbackInfo ci) {
        gases = null;
        gasesBuilder = new HashMap<>();
        syncedGasesBuilder = new HashMap<>();
    }

    @Inject(method = "addBlock", at = @At("TAIL"))
    private void onAddBlock(Level level, @NotNull BlockState state, BlockPos globalPos, BlockPos localPos, BlockEntity be, CallbackInfo ci) {
        MountedGasStorageType<?> gasType = MountedGasStorageType.REGISTRY.get(state.getBlock());
        if (gasType == null) {
            return;
        }

        MountedGasStorage storage = gasType.mount(level, state, globalPos, be);
        if (storage == null) {
            return;
        }

        addStorage(storage, localPos);
    }

    @Unique
    private void addStorage(MountedGasStorage storage, BlockPos pos) {
        gasesBuilder.put(pos, storage);
        if (!(storage instanceof SyncedMountedStorage synced)) {
            return;
        }

        syncedGasesBuilder.put(pos, synced);
    }

    @Inject(method = "unmount", at = @At("TAIL"))
    private void onUnmount(Level level, @NotNull StructureBlockInfo info, BlockPos globalPos, BlockEntity be, CallbackInfo ci) {
        BlockPos localPos = info.pos();
        BlockState state = info.state();
        MountedGasStorage gasStorage = getGases().storages.get(localPos);
        if (gasStorage == null) {
            return;
        }

        MountedGasStorageType<?> expectedType = MountedGasStorageType.REGISTRY.get(state.getBlock());
        if (gasStorage.type != expectedType) {
            return;
        }

        gasStorage.unmount(level, state, globalPos, be);
    }

    @Unique
    public MountedGasStorageWrapper getGases() {
        assertInitialized();
        return gases;
    }

    @Unique
    public void setGases(MountedGasStorageWrapper gases) {
        this.gases = gases;
    }

    @Shadow
    abstract void assertInitialized();

    @Inject(method = "tick", at = @At("RETURN"))
    private void onTick(AbstractContraptionEntity entity, CallbackInfo ci) {
        if (syncCooldown > 0) {
            return;
        }

        Map<BlockPos, MountedGasStorage> gases = new HashMap<>();
        syncedGases.forEach((pos, storage) -> {
            if (storage.isDirty()) {
                gases.put(pos, (MountedGasStorage) storage);
                storage.markClean();
            }
        });

        if (gases.isEmpty()) {
            return;
        }

        MountedStorageSyncPacketWithGas packet = new MountedStorageSyncPacketWithGas(entity.getId(), new HashMap<>(), new HashMap<>(), gases);
        CatnipServices.NETWORK.sendToClientsTrackingEntity(entity, packet);
        syncCooldown = 8;
    }

    @Override
    @Unique
    public void handleSyncWithGas(MountedStorageSyncPacketWithGas packet, AbstractContraptionEntity entity) {
        MountedGasStorageWrapper gases = getGases();
        Map<SyncedMountedStorage, BlockPos> syncedStorages = new IdentityHashMap<>();

        try {
            if (gasesBuilder != null) {
                gasesBuilder.putAll(gases.storages);
            }
            packet.gases().forEach((pos, storage) -> {
                if (gasesBuilder != null) {
                    gasesBuilder.put(pos, storage);
                }
                syncedStorages.put((SyncedMountedStorage) storage, pos);
            });
        } catch (Throwable t) {
            CreateCraftedBeginning.LOGGER.error("An error occurred while syncing gas storage in MountedStorageManager", t);
        }

        Contraption contraption = entity.getContraption();
        syncedStorages.forEach((storage, pos) -> storage.afterSync(contraption, pos));
    }

    @Inject(method = "write", at = @At("RETURN"))
    private void onWrite(CompoundTag compoundTag, Provider registries, boolean clientPacket, CallbackInfo ci) {
        ListTag gases = new ListTag();
        if (getGases().storages != null) {
            getGases().storages.forEach((pos, storage) -> {
                if (!clientPacket || storage instanceof SyncedMountedStorage) {
                    MountedGasStorage.CODEC.encodeStart(NbtOps.INSTANCE, storage).resultOrPartial(err -> CreateCraftedBeginning.LOGGER.error("Failed to serialize mounted gas storage: {}", err)).ifPresent(encoded -> {
                        CompoundTag tag = new CompoundTag();
                        tag.put("pos", NbtUtils.writeBlockPos(pos));
                        tag.put("storage", encoded);
                        gases.add(tag);
                    });
                }
            });
        }

        if (!gases.isEmpty()) {
            compoundTag.put("gases", gases);
        }

        if (clientPacket && getGases().storages != null) {
            Set<BlockPos> positions = Sets.union(getGases().storages.keySet(), getGases().storages.keySet());

            ListTag list = new ListTag();
            for (BlockPos pos : positions) {
                CompoundTag tag = new CompoundTag();
                tag.putInt("X", pos.getX());
                tag.putInt("Y", pos.getY());
                tag.putInt("Z", pos.getZ());
                list.add(tag);
            }
            compoundTag.put("interactable_positions", list);
        }
    }

    @Inject(method = "read", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/contraptions/MountedStorageManager;initialize()V", shift = Shift.BEFORE))
    private void onReadBeforeInitialize(CompoundTag nbt, Provider registries, boolean clientPacket, Contraption contraption, CallbackInfo ci) {
        try {
            if (nbt.contains("gases")) {
                NBTHelper.iterateCompoundList(nbt.getList("gases", Tag.TAG_COMPOUND), tag -> {
                    BlockPos pos = NBTHelper.readBlockPos(tag, "pos");
                    CompoundTag data = tag.getCompound("storage");
                    MountedGasStorage.CODEC.decode(NbtOps.INSTANCE, data).resultOrPartial(err -> CreateCraftedBeginning.LOGGER.error("Failed to deserialize mounted gas storage: {}", err)).map(Pair::getFirst).ifPresent(storage -> addStorage(storage, pos));
                });
            }
        } catch (Throwable t) {
            CreateCraftedBeginning.LOGGER.error("Error deserializing mounted gas storage", t);
        }
    }

    @Inject(method = "read", at = @At("RETURN"))
    private void onReadAfter(CompoundTag nbt, Provider registries, boolean clientPacket, Contraption contraption, CallbackInfo ci) {
        if (!clientPacket || contraption == null) {
            return;
        }

        getGases().storages.forEach((pos, storage) -> {
            if (storage instanceof SyncedMountedStorage synced) {
                synced.afterSync(contraption, pos);
            }
        });
    }
}
