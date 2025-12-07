package net.ty.createcraftedbeginning.content.airtights.teslaturbine;

import com.mojang.serialization.Codec;
import com.simibubi.create.api.equipment.goggles.IProxyHoveringInformation;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.render.MultiPosDestructionHandler;
import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import net.ty.createcraftedbeginning.content.airtights.teslaturbinenozzle.TeslaTurbineNozzleBlock;
import net.ty.createcraftedbeginning.data.CCBShapes;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import static net.ty.createcraftedbeginning.content.airtights.teslaturbine.TeslaTurbineBlock.calculateStructurePos;

public class TeslaTurbineStructuralBlock extends RotatedPillarBlock implements IWrenchable, SimpleWaterloggedBlock, IProxyHoveringInformation {
    private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final int PLACEMENT_HELPER_ID = PlacementHelpers.register(new NozzlePlacementHelper());

    public static final EnumProperty<StructuralPosition> STRUCTURAL_POSITION = EnumProperty.create("structural_position", StructuralPosition.class);

    public TeslaTurbineStructuralBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false).setValue(STRUCTURAL_POSITION, StructuralPosition.TOP_MID));
    }

    public static @NotNull BlockPos getMaster(@NotNull BlockPos pos, @NotNull BlockState state) {
        return pos.subtract(calculateOffset(state.getValue(STRUCTURAL_POSITION), state.getValue(AXIS)));
    }

    @Contract(value = "_, _ -> new", pure = true)
    private static @NotNull BlockPos calculateOffset(@NotNull StructuralPosition position, @NotNull Axis axis) {
        int u = position.u;
        int v = position.v;
        switch (axis) {
            case X -> {
                return new BlockPos(0, v, u);
            }
            case Z -> {
                return new BlockPos(u, v, 0);
            }
            default -> {
                return new BlockPos(u, 0, v);
            }
        }
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, @NotNull UseOnContext context) {
        BlockPos clickedPos = context.getClickedPos();
        Level level = context.getLevel();
        if (!stillValid(level, clickedPos, state, false)) {
            return IWrenchable.super.onSneakWrenched(state, context);
        }

        BlockPos masterPos = getMaster(clickedPos, state);
        context = new UseOnContext(level, context.getPlayer(), context.getHand(), context.getItemInHand(), new BlockHitResult(context.getClickLocation(), context.getClickedFace(), masterPos, context.isInside()));
        state = level.getBlockState(masterPos);
        return IWrenchable.super.onSneakWrenched(state, context);
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull BlockState state, @NotNull HitResult target, @NotNull LevelReader level, @NotNull BlockPos pos, @NotNull Player player) {
        return CCBBlocks.TESLA_TURBINE_BLOCK.asStack();
    }

    @Override
    public boolean addLandingEffects(@NotNull BlockState blockState1, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull BlockState blockState2, @NotNull LivingEntity entity, int numberOfParticles) {
        return true;
    }

    @Override
    public PushReaction getPistonPushReaction(@NotNull BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    public @NotNull BlockState playerWillDestroy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull Player player) {
        if (!stillValid(level, pos, state, false)) {
            return super.playerWillDestroy(level, pos, state, player);
        }

        BlockPos masterPos = getMaster(pos, state);
        level.destroyBlockProgress(masterPos.hashCode(), masterPos, -1);
        if (!level.isClientSide && player.isCreative()) {
            level.destroyBlock(masterPos, false);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void createBlockStateDefinition(@NotNull Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED).add(STRUCTURAL_POSITION);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public @NotNull BlockState updateShape(@NotNull BlockState state, @NotNull Direction direction, @NotNull BlockState neighborState, @NotNull LevelAccessor accessor, @NotNull BlockPos pos, @NotNull BlockPos neighborPos) {
        if (stillValid(accessor, pos, state, false)) {
            BlockPos masterPos = getMaster(pos, state);
            if (!accessor.getBlockTicks().hasScheduledTick(masterPos, CCBBlocks.TESLA_TURBINE_BLOCK.get())) {
                accessor.scheduleTick(masterPos, CCBBlocks.TESLA_TURBINE_BLOCK.get(), 1);
            }
            return state;
        }
        if (!(accessor instanceof Level level) || level.isClientSide) {
            return state;
        }

        if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, 1);
        }
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return state;
    }

    @Override
    public void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean moving) {
        if (!stillValid(level, pos, state, false)) {
            return;
        }

        level.destroyBlock(getMaster(pos, state), true);
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hitResult) {
        IPlacementHelper placementHelper = PlacementHelpers.get(PLACEMENT_HELPER_ID);
        return placementHelper.matchesItem(stack) ? placementHelper.getOffset(player, level, state, pos, hitResult).placeInWorld(level, (BlockItem) stack.getItem(), player, hand, hitResult) : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public @NotNull FluidState getFluidState(@NotNull BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.defaultFluidState() : super.getFluidState(state);
    }

    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter worldIn, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return CCBShapes.TESLA_TURBINE.get(state.getValue(BlockStateProperties.AXIS));
    }

    @Override
    public void tick(@NotNull BlockState state, @NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull RandomSource random) {
        if (stillValid(level, pos, state, false)) {
            return;
        }

        level.destroyBlock(pos, false);
    }

    public boolean stillValid(BlockGetter level, BlockPos pos, @NotNull BlockState state, boolean ignored) {
        if (!state.is(this)) {
            return false;
        }

        BlockState targetedState = level.getBlockState(getMaster(pos, state));
        return targetedState.getBlock() instanceof TeslaTurbineBlock && targetedState.getValue(TeslaTurbineBlock.AXIS) == state.getValue(AXIS);
    }

    @Override
    public BlockPos getInformationSource(Level level, BlockPos pos, BlockState state) {
        return stillValid(level, pos, state, false) ? getMaster(pos, state) : pos;
    }

    public enum StructuralPosition implements StringRepresentable {
        TOP_LEFT(-1, 1),
        TOP_MID(0, 1),
        TOP_RIGHT(1, 1),
        MID_LEFT(-1, 0),
        MID_RIGHT(1, 0),
        BOTTOM_LEFT(-1, -1),
        BOTTOM_MID(0, -1),
        BOTTOM_RIGHT(1, -1);

        public static final Codec<StructuralPosition> CODEC = StringRepresentable.fromEnum(StructuralPosition::values);
        public final int u;
        public final int v;

        StructuralPosition(int u, int v) {
            this.u = u;
            this.v = v;
        }

        @Contract(pure = true)
        public static boolean isMid(@NotNull StructuralPosition pos) {
            return pos.u == 0 || pos.v == 0;
        }

        public static StructuralPosition fromOffset(int u, int v) {
            if (u == -1 && v == 1) {
                return TOP_LEFT;
            }
            if (u == 0 && v == 1) {
                return TOP_MID;
            }
            if (u == 1 && v == 1) {
                return TOP_RIGHT;
            }
            if (u == -1 && v == 0) {
                return MID_LEFT;
            }
            if (u == 1 && v == 0) {
                return MID_RIGHT;
            }
            if (u == -1 && v == -1) {
                return BOTTOM_LEFT;
            }
            if (u == 0 && v == -1) {
                return BOTTOM_MID;
            }
            return u == 1 && v == -1 ? BOTTOM_RIGHT : TOP_MID;
        }

        public static @NotNull Set<Direction> getPossiblePosition(@NotNull StructuralPosition pos, Axis axis) {
            Set<Direction> directionSet = new HashSet<>();
            int u = pos.u;
            int v = pos.v;
            if (axis == Axis.X) {
                directionSet.add(u > 0 ? Direction.SOUTH : Direction.NORTH);
                directionSet.add(v > 0 ? Direction.UP : Direction.DOWN);
            }
            else if (axis == Axis.Z) {
                directionSet.add(u > 0 ? Direction.EAST : Direction.WEST);
                directionSet.add(v > 0 ? Direction.UP : Direction.DOWN);
            }
            else {
                directionSet.add(u > 0 ? Direction.EAST : Direction.WEST);
                directionSet.add(v > 0 ? Direction.SOUTH : Direction.NORTH);
            }
            return directionSet;
        }

        @Override
        public @NotNull String getSerializedName() {
            return Lang.asId(name());
        }
    }

    public static class RenderProperties implements IClientBlockExtensions, MultiPosDestructionHandler {
        @Override
        public boolean addHitEffects(@NotNull BlockState state, @NotNull Level level, @NotNull HitResult target, @NotNull ParticleEngine manager) {
            if (!(target instanceof BlockHitResult result)) {
                return IClientBlockExtensions.super.addHitEffects(state, level, target, manager);
            }

            BlockPos targetPos = result.getBlockPos();
            TeslaTurbineStructuralBlock block = CCBBlocks.TESLA_TURBINE_STRUCTURAL_BLOCK.get();
            if (!block.stillValid(level, targetPos, state, false)) {
                return true;
            }

            manager.crack(getMaster(targetPos, state), result.getDirection());
            return IClientBlockExtensions.super.addHitEffects(state, level, target, manager);
        }

        @Override
        public boolean addDestroyEffects(@NotNull BlockState state, @NotNull Level Level, @NotNull BlockPos pos, @NotNull ParticleEngine manager) {
            return true;
        }

        @Override
        @Nullable
        public Set<BlockPos> getExtraPositions(ClientLevel level, BlockPos pos, BlockState blockState, int progress) {
            TeslaTurbineStructuralBlock block = CCBBlocks.TESLA_TURBINE_STRUCTURAL_BLOCK.get();
            if (!block.stillValid(level, pos, blockState, false)) {
                return null;
            }

            BlockPos masterPos = getMaster(pos, blockState);
            Axis axis = blockState.getValue(AXIS);
            HashSet<BlockPos> positions = new HashSet<>();
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    if (i == 0 && j == 0) {
                        continue;
                    }

                    BlockPos structurePos = calculateStructurePos(masterPos, axis, i, j);
                    positions.add(structurePos);
                }
            }

            positions.add(masterPos);
            return positions;
        }
    }

    private static class NozzlePlacementHelper implements IPlacementHelper {
        private static final Set<BlockPos> ALLOWED_OFFSETS = Set.of(new BlockPos(2, 1, 0), new BlockPos(2, -1, 0), new BlockPos(-2, 1, 0), new BlockPos(-2, -1, 0), new BlockPos(1, 2, 0), new BlockPos(1, -2, 0), new BlockPos(-1, 2, 0), new BlockPos(-1, -2, 0));

        private static @NotNull Set<BlockPos> getWorldOffsets(Axis axis) {
            Set<BlockPos> worldOffsets = new HashSet<>();
            for (BlockPos offset : ALLOWED_OFFSETS) {
                BlockPos transformedOffset;
                if (axis == Axis.X) {
                    transformedOffset = new BlockPos(0, offset.getY(), offset.getX());
                }
                else if (axis == Axis.Z) {
                    transformedOffset = new BlockPos(offset.getX(), offset.getY(), 0);
                }
                else {
                    transformedOffset = new BlockPos(offset.getX(), 0, offset.getY());
                }
                worldOffsets.add(transformedOffset);
            }
            return worldOffsets;
        }

        private static Direction calculateFacingDirection(BlockPos nozzlePos, @NotNull BlockPos masterPos, Axis axis) {
            BlockPos diff = masterPos.subtract(nozzlePos);
            int x = diff.getX();
            int y = diff.getY();
            int z = diff.getZ();

            if (axis == Axis.X) {
                if (Math.abs(z) > Math.abs(y)) {
                    return z > 0 ? Direction.NORTH : Direction.SOUTH;
                }
                else {
                    return y > 0 ? Direction.DOWN : Direction.UP;
                }
            }
            else if (axis == Axis.Z) {
                if (Math.abs(x) > Math.abs(y)) {
                    return x > 0 ? Direction.WEST : Direction.EAST;
                }
                else {
                    return y > 0 ? Direction.DOWN : Direction.UP;
                }
            }
            else {
                if (Math.abs(x) > Math.abs(z)) {
                    return x > 0 ? Direction.WEST : Direction.EAST;
                }
                else {
                    return z > 0 ? Direction.NORTH : Direction.SOUTH;
                }
            }
        }

        @Contract(pure = true)
        @Override
        public @NotNull Predicate<ItemStack> getItemPredicate() {
            return CCBBlocks.TESLA_TURBINE_NOZZLE_BLOCK::isIn;
        }

        @Contract(pure = true)
        @Override
        public @NotNull Predicate<BlockState> getStatePredicate() {
            return s -> s.getBlock() instanceof TeslaTurbineStructuralBlock;
        }

        @Override
        public @NotNull PlacementOffset getOffset(@NotNull Player player, @NotNull Level level, @NotNull BlockState state, @NotNull BlockPos pos, @NotNull BlockHitResult ray) {
            BlockPos masterPos = getMaster(pos, state);
            Axis axis = state.getValue(BlockStateProperties.AXIS);
            Set<BlockPos> worldOffsets = getWorldOffsets(axis);

            BlockPos bestPos = null;
            double minDistance = Double.MAX_VALUE;
            Vec3 hitPos = ray.getLocation();

            for (BlockPos offset : worldOffsets) {
                BlockPos candidate = masterPos.offset(offset);

                if (!level.getBlockState(candidate).canBeReplaced()) {
                    continue;
                }

                double distance = candidate.distToCenterSqr(hitPos);
                if (distance < minDistance) {
                    minDistance = distance;
                    bestPos = candidate;
                }
            }

            if (bestPos == null) {
                return PlacementOffset.fail();
            }

            Direction facing = calculateFacingDirection(bestPos, masterPos, axis);
            if (TeslaTurbineNozzleBlock.isInvalidPlacement(level, facing.getOpposite(), bestPos)) {
                return PlacementOffset.fail();
            }

            boolean clockwise = TeslaTurbineNozzleBlock.isClockwise(level, facing.getOpposite(), bestPos);
            BlockState nozzleState = CCBBlocks.TESLA_TURBINE_NOZZLE_BLOCK.get().defaultBlockState().setValue(TeslaTurbineNozzleBlock.FACING, facing).setValue(TeslaTurbineNozzleBlock.CLOCKWISE, clockwise);
            return PlacementOffset.success(bestPos, s -> nozzleState);
        }
    }
}
