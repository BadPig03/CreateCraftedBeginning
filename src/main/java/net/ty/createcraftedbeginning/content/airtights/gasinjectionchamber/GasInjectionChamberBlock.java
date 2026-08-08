package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IAirtightComponent;
import net.ty.createcraftedbeginning.data.CCBShapes;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasInjectionChamberBlock extends HorizontalDirectionalBlock implements IBE<GasInjectionChamberBlockEntity>, IWrenchable, IAirtightComponent {
    public GasInjectionChamberBlock(Properties properties) {
        super(properties);
    }

    private static ItemInteractionResult installFilter(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, GasInjectionChamberBlockEntity chamber) {
        if (chamber.hasInstalledFilter()) {
            return ItemInteractionResult.FAIL;
        }

        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        if (!chamber.installFilter(stack)) {
            return ItemInteractionResult.FAIL;
        }

        stack.shrink(1);
        level.playSound(null, pos, state.getSoundType(level, pos, player).getPlaceSound(), SoundSource.BLOCKS, 0.75f, 1.1f);
        return ItemInteractionResult.SUCCESS;
    }

    private static ItemInteractionResult removeFilter(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, GasInjectionChamberBlockEntity chamber) {
        if (chamber.isFilterLocked()) {
            return ItemInteractionResult.FAIL;
        }

        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        ItemStack removed = chamber.removeInstalledFilter();
        if (removed.isEmpty()) {
            return ItemInteractionResult.SUCCESS;
        }

        player.setItemInHand(hand, removed);
        level.playSound(null, pos, state.getSoundType(level, pos, player).getBreakSound(), SoundSource.BLOCKS, 0.75f, 1.1f);
        return ItemInteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        return state.setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(level, pos, state, entity, stack);
        CCBAdvancementBehaviour.setPlacedBy(level, pos, entity);
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(FACING);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (hitResult.getDirection() != Direction.DOWN) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        GasInjectionChamberBlockEntity chamber = getBlockEntity(level, pos);
        if (chamber == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (GasInjectionChamberUtils.isFilter(stack)) {
            return installFilter(stack, state, level, pos, player, chamber);
        }

        if (!stack.isEmpty() || !chamber.hasInstalledFilter()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return removeFilter(state, level, pos, player, hand, chamber);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CCBShapes.GAS_INJECTION_CHAMBER_SHAPE;
    }

    @Override
    public Class<GasInjectionChamberBlockEntity> getBlockEntityClass() {
        return GasInjectionChamberBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends GasInjectionChamberBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.GAS_INJECTION_CHAMBER.get();
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(GasInjectionChamberBlock::new);
    }

    @Override
    public boolean isAirtight(BlockPos currentPos, BlockState currentState, Direction oppositeDirection) {
        return oppositeDirection == Direction.DOWN;
    }
}
