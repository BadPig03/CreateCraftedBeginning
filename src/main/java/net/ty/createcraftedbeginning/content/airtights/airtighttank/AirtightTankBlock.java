package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.MethodsReturnNonnullByDefault;
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
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IAirtightComponent;
import net.ty.createcraftedbeginning.content.airtights.gas.transport.GasConnectivityHandler;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightTankBlock extends Block implements IBE<AirtightTankBlockEntity>, IWrenchable, IAirtightComponent {
    static final BooleanProperty TOP = BooleanProperty.create("top");
    static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");

    public AirtightTankBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(TOP, true).setValue(BOTTOM, true));
    }

    public static void updateTankState(Level level, BlockPos tankPos) {
        if (level.isClientSide) {
            return;
        }

        if (!(level.getBlockState(tankPos).getBlock() instanceof AirtightTankBlock tankBlock)) {
            return;
        }

        AirtightTankBlockEntity tank = tankBlock.getBlockEntity(level, tankPos);
        if (tank == null) {
            return;
        }

        AirtightTankBlockEntity controller = tank.getControllerBE();
        if (controller == null) {
            return;
        }

        controller.updateTankState();
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        if (oldState.getBlock() == state.getBlock() || moved) {
            return;
        }

        withBlockEntityDo(level, pos, AirtightTankBlockEntity::updateConnectivity);
        BlockState updatedState = level.getBlockState(pos);
        if (state == updatedState || updatedState.getBlock() != this) {
            return;
        }

        level.markAndNotifyBlock(pos, level.getChunkAt(pos), oldState, updatedState, UPDATE_ALL_IMMEDIATE, 512);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.hasBlockEntity() || state.is(newState.getBlock())) {
            return;
        }

        if (!(level.getBlockEntity(pos) instanceof AirtightTankBlockEntity tank)) {
            return;
        }

        GasConnectivityHandler.splitMultiOnRemoval(tank);
        level.removeBlockEntity(pos);
    }

    @Override
    public VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(level, pos, state, entity, stack);
        CCBAdvancementBehaviour.setPlacedBy(level, pos, entity);
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(TOP, BOTTOM);
        super.createBlockStateDefinition(builder);
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
    public boolean canConnectOnFace(BlockPos currentPos, BlockState currentState, Direction localFace) {
        return true;
    }
}
