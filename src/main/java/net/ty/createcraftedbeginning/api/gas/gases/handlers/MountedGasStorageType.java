package net.ty.createcraftedbeginning.api.gas.gases.handlers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.registry.SimpleRegistry;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.registry.CCBBuiltInRegistries;
import net.ty.createcraftedbeginning.registry.CCBRegistries;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class MountedGasStorageType<T extends MountedGasStorage> {
    public static final Codec<MountedGasStorageType<?>> CODEC = CCBBuiltInRegistries.MOUNTED_GAS_STORAGE_TYPE.byNameCodec();
    public static final SimpleRegistry<Block, MountedGasStorageType<?>> REGISTRY = SimpleRegistry.create();

    public final MapCodec<? extends T> codec;

    protected MountedGasStorageType(MapCodec<? extends T> codec) {
        this.codec = codec;
    }

    /**
     * Creates mounted storage for the supplied block entity or item.
     *
     * @param <B>  the value type constrained by {@code extends Block}
     * @param <P>  the value type used by this operation
     * @param type the type to use
     * @return the resulting non-null unary operator
     */
    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> mountedGasStorage(RegistryEntry<MountedGasStorageType<?>, ? extends MountedGasStorageType<?>> type) {
        return builder -> builder.onRegisterAfter(CCBRegistries.MOUNTED_GAS_STORAGE_TYPE, block -> REGISTRY.register(block, type.get()));
    }

    /**
     * Creates mounted storage for the supplied block entity or item.
     *
     * @param level the level in which the operation is performed
     * @param state the block state to inspect or process
     * @param pos   the target block position
     * @param be    the block entity that participates in the operation
     * @return the resulting value
     */
    @Nullable
    public abstract T mount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be);
}
