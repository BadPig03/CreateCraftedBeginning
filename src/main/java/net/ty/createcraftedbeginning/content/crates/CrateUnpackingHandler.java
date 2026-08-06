package net.ty.createcraftedbeginning.content.crates;

import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("UnstableApiUsage")
public final class CrateUnpackingHandler<B extends CratesBlockEntity> implements UnpackingHandler {
    private final Class<B> blockEntityClass;
    private final boolean discarding;

    private CrateUnpackingHandler(Class<B> blockEntityClass, boolean discarding) {
        this.blockEntityClass = Objects.requireNonNull(blockEntityClass);
        this.discarding = discarding;
    }

    public static <B extends CratesBlockEntity> CrateUnpackingHandler<B> standard(Class<B> blockEntityClass) {
        return new CrateUnpackingHandler<>(blockEntityClass, false);
    }

    public static <B extends CratesBlockEntity> CrateUnpackingHandler<B> discarding(Class<B> blockEntityClass) {
        return new CrateUnpackingHandler<>(blockEntityClass, true);
    }

    @Override
    public boolean unpack(Level level, BlockPos pos, BlockState state, Direction side, List<ItemStack> items, @Nullable PackageOrderWithCrafts orderContext, boolean simulate) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!blockEntityClass.isInstance(blockEntity)) {
            return false;
        }

        if (!discarding) {
            return CrateContainersUtils.defaultUnpack(level, pos, items, simulate);
        }

        B crate = blockEntityClass.cast(blockEntity);
        CrateItemStackHandler handler = crate.getHandler();
        return handler.runInBatch(() -> {
            for (ItemStack stack : items) {
                if (!handler.insertItem(0, stack, simulate).isEmpty()) {
                    return false;
                }
            }

            return true;
        });
    }
}
