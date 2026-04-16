package net.ty.createcraftedbeginning.content.end.endsculksilencer;

import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.content.end.endcasing.EndMechanicalBlock;
import net.ty.createcraftedbeginning.data.CCBShapes;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;

public class EndSculkSilencerBlock extends EndMechanicalBlock implements IBE<EndSculkSilencerBlockEntity> {
    public EndSculkSilencerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState blockState, LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, blockState, placer, stack);
        withBlockEntityDo(level, pos, EndSculkSilencerBlockEntity::updateStructural);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos blockPos, @NotNull CollisionContext collisionContext) {
        return CCBShapes.END_SCULK_SILENCER_SHAPE;
    }

    @Override
    protected @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return CCBShapes.END_SCULK_SILENCER_COLLISION_SHAPE;
    }

    @Override
    public Class<EndSculkSilencerBlockEntity> getBlockEntityClass() {
        return EndSculkSilencerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends EndSculkSilencerBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.END_SCULK_SILENCER.get();
    }
}
