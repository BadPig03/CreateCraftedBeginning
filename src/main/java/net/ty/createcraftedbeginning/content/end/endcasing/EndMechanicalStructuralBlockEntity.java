package net.ty.createcraftedbeginning.content.end.endcasing;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.Nullable;

public abstract class EndMechanicalStructuralBlockEntity<T extends EndMechanicalBlockEntity<?>> extends KineticBlockEntity {
    protected T master;

    public EndMechanicalStructuralBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }

        if (master == null || master.isRemoved()) {
            master = getMaster();
        }
        if (master != null) {
            return;
        }

        level.setBlockAndUpdate(worldPosition, CCBBlocks.END_CASING_BLOCK.get().defaultBlockState());
    }

    @SuppressWarnings("unchecked")
    protected @Nullable T getMaster() {
        if (level == null) {
            return null;
        }

        BlockEntity be = level.getBlockEntity(worldPosition.above());
        if (!(be instanceof EndMechanicalBlockEntity)) {
            return null;
        }

        return (T) be;
    }
}
