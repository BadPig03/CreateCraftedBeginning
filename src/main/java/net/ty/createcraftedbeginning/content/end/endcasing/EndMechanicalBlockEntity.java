package net.ty.createcraftedbeginning.content.end.endcasing;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class EndMechanicalBlockEntity<T extends EndMechanicalStructuralBlockEntity<?>> extends KineticBlockEntity {
    private T structural;

    protected EndMechanicalBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public abstract void addBehaviours(List<BlockEntityBehaviour> behaviours);

    @Override
    public void onLoad() {
        super.onLoad();
        if (level == null || level.isClientSide) {
            return;
        }

        structural = getStructural();
        if (structural == null && level.getBlockState(worldPosition.below()).getBlock() instanceof EndCasingBlock) {
            updateStructural();
        }
        level.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
    }

    protected abstract Class<T> getStructuralClass();

    protected abstract void updateStructural();

    protected @Nullable T getStructuralForUse() {
        if (structural != null && !structural.isRemoved()) {
            return structural;
        }

        structural = getStructural();
        return structural;
    }

    protected boolean convertCasingToStructural(BlockState structuralState) {
        if (level == null || level.isClientSide) {
            return false;
        }

        BlockPos structuralPos = worldPosition.below();
        if (!(level.getBlockState(structuralPos).getBlock() instanceof EndCasingBlock)) {
            return false;
        }

        if (!level.setBlockAndUpdate(structuralPos, structuralState)) {
            return false;
        }

        structural = getStructural();
        return true;
    }

    void verifyStructural() {
        if (level == null || level.isClientSide) {
            return;
        }

        structural = getStructural();
        if (structural != null) {
            return;
        }

        level.destroyBlock(worldPosition, true);
    }

    private @Nullable T getStructural() {
        if (level == null) {
            return null;
        }

        BlockEntity candidateStructural = level.getBlockEntity(worldPosition.below());
        Class<T> structuralClass = getStructuralClass();
        return structuralClass.isInstance(candidateStructural) ? structuralClass.cast(candidateStructural) : null;
    }
}
