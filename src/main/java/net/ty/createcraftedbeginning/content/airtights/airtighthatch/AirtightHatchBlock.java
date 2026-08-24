package net.ty.createcraftedbeginning.content.airtights.airtighthatch;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.StackRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.StrictNbtStackRequirement;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.createmod.catnip.lang.Lang;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams.Builder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags.Items;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.ty.createcraftedbeginning.advancement.CCBAdvancementBehaviour;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gascanisters.AirtightHatchCanisters;
import net.ty.createcraftedbeginning.content.airtights.gas.interfaces.IAirtightComponent;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterUtils;
import net.ty.createcraftedbeginning.foundation.block.CCBShapes;
import net.ty.createcraftedbeginning.registry.CCBBlockEntities;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightHatchBlock extends HorizontalDirectionalBlock implements IBE<AirtightHatchBlockEntity>, ProperWaterloggedBlock, SpecialBlockItemRequirement, IWrenchable {
    public static final EnumProperty<CanisterType> CANISTER_TYPE = EnumProperty.create("canister_type", CanisterType.class);
    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public AirtightHatchBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false).setValue(CANISTER_TYPE, CanisterType.EMPTY));
    }

    static boolean hasValidAttachment(LevelReader level, BlockPos hatchPos, BlockState hatchState) {
        Direction facing = hatchState.getValue(FACING);
        BlockPos targetPos = hatchPos.relative(facing);
        BlockState targetState = level.getBlockState(targetPos);
        Direction targetFace = facing.getOpposite();
        return canSupportCenter(level, targetPos, targetFace) && targetState.getBlock() instanceof IAirtightComponent component && component.canConnectOnFace(targetPos, targetState, targetFace);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(AirtightHatchBlock::new);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState placementState = super.getStateForPlacement(context);
        if (placementState == null) {
            return null;
        }

        Direction clickedFace = context.getClickedFace();
        if (clickedFace.getAxis() == Axis.Y) {
            return null;
        }

        placementState = placementState.setValue(FACING, clickedFace.getOpposite()).setValue(CANISTER_TYPE, CanisterType.EMPTY);
        return ProperWaterloggedBlock.withWater(context.getLevel(), placementState, context.getClickedPos());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(level, pos, state, entity, stack);
        CCBAdvancementBehaviour.setPlacedBy(level, pos, entity);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED, CANISTER_TYPE);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (!(level instanceof ServerLevel serverLevel) || player == null) {
            return InteractionResult.SUCCESS;
        }

        BreakEvent breakEvent = new BreakEvent(level, pos, state, player);
        NeoForge.EVENT_BUS.post(breakEvent);
        if (breakEvent.isCanceled()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!player.isCreative()) {
            getDrops(state, serverLevel, pos, blockEntity, player, context.getItemInHand()).forEach(dropStack -> ItemHandlerHelper.giveItemToPlayer(player, dropStack));
        }
        else if (blockEntity instanceof AirtightHatchBlockEntity hatch && !hatch.isEmpty() && !hatch.giveCanisterToPlayer(player)) {
            return InteractionResult.SUCCESS;
        }
        state.spawnAfterBreak(serverLevel, pos, ItemStack.EMPTY, true);
        level.destroyBlock(pos, false);
        IWrenchable.playRemoveSound(level, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (!state.getValue(WATERLOGGED)) {
            return state;
        }

        level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return state;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block otherBlock, BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, otherBlock, neighborPos, isMoving);
        if (canSurvive(state, level, pos)) {
            return;
        }

        level.destroyBlock(pos, true);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        boolean isWrench = stack.is(Items.TOOLS_WRENCH);
        if (player.isShiftKeyDown() && isWrench) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!(level.getBlockEntity(pos) instanceof AirtightHatchBlockEntity hatch)) {
            return ItemInteractionResult.FAIL;
        }

        if (!hatch.isEmpty()) {
            if (!isWrench) {
                if (!stack.isEmpty()) {
                    GasCanisterUtils.displayCustomWarningHint(player, "gui.warnings.invalid_item", stack.getHoverName());
                }
                return ItemInteractionResult.FAIL;
            }

            if (!hatch.giveCanisterToPlayer(player)) {
                return ItemInteractionResult.FAIL;
            }

            CCBSoundEvents.CANISTER_REMOVED.playOnServer(player.level(), pos, 1, 1);
            return ItemInteractionResult.SUCCESS;
        }

        if (!AirtightHatchCanisters.isCompatible(stack)) {
            if (isWrench) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }

            if (!stack.isEmpty()) {
                GasCanisterUtils.displayCustomWarningHint(player, "gui.warnings.invalid_item", stack.getHoverName());
            }
            return ItemInteractionResult.FAIL;
        }

        Direction facing = state.getValue(FACING);
        if (level.getCapability(GasHandler.BLOCK, pos.relative(facing), facing.getOpposite()) == null || !hasValidAttachment(level, pos, state)) {
            GasCanisterUtils.displayCustomWarningHint(player, "gui.warnings.invalid_face");
            return ItemInteractionResult.FAIL;
        }

        if (!hatch.installCanister(stack)) {
            return ItemInteractionResult.FAIL;
        }

        CCBSoundEvents.CANISTER_ADDED.playOnServer(player.level(), pos, 1, 1);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);
        if (!(builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof AirtightHatchBlockEntity hatch)) {
            return drops;
        }

        ItemStack canisterStack = hatch.createCanisterItemStack();
        if (!canisterStack.isEmpty()) {
            drops.add(canisterStack);
        }
        return drops;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return hasValidAttachment(level, pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CCBShapes.AIRTIGHT_HATCH.get(state.getValue(FACING).getOpposite());
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state, @Nullable BlockEntity blockEntity) {
        List<StackRequirement> requirements = new ArrayList<>(2);
        requirements.add(new StackRequirement(new ItemStack(asItem()), ItemUseType.CONSUME));
        if (blockEntity instanceof AirtightHatchBlockEntity hatch) {
            ItemStack requiredCanister = hatch.createCanisterItemStack();
            if (!requiredCanister.isEmpty()) {
                requirements.add(new StrictNbtStackRequirement(requiredCanister, ItemUseType.CONSUME));
                return new ItemRequirement(requirements);
            }

            Level level = hatch.getLevel();
            if (level != null && !level.isClientSide) {
                return new ItemRequirement(requirements);
            }
        }

        CanisterType canisterType = state.getValue(CANISTER_TYPE);
        if (canisterType != CanisterType.EMPTY) {
            ItemStack requiredCanister = new ItemStack(canisterType == CanisterType.CREATIVE ? CCBItems.CREATIVE_GAS_CANISTER.asItem() : CCBItems.GAS_CANISTER.asItem());
            requirements.add(new StrictNbtStackRequirement(requiredCanister, ItemUseType.CONSUME));
        }
        return new ItemRequirement(requirements);
    }

    @Override
    public Class<AirtightHatchBlockEntity> getBlockEntityClass() {
        return AirtightHatchBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends AirtightHatchBlockEntity> getBlockEntityType() {
        return CCBBlockEntities.AIRTIGHT_HATCH.get();
    }

    public enum CanisterType implements StringRepresentable {
        EMPTY,
        NORMAL,
        CREATIVE;

        @Override
        public String getSerializedName() {
            return Lang.asId(name());
        }
    }
}
