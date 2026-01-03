package net.ty.createcraftedbeginning.api.gas.gases;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.registry.SimpleRegistry;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
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

public abstract class MountedGasStorageType<T extends MountedGasStorage> {
    public static final Codec<MountedGasStorageType<?>> CODEC = CCBBuiltInRegistries.MOUNTED_GAS_STORAGE_TYPE.byNameCodec();
    public static final SimpleRegistry<Block, MountedGasStorageType<?>> REGISTRY = SimpleRegistry.create();

    public final MapCodec<? extends T> codec;

    protected MountedGasStorageType(MapCodec<? extends T> codec) {
        this.codec = codec;
    }

    @Contract(pure = true)
    public static <B extends Block, P> @NotNull NonNullUnaryOperator<BlockBuilder<B, P>> mountedGasStorage(RegistryEntry<MountedGasStorageType<?>, ? extends MountedGasStorageType<?>> type) {
        return builder -> builder.onRegisterAfter(CCBRegistries.MOUNTED_GAS_STORAGE_TYPE, block -> REGISTRY.register(block, type.get()));
    }

    @Nullable
    public abstract T mount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be);
}
