package net.ty.createcraftedbeginning.mixin.server.create;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.simibubi.create.api.contraption.storage.SyncedMountedStorage;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.MountedStorageManager;
import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.MethodsReturnNonnullByDefault;
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
import net.ty.createcraftedbeginning.api.gas.gases.handlers.MountedGasStorage;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.MountedGasStorageType;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.MountedGasStorageWrapper;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IMountedStorageManagerWithGas;
import net.ty.createcraftedbeginning.api.gas.gases.packets.MountedStorageSyncWithGasPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("LoggingSimilarMessage")
@Mixin(value = MountedStorageManager.class, remap = false)
public abstract class MountedStorageManagerMixin implements IMountedStorageManagerWithGas {
    @Unique
    private Map<BlockPos, MountedGasStorage> ccb$gasesBuilder;
    @Unique
    private Map<BlockPos, SyncedMountedStorage> ccb$syncedGasesBuilder;
    @Unique
    private MountedGasStorageWrapper ccb$gases;
    @Unique
    private ImmutableMap<BlockPos, SyncedMountedStorage> ccb$syncedGases;

    @Override
    @Unique
    public MountedGasStorageWrapper ccb$getGases() {
        assertInitialized();
        return ccb$gases;
    }

    @Override
    @Unique
    public void ccb$setGases(MountedGasStorageWrapper gases) {
        ccb$gases = gases;
    }

    @Unique
    private void ccb$addStorage(MountedGasStorage storage, BlockPos pos) {
        ccb$gasesBuilder.put(pos, storage);
        if (!(storage instanceof SyncedMountedStorage synced)) {
            return;
        }

        ccb$syncedGasesBuilder.put(pos, synced);
    }

    @Override
    @Unique
    public void ccb$handleSyncWithGas(MountedStorageSyncWithGasPacket packet, AbstractContraptionEntity entity) {
        MountedGasStorageWrapper gases = ccb$getGases();
        Map<SyncedMountedStorage, BlockPos> syncedStorages = new IdentityHashMap<>();
        try {
            if (ccb$gasesBuilder != null) {
                ccb$gasesBuilder.putAll(gases.storages);
            }
            packet.gases().forEach((pos, storage) -> {
                if (ccb$gasesBuilder != null) {
                    ccb$gasesBuilder.put(pos, storage);
                }
                syncedStorages.put((SyncedMountedStorage) storage, pos);
            });
        } catch (Throwable t) {
            CreateCraftedBeginning.LOGGER.error("An error occurred while syncing gas storage in MountedStorageManager", t);
        }

        Contraption contraption = entity.getContraption();
        syncedStorages.forEach((storage, pos) -> storage.afterSync(contraption, pos));
    }

    @Shadow
    private int syncCooldown;

    @Shadow
    protected abstract void assertInitialized();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void ccb$init(CallbackInfo ci) {
        ccb$gases = null;
        ccb$gasesBuilder = new HashMap<>();
        ccb$syncedGasesBuilder = new HashMap<>();
    }

    @Inject(method = "initialize", at = @At("TAIL"))
    private void ccb$initialize(CallbackInfo ci) {
        ImmutableMap<BlockPos, MountedGasStorage> gases = ImmutableMap.copyOf(ccb$gasesBuilder);
        ccb$gases = new MountedGasStorageWrapper(gases);
        ccb$gasesBuilder = null;
        ccb$syncedGases = ImmutableMap.copyOf(ccb$syncedGasesBuilder);
        ccb$syncedGasesBuilder = null;
    }

    @Inject(method = "reset", at = @At("TAIL"))
    private void ccb$reset(CallbackInfo ci) {
        ccb$gases = null;
        ccb$gasesBuilder = new HashMap<>();
        ccb$syncedGasesBuilder = new HashMap<>();
    }

    @Inject(method = "addBlock", at = @At("TAIL"))
    private void ccb$addBlock(Level level, BlockState state, BlockPos globalPos, BlockPos localPos, BlockEntity be, CallbackInfo ci) {
        MountedGasStorageType<?> gasType = MountedGasStorageType.REGISTRY.get(state.getBlock());
        if (gasType == null) {
            return;
        }

        MountedGasStorage storage = gasType.mount(level, state, globalPos, be);
        if (storage == null) {
            return;
        }

        ccb$addStorage(storage, localPos);
    }

    @Inject(method = "unmount", at = @At("TAIL"))
    private void ccb$unmount(Level level, StructureBlockInfo info, BlockPos globalPos, BlockEntity be, CallbackInfo ci) {
        BlockPos localPos = info.pos();
        BlockState state = info.state();
        MountedGasStorage gasStorage = ccb$getGases().storages.get(localPos);
        if (gasStorage == null) {
            return;
        }

        MountedGasStorageType<?> expectedType = MountedGasStorageType.REGISTRY.get(state.getBlock());
        if (gasStorage.type != expectedType) {
            return;
        }

        gasStorage.unmount(level, state, globalPos, be);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void ccb$tick(AbstractContraptionEntity entity, CallbackInfo ci) {
        if (syncCooldown > 0) {
            return;
        }

        Map<BlockPos, MountedGasStorage> gases = new HashMap<>();
        ccb$syncedGases.forEach((pos, storage) -> {
            if (storage.isDirty()) {
                gases.put(pos, (MountedGasStorage) storage);
                storage.markClean();
            }
        });

        if (gases.isEmpty()) {
            return;
        }

        MountedStorageSyncWithGasPacket packet = new MountedStorageSyncWithGasPacket(entity.getId(), new HashMap<>(), new HashMap<>(), gases);
        CatnipServices.NETWORK.sendToClientsTrackingEntity(entity, packet);
        syncCooldown = 8;
    }

    @Inject(method = "write", at = @At("RETURN"))
    private void ccb$write(CompoundTag compoundTag, Provider provider, boolean clientPacket, CallbackInfo ci) {
        ListTag gases = new ListTag();
        if (ccb$getGases().storages != null) {
            ccb$getGases().storages.forEach((pos, storage) -> {
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
        if (!clientPacket || ccb$getGases().storages == null) {
            return;
        }

        Set<BlockPos> positions = Sets.union(ccb$getGases().storages.keySet(), ccb$getGases().storages.keySet());
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

    @Inject(method = "read", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/contraptions/MountedStorageManager;initialize()V", shift = Shift.BEFORE))
    private void ccb$readOnInitialization(CompoundTag nbt, Provider provider, boolean clientPacket, Contraption contraption, CallbackInfo ci) {
        try {
            if (nbt.contains("gases")) {
                NBTHelper.iterateCompoundList(nbt.getList("gases", Tag.TAG_COMPOUND), tag -> {
                    BlockPos pos = NBTHelper.readBlockPos(tag, "pos");
                    CompoundTag data = tag.getCompound("storage");
                    MountedGasStorage.CODEC.decode(NbtOps.INSTANCE, data).resultOrPartial(err -> CreateCraftedBeginning.LOGGER.error("Failed to deserialize mounted gas storage: {}", err)).map(Pair::getFirst).ifPresent(storage -> ccb$addStorage(storage, pos));
                });
            }
        } catch (Throwable t) {
            CreateCraftedBeginning.LOGGER.error("Error deserializing mounted gas storage", t);
        }
    }

    @Inject(method = "read", at = @At("RETURN"))
    private void ccb$readAtReturn(CompoundTag nbt, Provider registries, boolean clientPacket, Contraption contraption, CallbackInfo ci) {
        if (!clientPacket) {
            return;
        }

        ccb$getGases().storages.forEach((pos, storage) -> {
            if (storage instanceof SyncedMountedStorage synced) {
                synced.afterSync(contraption, pos);
            }
        });
    }
}
