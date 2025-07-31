package net.ty.createcraftedbeginning.content.andesitecrate;

import com.simibubi.create.content.logistics.crate.CrateBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;

public class AndesiteCrateBlock extends CrateBlock implements IBE<AndesiteCrateBlockEntity> {
    public static final int MAX_SLOT = 32;
    public static final int SLOT_LIMIT = 1;

    public AndesiteCrateBlock(BlockBehaviour.Properties p) {
        super(p);
    }

    @Override
    public Class<AndesiteCrateBlockEntity> getBlockEntityClass() {
        return AndesiteCrateBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AndesiteCrateBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.ANDESITE_CRATE.get();
    }

    @Override
    public void onRemove(BlockState state, @NotNull Level level, @NotNull BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.is(newState.getBlock())) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof AndesiteCrateBlockEntity crate)) {
            super.onRemove(state, level, pos, newState, isMoving);
            return;
        }

        for (int i = 0; i < MAX_SLOT; i++) {
            ItemStack stack = crate.inv.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }
}
