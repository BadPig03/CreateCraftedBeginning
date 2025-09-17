package net.ty.createcraftedbeginning.api.gas;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.interfaces.IGasHandler;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@SuppressWarnings("unused")
public abstract class MountedGasStorage implements IGasHandler {
    public static final Codec<MountedGasStorage> CODEC = MountedGasStorageType.CODEC.dispatch(storage -> storage.type, type -> type.codec);

    @SuppressWarnings("deprecation")
    public static final StreamCodec<RegistryFriendlyByteBuf, MountedGasStorage> STREAM_CODEC = StreamCodec.of((b, t) -> b.writeWithCodec(RegistryOps.create(NbtOps.INSTANCE, b.registryAccess()), CODEC, t), b -> b.readWithCodecTrusted(RegistryOps.create(NbtOps.INSTANCE, b.registryAccess()), CODEC));

    public final MountedGasStorageType<? extends MountedGasStorage> type;

    protected MountedGasStorage(MountedGasStorageType<?> type) {
        this.type = Objects.requireNonNull(type);
    }

    public abstract void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be);
}
