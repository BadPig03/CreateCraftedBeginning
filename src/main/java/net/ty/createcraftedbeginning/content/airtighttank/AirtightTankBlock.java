package net.ty.createcraftedbeginning.content.airtighttank;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;

public class AirtightTankBlock extends Block implements IBE<AirtightTankBlockEntity>, IWrenchable {
    public static final BooleanProperty TOP = BooleanProperty.create("top");
    public static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");

    public AirtightTankBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(TOP, true).setValue(BOTTOM, true));
    }

    public static void updateTankState(Level level, BlockPos tankPos) {
        BlockState tankState = level.getBlockState(tankPos);
        if (!(tankState.getBlock() instanceof AirtightTankBlock tank)) {
            return;
        }
        AirtightTankBlockEntity tankBlockEntity = tank.getBlockEntity(level, tankPos);
        if (tankBlockEntity == null) {
            return;
        }
        AirtightTankBlockEntity controller = tankBlockEntity.getControllerBE();
        if (controller == null) {
            return;
        }
        controller.updateTankState();
    }

    public static boolean isTank(BlockState state) {
		return state.getBlock() instanceof AirtightTankBlock;
	}

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(TOP, BOTTOM);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public void onPlace(BlockState state, @NotNull Level world, @NotNull BlockPos pos, BlockState oldState, boolean moved) {
        if (oldState.getBlock() == state.getBlock() || moved) {
            return;
        }
        withBlockEntityDo(world, pos, AirtightTankBlockEntity::updateConnectivity);
    }

    @Override
    public void onRemove(BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
        if (!state.hasBlockEntity() || (state.getBlock() == newState.getBlock() && newState.hasBlockEntity())) {
            return;
        }
        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof AirtightTankBlockEntity tank)) {
            return;
        }
        world.removeBlockEntity(pos);
        ConnectivityHandler.splitMulti(tank);
    }

    @Override
    public @NotNull VoxelShape getBlockSupportShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        return Shapes.block();
    }

    @Override
    public Class<AirtightTankBlockEntity> getBlockEntityClass() {
        return AirtightTankBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AirtightTankBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.AIRTIGHT_TANK.get();
    }
}
