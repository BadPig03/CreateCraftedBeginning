package net.ty.createcraftedbeginning.content.airtights.creativeairtighttank;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.GasConnectivityHandler;
import net.ty.createcraftedbeginning.api.gas.cansiters.GasCanisterQueryUtils;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.interfaces.IAirtightComponent;
import net.ty.createcraftedbeginning.api.gas.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

public class CreativeAirtightTankBlock extends Block implements IBE<CreativeAirtightTankBlockEntity>, IWrenchable, IAirtightComponent {
    public static final BooleanProperty TOP = BooleanProperty.create("top");
    public static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");

    public CreativeAirtightTankBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(TOP, true).setValue(BOTTOM, true));
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

        withBlockEntityDo(level, pos, CreativeAirtightTankBlockEntity::updateConnectivity);
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
        if (!(level.getBlockEntity(pos) instanceof CreativeAirtightTankBlockEntity tank)) {
            return;
        }

        level.removeBlockEntity(pos);
        GasConnectivityHandler.splitMulti(tank);
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hitResult) {
        if (player.isShiftKeyDown() || !stack.is(CCBItems.GAS_CANISTER)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!(level.getBlockEntity(pos) instanceof CreativeAirtightTankBlockEntity tankBlockEntity)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        CreativeAirtightTankBlockEntity controller = tankBlockEntity.getControllerBE();
        if (controller == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        IGasHandler gasHandler = level.getCapability(GasHandler.BLOCK, controller.getBlockPos(), null);
        if (gasHandler == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!(gasHandler instanceof CreativeSmartGasTank creativeTankHandler)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (level.isClientSide) {
            return ItemInteractionResult.sidedSuccess(true);
        }

        GasStack gasStack = GasCanisterQueryUtils.getCanisterContent(stack);
        if (gasStack.isEmpty()) {
            creativeTankHandler.setContainedGas(GasStack.EMPTY);
            return ItemInteractionResult.sidedSuccess(false);
        }

        creativeTankHandler.setContainedGas(gasStack.copy());
        return ItemInteractionResult.sidedSuccess(false);
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

    @Override
    public boolean isAirtight(BlockPos currentPos, BlockState currentState, Direction oppositeDirection) {
        return true;
    }
}
