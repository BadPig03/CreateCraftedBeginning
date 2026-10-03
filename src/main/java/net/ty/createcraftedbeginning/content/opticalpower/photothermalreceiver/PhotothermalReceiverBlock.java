package net.ty.createcraftedbeginning.content.opticalpower.photothermalreceiver;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.foundation.block.CCBShapes;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class PhotothermalReceiverBlock extends Block implements IBE<PhotothermalReceiverBlockEntity>, IWrenchable {
    public PhotothermalReceiverBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.NONE));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (level.isClientSide) {
            return;
        }

        BlockPos heatedPos = pos.above();
        if (!level.isLoaded(heatedPos)) {
            return;
        }

        BlockEntity heatedBlockEntity = level.getBlockEntity(heatedPos);
        if (!(heatedBlockEntity instanceof BasinBlockEntity basin)) {
            return;
        }

        basin.notifyChangeOfContents();
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(BlazeBurnerBlock.HEAT_LEVEL);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CCBShapes.PHOTOTHERMAL_RECEIVER;
    }

    @Override
    public Class<PhotothermalReceiverBlockEntity> getBlockEntityClass() {
        return PhotothermalReceiverBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends PhotothermalReceiverBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.PHOTOTHERMAL_RECEIVER.get();
    }
}
