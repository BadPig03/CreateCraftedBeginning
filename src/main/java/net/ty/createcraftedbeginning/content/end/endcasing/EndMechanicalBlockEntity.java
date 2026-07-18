package net.ty.createcraftedbeginning.content.end.endcasing;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class EndMechanicalBlockEntity<T extends EndMechanicalStructuralBlockEntity<?>> extends KineticBlockEntity {
    protected T structural;
    protected CCBAdvancementBehaviour advancementBehaviour;

    public EndMechanicalBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
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

    protected @Nullable T getStructural() {
        if (level == null) {
            return null;
        }

        BlockEntity blockEntity = level.getBlockEntity(worldPosition.below());
        Class<T> structuralClass = getStructuralClass();
        return structuralClass.isInstance(blockEntity) ? structuralClass.cast(blockEntity) : null;
    }

    public void verifyStructural() {
        if (level == null || level.isClientSide) {
            return;
        }

        structural = getStructural();
        if (structural != null) {
            return;
        }

        level.destroyBlock(worldPosition, true);
    }

    public abstract void updateStructural();
}
