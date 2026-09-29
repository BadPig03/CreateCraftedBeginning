package net.ty.createcraftedbeginning.gas.mounted;

import com.mojang.serialization.Codec;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class MountedGasStorage implements GasStorageHandler {
    public static final Codec<MountedGasStorage> CODEC = MountedGasStorageType.CODEC.dispatch(storage -> storage.type, type -> type.codec);
    static final StreamCodec<RegistryFriendlyByteBuf, MountedGasStorage> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public final MountedGasStorageType<? extends MountedGasStorage> type;

    protected MountedGasStorage(MountedGasStorageType<?> type) {
        this.type = Objects.requireNonNull(type, "Parameter 'type' must not be null.");
    }

    public abstract void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity blockEntity);
}
