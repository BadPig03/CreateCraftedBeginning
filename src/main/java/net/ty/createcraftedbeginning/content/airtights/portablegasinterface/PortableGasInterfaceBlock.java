package net.ty.createcraftedbeginning.content.airtights.portablegasinterface;

import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import net.minecraft.MethodsReturnNonnullByDefault;
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
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.data.CCBShapes;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PortableGasInterfaceBlock extends WrenchableDirectionalBlock implements IBE<PortableGasInterfaceBlockEntity> {
    public PortableGasInterfaceBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean moving) {
        withBlockEntityDo(level, pos, PortableGasInterfaceBlockEntity::neighbourChanged);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState blockState, Level level, BlockPos pos) {
        return getBlockEntityOptional(level, pos).map(be -> be.isConnected() ? 15 : 0).orElse(0);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CCBShapes.PORTABLE_GAS_INTERFACE.get(state.getValue(FACING));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(level, pos, state, entity, stack);
        CCBAdvancementBehaviour.setPlacedBy(level, pos, entity);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }

        Direction direction = context.getNearestLookingDirection();
        return context.getPlayer() != null && context.getPlayer().isShiftKeyDown() ? state.setValue(FACING, direction) : state.setValue(FACING, direction.getOpposite());
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
