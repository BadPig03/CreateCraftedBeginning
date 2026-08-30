package net.ty.createcraftedbeginning.content.end.endcasing;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class EndMechanicalStructuralBlockEntity<T extends EndMechanicalBlockEntity<?>> extends KineticBlockEntity {
    private T master;

    protected EndMechanicalStructuralBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level == null || level.isClientSide) {
            return;
        }

        master = getMaster();
        level.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
    }

    protected abstract Class<T> getMasterClass();

    protected @Nullable T getMasterForUse() {
        if (master != null && !master.isRemoved()) {
            return master;
        }

        master = getMaster();
        return master;
    }

    void verifyMaster() {
        if (level == null || level.isClientSide) {
            return;
        }

        master = getMaster();
        if (master != null) {
            return;
        }

        level.setBlockAndUpdate(worldPosition, CCBBlocks.END_CASING_BLOCK.get().defaultBlockState());
    }

    private @Nullable T getMaster() {
        if (level == null) {
            return null;
        }

        BlockEntity candidateMaster = level.getBlockEntity(worldPosition.above());
        Class<T> masterClass = getMasterClass();
        if (!masterClass.isInstance(candidateMaster)) {
            return null;
        }
        return masterClass.cast(candidateMaster);
    }
}
