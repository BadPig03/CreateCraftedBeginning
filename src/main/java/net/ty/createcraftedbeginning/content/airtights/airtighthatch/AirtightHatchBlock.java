package net.ty.createcraftedbeginning.content.airtights.airtighthatch;

import com.mojang.serialization.Codec;
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
import net.ty.createcraftedbeginning.api.gas.canisters.CanisterContainerSuppliers;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IAirtightComponent;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterContainerContents;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterUtils;
import net.ty.createcraftedbeginning.data.CCBShapes;
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

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(AirtightHatchBlock::new);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }

        Direction direction = context.getClickedFace();
        if (direction.getAxis() == Axis.Y) {
            return null;
        }

        state = state.setValue(FACING, direction.getOpposite()).setValue(CANISTER_TYPE, CanisterType.EMPTY);
        return ProperWaterloggedBlock.withWater(context.getLevel(), state, context.getClickedPos());
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

        BreakEvent event = new BreakEvent(level, pos, state, player);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!player.isCreative()) {
            getDrops(state, serverLevel, pos, blockEntity, player, context.getItemInHand()).forEach(itemStack -> ItemHandlerHelper.giveItemToPlayer(player, itemStack));
        }
        else if (blockEntity instanceof AirtightHatchBlockEntity hatch && state.getValue(CANISTER_TYPE) != CanisterType.EMPTY) {
            hatch.giveCanisterToPlayer(player);
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
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return state;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        Direction facing = state.getValue(FACING);
        BlockPos targetPos = pos.relative(facing);
        if (level.getBlockEntity(targetPos) == null) {
            GasCanisterUtils.displayCustomWarningHint(player, "gui.warnings.invalid_face");
            return ItemInteractionResult.FAIL;
        }

        IGasHandler targetHandler = level.getCapability(GasHandler.BLOCK, targetPos, facing.getOpposite());
        if (targetHandler == null) {
            GasCanisterUtils.displayCustomWarningHint(player, "gui.warnings.invalid_face");
            return ItemInteractionResult.FAIL;
        }

        if (level.getBlockState(targetPos).getBlock() instanceof IAirtightComponent airtightComponent) {
            boolean isFaceAirtight = airtightComponent.isAirtight(targetPos, level.getBlockState(targetPos), facing.getOpposite());
            if (!isFaceAirtight) {
                GasCanisterUtils.displayCustomWarningHint(player, "gui.warnings.invalid_face");
                return ItemInteractionResult.FAIL;
            }
        }

        boolean isEmpty = stack.isEmpty();
        boolean isWrench = stack.is(Items.TOOLS_WRENCH);
        if (player.isShiftKeyDown() && isWrench) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (state.getValue(CANISTER_TYPE) == CanisterType.EMPTY) {
            if (stack.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents) {
                withBlockEntityDo(level, pos, be -> be.setCanisterContent(stack));
                level.setBlockAndUpdate(pos, state.setValue(CANISTER_TYPE, CanisterContainerSuppliers.isValidCreativeGasCanister(stack) ? CanisterType.CREATIVE : CanisterType.NORMAL));
                stack.shrink(1);
                CCBSoundEvents.CANISTER_ADDED.playOnServer(player.level(), player.blockPosition(), 1, 1);
                return ItemInteractionResult.SUCCESS;
            }
            else {
                if (isWrench) {
                    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                }

                if (!isEmpty) {
                    GasCanisterUtils.displayCustomWarningHint(player, "gui.warnings.invalid_item", stack.getHoverName());
                }
                return ItemInteractionResult.FAIL;
            }
        }
        else {
            if (isWrench) {
                withBlockEntityDo(level, pos, be -> be.giveCanisterToPlayer(player));
                level.setBlockAndUpdate(pos, state.setValue(CANISTER_TYPE, CanisterType.EMPTY));
                CCBSoundEvents.CANISTER_REMOVED.playOnServer(player.level(), player.blockPosition(), 1, 1);
                return ItemInteractionResult.SUCCESS;
            }
            else {
                if (!isEmpty) {
                    GasCanisterUtils.displayCustomWarningHint(player, "gui.warnings.invalid_item", stack.getHoverName());
                }
                return ItemInteractionResult.FAIL;
            }
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos blockPos, BlockState blockState, @Nullable LivingEntity placer, ItemStack itemStack) {
        super.setPlacedBy(level, blockPos, blockState, placer, itemStack);
        CCBAdvancementBehaviour.setPlacedBy(level, blockPos, placer);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, Builder builder) {
        List<ItemStack> lootDrops = super.getDrops(state, builder);
        if (!(builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof AirtightHatchBlockEntity hatch) || state.getValue(CANISTER_TYPE) == CanisterType.EMPTY) {
            return lootDrops;
        }
        lootDrops.add(hatch.createCanisterItemStack());
        return lootDrops;
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state, @Nullable BlockEntity blockEntity) {
        List<StackRequirement> requirements = new ArrayList<>();
        requirements.add(new StackRequirement(new ItemStack(asItem()), ItemUseType.CONSUME));
        CanisterType canisterType = state.getValue(CANISTER_TYPE);
        if (canisterType == CanisterType.NORMAL) {
            requirements.add(new StrictNbtStackRequirement(new ItemStack(CCBItems.GAS_CANISTER.asItem()), ItemUseType.CONSUME));
        }
        else if (canisterType == CanisterType.CREATIVE) {
            requirements.add(new StrictNbtStackRequirement(new ItemStack(CCBItems.CREATIVE_GAS_CANISTER.asItem()), ItemUseType.CONSUME));
        }
        return new ItemRequirement(requirements);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction direction = state.getValue(FACING);
        BlockPos relativePos = pos.relative(direction);
        BlockState relativeState = level.getBlockState(relativePos);
        return canSupportCenter(level, relativePos, direction.getOpposite()) && relativeState.getBlock() instanceof IAirtightComponent airtightComponent && airtightComponent.isAirtight(relativePos, relativeState, direction);
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
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CCBShapes.AIRTIGHT_HATCH.get(state.getValue(FACING).getOpposite());
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

        public static final Codec<CanisterType> CODEC = StringRepresentable.fromEnum(CanisterType::values);

        @Override
        public String getSerializedName() {
            return Lang.asId(name());
        }
    }
}
