package net.ty.createcraftedbeginning.api.gas.gases.handlers;

import com.mojang.serialization.Codec;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class MountedGasStorage implements IGasHandler {
    public static final Codec<MountedGasStorage> CODEC = MountedGasStorageType.CODEC.dispatch(storage -> storage.type, type -> type.codec);

    @SuppressWarnings("deprecation")
    public static final StreamCodec<RegistryFriendlyByteBuf, MountedGasStorage> STREAM_CODEC = StreamCodec.of((buffer, storage) -> buffer.writeWithCodec(RegistryOps.create(NbtOps.INSTANCE, buffer.registryAccess()), CODEC, storage), buffer -> buffer.readWithCodecTrusted(RegistryOps.create(NbtOps.INSTANCE, buffer.registryAccess()), CODEC));

    public final MountedGasStorageType<? extends MountedGasStorage> type;

    protected MountedGasStorage(MountedGasStorageType<?> type) {
        this.type = Objects.requireNonNull(type);
    }

    /**
     * Restores the contents of this mounted storage to its target.
     *
     * @param level the level in which the operation is performed
     * @param state the block state to inspect or process
     * @param pos   the target block position
     * @param be    the block entity that participates in the operation
     */
    public abstract void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be);
}
