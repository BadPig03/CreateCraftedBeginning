package net.ty.createcraftedbeginning.mixin;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.simibubi.create.api.contraption.storage.SyncedMountedStorage;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.MountedStorageManager;
import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.interfaces.IMountedStorageManagerWithGas;
import net.ty.createcraftedbeginning.api.gas.MountedGasStorage;
import net.ty.createcraftedbeginning.api.gas.MountedGasStorageType;
import net.ty.createcraftedbeginning.api.gas.MountedGasStorageWrapper;
import net.ty.createcraftedbeginning.api.gas.MountedStorageSyncPacketWithGas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
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
    private static <K, V> ImmutableMap<K, V> subMap(Map<K, V> map, Predicate<V> predicate) {
        ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
        map.forEach((key, value) -> {
            if (predicate.test(value)) {
                builder.put(key, value);
            }
        });
        return builder.build();
    }

    @Shadow
    abstract void assertInitialized();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        this.gases = null;
        this.gasesBuilder = new HashMap<>();
        this.syncedGasesBuilder = new HashMap<>();
    }

    @Inject(method = "initialize", at = @At("TAIL"))
    private void onInitialize(CallbackInfo ci) {
        ImmutableMap<BlockPos, MountedGasStorage> gases = ImmutableMap.copyOf(this.gasesBuilder);
        this.gases = new MountedGasStorageWrapper(gases);
        this.gasesBuilder = null;

        this.syncedGases = ImmutableMap.copyOf(this.syncedGasesBuilder);
        this.syncedGasesBuilder = null;
    }

    @Inject(method = "reset", at = @At("TAIL"))
    private void onReset(CallbackInfo ci) {
        this.gases = null;
        this.gasesBuilder = new HashMap<>();
        this.syncedGasesBuilder = new HashMap<>();
    }

    @Inject(method = "addBlock", at = @At("TAIL"))
    private void onAddBlock(Level level, BlockState state, BlockPos globalPos, BlockPos localPos, BlockEntity be, CallbackInfo ci) {
        MountedGasStorageType<?> gasType = MountedGasStorageType.REGISTRY.get(state.getBlock());
        if (gasType != null) {
            MountedGasStorage storage = gasType.mount(level, state, globalPos, be);
            if (storage != null) {
                this.addStorage(storage, localPos);
            }
        }
    }

    @Inject(method = "unmount", at = @At("TAIL"))
    private void onUnmount(Level level, StructureTemplate.StructureBlockInfo info, BlockPos globalPos, BlockEntity be, CallbackInfo ci) {
        BlockPos localPos = info.pos();
        BlockState state = info.state();

        MountedGasStorage gasStorage = this.getGases().storages.get(localPos);
        if (gasStorage != null) {
            MountedGasStorageType<?> expectedType = MountedGasStorageType.REGISTRY.get(state.getBlock());
            if (gasStorage.type == expectedType) {
                gasStorage.unmount(level, state, globalPos, be);
            }
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void onTick(AbstractContraptionEntity entity, CallbackInfo ci) {
        if (this.syncCooldown > 0) {
            return;
        }

        Map<BlockPos, MountedGasStorage> gases = new HashMap<>();
        this.syncedGases.forEach((pos, storage) -> {
            if (storage.isDirty()) {
                gases.put(pos, (MountedGasStorage) storage);
                storage.markClean();
            }
        });

        if (!gases.isEmpty()) {
            MountedStorageSyncPacketWithGas packet = new MountedStorageSyncPacketWithGas(entity.getId(), new HashMap<>(), new HashMap<>(), gases);
            CatnipServices.NETWORK.sendToClientsTrackingEntity(entity, packet);
            this.syncCooldown = 8;
        }
    }

    @Unique
    public void handleSyncWithGas(MountedStorageSyncPacketWithGas packet, AbstractContraptionEntity entity) {
        MountedGasStorageWrapper gases = this.getGases();
        Map<SyncedMountedStorage, BlockPos> syncedStorages = new IdentityHashMap<>();

        try {
            if (this.gasesBuilder != null) {
                this.gasesBuilder.putAll(gases.storages);
            }
            packet.gases().forEach((pos, storage) -> {
                if (this.gasesBuilder != null) {
                    this.gasesBuilder.put(pos, storage);
                }
                syncedStorages.put((SyncedMountedStorage) storage, pos);
            });
        } catch (Throwable t) {
            CreateCraftedBeginning.LOGGER.error("An error occurred while syncing gas storage in MountedStorageManager", t);
        }

        Contraption contraption = entity.getContraption();
        syncedStorages.forEach((storage, pos) -> storage.afterSync(contraption, pos));
    }

    @SuppressWarnings("ExtractMethodRecommender")
    @Inject(method = "write", at = @At("RETURN"))
    private void onWrite(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        ListTag gases = new ListTag();
        if (this.getGases().storages != null) {
            this.getGases().storages.forEach((pos, storage) -> {
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
            nbt.put("gases", gases);
        }

        if (clientPacket && this.getGases().storages != null) {
            Set<BlockPos> positions = Sets.union(this.getGases().storages.keySet(), this.getGases().storages.keySet());

            ListTag list = new ListTag();
            for (BlockPos pos : positions) {
                CompoundTag tag = new CompoundTag();
                tag.putInt("X", pos.getX());
                tag.putInt("Y", pos.getY());
                tag.putInt("Z", pos.getZ());
                list.add(tag);
            }
            nbt.put("interactable_positions", list);
        }
    }

    @Inject(method = "read", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/contraptions/MountedStorageManager;initialize()V", shift = At.Shift.BEFORE))
    private void onReadBeforeInitialize(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket, Contraption contraption, CallbackInfo ci) {
        try {
            if (nbt.contains("gases")) {
                NBTHelper.iterateCompoundList(nbt.getList("gases", Tag.TAG_COMPOUND), tag -> {
                    BlockPos pos = NBTHelper.readBlockPos(tag, "pos");
                    CompoundTag data = tag.getCompound("storage");
                    MountedGasStorage.CODEC.decode(NbtOps.INSTANCE, data).resultOrPartial(err -> CreateCraftedBeginning.LOGGER.error("Failed to deserialize mounted gas storage: {}", err)).map(Pair::getFirst).ifPresent(storage -> this.addStorage(storage, pos));
                });
            }
        } catch (Throwable t) {
            CreateCraftedBeginning.LOGGER.error("Error deserializing mounted gas storage", t);
        }
    }

    @Inject(method = "read", at = @At("RETURN"))
    private void onReadAfter(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket, Contraption contraption, CallbackInfo ci) {
        if (clientPacket && contraption != null) {
            this.getGases().storages.forEach((pos, storage) -> {
                if (storage instanceof SyncedMountedStorage synced) {
                    synced.afterSync(contraption, pos);
                }
            });
        }
    }

    @Unique
    public MountedGasStorageWrapper getGases() {
        this.assertInitialized();
        return this.gases;
    }

    @Unique
    public void setGases(MountedGasStorageWrapper gases) {
        this.gases = gases;
    }

    @Unique
    private void addStorage(MountedGasStorage storage, BlockPos pos) {
        this.gasesBuilder.put(pos, storage);
        if (storage instanceof SyncedMountedStorage synced) {
            this.syncedGasesBuilder.put(pos, synced);
        }
    }
}
