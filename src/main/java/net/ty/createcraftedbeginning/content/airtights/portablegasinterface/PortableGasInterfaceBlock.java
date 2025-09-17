package net.ty.createcraftedbeginning.content.airtights.portablegasinterface;

import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBShapes;
import org.jetbrains.annotations.NotNull;

public class PortableGasInterfaceBlock extends WrenchableDirectionalBlock implements IBE<PortableGasInterfaceBlockEntity> {
    public PortableGasInterfaceBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void neighborChanged(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull Block neighborBlock, @NotNull BlockPos neighborPos, boolean moving) {
        withBlockEntityDo(world, pos, PortableGasInterfaceBlockEntity::neighbourChanged);
    }

    @Override
    public boolean hasAnalogOutputSignal(@NotNull BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(@NotNull BlockState blockState, @NotNull Level worldIn, @NotNull BlockPos pos) {
        return getBlockEntityOptional(worldIn, pos).map(be -> be.isConnected() ? 15 : 0).orElse(0);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter worldIn, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return CCBShapes.PORTABLE_GAS_INTERFACE.get(state.getValue(FACING));
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos blockPos, @NotNull BlockState state, LivingEntity entity, @NotNull ItemStack itemStack) {
        super.setPlacedBy(level, blockPos, state, entity, itemStack);
    }

    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        Direction direction = context.getNearestLookingDirection();
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            direction = direction.getOpposite();
        }
        return defaultBlockState().setValue(FACING, direction.getOpposite());
    }

    @Override
    public Class<PortableGasInterfaceBlockEntity> getBlockEntityClass() {
        return PortableGasInterfaceBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PortableGasInterfaceBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.PORTABLE_GAS_INTERFACE.get();
    }
}
