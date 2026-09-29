package net.ty.createcraftedbeginning.content.crates;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.function.Function;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CrateMountedStorageType<B extends CratesBlockEntity, S extends CrateMountedItemStorage<B>> extends MountedItemStorageType<S> {
    private final Class<B> blockEntityClass;
    private final Function<B, S> factory;

    public CrateMountedStorageType(MapCodec<S> codec, Class<B> blockEntityClass, Function<B, S> factory) {
        super(codec);
        this.blockEntityClass = Objects.requireNonNull(blockEntityClass, "Parameter 'blockEntityClass' must not be null.");
        this.factory = Objects.requireNonNull(factory, "Parameter 'factory' must not be null.");
    }

    @Override
    @Nullable
    public S mount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity blockEntity) {
        if (!blockEntityClass.isInstance(blockEntity)) {
            return null;
        }

        return factory.apply(blockEntityClass.cast(blockEntity));
    }
}
