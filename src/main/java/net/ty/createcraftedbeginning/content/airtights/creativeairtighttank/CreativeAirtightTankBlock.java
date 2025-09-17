package net.ty.createcraftedbeginning.content.airtights.creativeairtighttank;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.api.gas.GasConnectivityHandler;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;

public class CreativeAirtightTankBlock extends Block implements IBE<CreativeAirtightTankBlockEntity>, IWrenchable {
    public static final BooleanProperty TOP = BooleanProperty.create("top");
    public static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");

    public CreativeAirtightTankBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(TOP, true).setValue(BOTTOM, true));
    }

    @Override
    protected void createBlockStateDefinition(@NotNull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TOP, BOTTOM);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public void onPlace(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean moved) {
        if (oldState.getBlock() == state.getBlock() || moved) {
            return;
        }
        withBlockEntityDo(world, pos, CreativeAirtightTankBlockEntity::updateConnectivity);
    }

    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
        if (!state.hasBlockEntity() || (state.getBlock() == newState.getBlock() && newState.hasBlockEntity())) {
            return;
        }
        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof CreativeAirtightTankBlockEntity tank)) {
            return;
        }
        world.removeBlockEntity(pos);
        GasConnectivityHandler.splitMulti(tank);
    }

    @Override
    public @NotNull VoxelShape getBlockSupportShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        return Shapes.block();
    }

    @Override
    public Class<CreativeAirtightTankBlockEntity> getBlockEntityClass() {
        return CreativeAirtightTankBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends CreativeAirtightTankBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.CREATIVE_AIRTIGHT_TANK.get();
    }
}
