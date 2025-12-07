package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.api.gas.GasConnectivityHandler;
import net.ty.createcraftedbeginning.api.gas.interfaces.IAirtightComponent;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.NotNull;

public class AirtightTankBlock extends Block implements IBE<AirtightTankBlockEntity>, IWrenchable, IAirtightComponent {
    public static final BooleanProperty TOP = BooleanProperty.create("top");
    public static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");

    public AirtightTankBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(TOP, true).setValue(BOTTOM, true));
    }

    public static void updateTankState(@NotNull Level level, BlockPos tankPos) {
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

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isTank(@NotNull BlockState state) {
        return state.getBlock() instanceof AirtightTankBlock;
    }

    @Override
    protected void createBlockStateDefinition(@NotNull Builder<Block, BlockState> builder) {
        builder.add(TOP, BOTTOM);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public void onPlace(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean moved) {
        if (oldState.getBlock() == state.getBlock() || moved) {
            return;
        }

        withBlockEntityDo(level, pos, AirtightTankBlockEntity::updateConnectivity);
    }

    @Override
    public void setPlacedBy(@NotNull Level level, @NotNull BlockPos blockPos, @NotNull BlockState blockState, LivingEntity placer, @NotNull ItemStack itemStack) {
        super.setPlacedBy(level, blockPos, blockState, placer, itemStack);
        CCBAdvancementBehaviour.setPlacedBy(level, blockPos, placer);
    }

    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean isMoving) {
        if (!state.hasBlockEntity() || state.is(newState.getBlock())) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof AirtightTankBlockEntity tank)) {
            return;
        }

        level.removeBlockEntity(pos);
        GasConnectivityHandler.splitMulti(tank);
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

    @Override
    public boolean isAirtight(BlockPos currentPos, BlockState currentState, Direction oppositeDirection) {
        return true;
    }
}
