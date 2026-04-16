package net.ty.createcraftedbeginning.content.end.endcasing;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public abstract class EndMechanicalBlockEntity<T extends EndMechanicalStructuralBlockEntity<?>> extends KineticBlockEntity {
    protected T structural;

    public EndMechanicalBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }

        verifyStructural();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level == null || level.isClientSide) {
            return;
        }

        updateStructural();
    }

    @SuppressWarnings("unchecked")
    protected @Nullable T getStructural() {
        if (level == null) {
            return null;
        }

        BlockEntity be = level.getBlockEntity(worldPosition.below());
        if (!(be instanceof EndMechanicalStructuralBlockEntity)) {
            return null;
        }

        return (T) be;
    }

    protected void verifyStructural() {
        if (level == null || level.isClientSide) {
            return;
        }

        if (structural == null || structural.isRemoved()) {
            structural = getStructural();
        }
        if (structural != null) {
            return;
        }

        level.destroyBlock(worldPosition, true);
    }

    public abstract void updateStructural();
}
