package net.ty.createcraftedbeginning.content.opticalpower.opticalfiber;

import com.simibubi.create.content.equipment.extendoGrip.ExtendoGripItem;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class OpticalFiberPlacementHelper implements IPlacementHelper {
    private static final double STRAIGHT_DIRECTION_TOLERANCE = 0.0625;
    private static final int EXTENDO_GRIP_EXTRA_RANGE = 4;

    private static @Nullable Axis findStraightAxis(BlockState state) {
        Axis axis = null;
        for (Direction direction : Iterate.directions) {
            if (!OpticalFiberBlock.isConnected(state, direction)) {
                continue;
            }

            Axis connectionAxis = direction.getAxis();
            if (axis != null && axis != connectionAxis) {
                return null;
            }

            axis = connectionAxis;
        }
        return axis;
    }

    private static Direction resolveNearestDirection(Vec3 localHit, @Nullable Axis axis) {
        Direction nearestDirection = Iterate.directions[0];
        double nearestDistance = Double.POSITIVE_INFINITY;
        boolean foundDirection = false;
        for (Direction direction : Iterate.directions) {
            if (axis != null && direction.getAxis() != axis) {
                continue;
            }

            double distance = Vec3.atLowerCornerOf(direction.getNormal()).distanceTo(localHit);
            if (foundDirection && Double.compare(distance, nearestDistance) >= 0) {
                continue;
            }

            nearestDirection = direction;
            nearestDistance = distance;
            foundDirection = true;
        }
        return nearestDirection;
    }

    @Override
    public Predicate<ItemStack> getItemPredicate() {
        return CCBBlocks.OPTICAL_FIBER_BLOCK::isIn;
    }

    @Override
    public Predicate<BlockState> getStatePredicate() {
        return state -> !state.isAir();
    }

    @Override
    public PlacementOffset getOffset(Player player, Level level, BlockState state, BlockPos pos, BlockHitResult ray) {
        ItemStack heldItem = player.getMainHandItem();
        if (!matchesItem(heldItem)) {
            heldItem = player.getOffhandItem();
        }

        return getOffset(player, level, state, pos, ray, heldItem);
    }

    @Override
    public PlacementOffset getOffset(Player player, Level level, BlockState state, BlockPos pos, BlockHitResult ray, ItemStack heldItem) {
        if (!canAssist(player, level, pos, heldItem)) {
            return PlacementOffset.fail();
        }

        InteractionHand hand = InteractionHand.MAIN_HAND;
        if (heldItem == player.getOffhandItem()) {
            hand = InteractionHand.OFF_HAND;
        }

        Placement placement = planPlacement(player, level, state, pos, ray, hand, heldItem);
        if (placement == null) {
            return PlacementOffset.fail();
        }

        return placement.toOffset();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderAt(BlockPos pos, BlockState state, BlockHitResult ray, PlacementOffset offset) {
        OpticalFiberPlacementRenderer.show(this, offset);
    }

    boolean canAssist(Player player, Level level, BlockPos pos, ItemStack heldItem) {
        return !player.isShiftKeyDown() && player.mayBuild() && matchesItem(heldItem) && level.isLoaded(pos);
    }

    @Nullable Placement planPlacement(Player player, Level level, BlockState state, BlockPos pos, BlockHitResult ray, InteractionHand hand, ItemStack heldItem) {
        Direction clickedFace = ray.getDirection();
        Direction direction = clickedFace;
        if (state.getBlock() instanceof OpticalFiberBlock) {
            Vec3 localHit = ray.getLocation().subtract(Vec3.atCenterOf(pos));
            direction = resolveNearestDirection(localHit, null);
            Axis axis = findStraightAxis(state);
            if (axis != null && Math.abs(axis.choose(localHit.x, localHit.y, localHit.z)) + STRAIGHT_DIRECTION_TOLERANCE >= localHit.dot(Vec3.atLowerCornerOf(direction.getNormal()))) {
                direction = resolveNearestDirection(localHit, axis);
            }
        }
        else if (!OpticalFiberBlock.canConnectTo(level, pos, state, direction)) {
            return null;
        }

        int range = AllConfigs.server().equipment.placementAssistRange.get();
        AttributeInstance reach = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        if (reach != null && reach.hasModifier(ExtendoGripItem.singleRangeAttributeModifier.id())) {
            range += EXTENDO_GRIP_EXTRA_RANGE;
        }

        Axis axis = direction.getAxis();
        WorldBorder border = level.getWorldBorder();
        for (int distance = 1; distance <= range; distance++) {
            BlockPos target = pos.relative(direction, distance);
            if (!level.isLoaded(target) || !border.isWithinBounds(target)) {
                return null;
            }

            BlockState targetState = level.getBlockState(target);
            if (targetState.getBlock() instanceof OpticalFiberBlock && findStraightAxis(targetState) == axis) {
                continue;
            }

            if (!targetState.canBeReplaced() || !level.mayInteract(player, target)) {
                return null;
            }

            for (Direction neighborDirection : Iterate.directions) {
                BlockPos neighbor = target.relative(neighborDirection);
                if (!level.isOutsideBuildHeight(neighbor) && !level.isLoaded(neighbor)) {
                    return null;
                }
            }

            Vec3 hitLocation = Vec3.atCenterOf(target).add(clickedFace.getStepX() * 0.5, clickedFace.getStepY() * 0.5, clickedFace.getStepZ() * 0.5);
            BlockHitResult targetRay = new BlockHitResult(hitLocation, clickedFace, target, false);
            BlockPlaceContext context = new BlockPlaceContext(level, player, hand, heldItem, targetRay);
            if (!target.equals(context.getClickedPos()) || !player.mayUseItemAt(target, clickedFace, heldItem)) {
                return null;
            }

            BlockState placementState = CCBBlocks.OPTICAL_FIBER_BLOCK.get().getStateForPlacement(context);
            if (!level.isUnobstructed(placementState, target, CollisionContext.of(player))) {
                return null;
            }

            return new Placement(context, placementState);
        }
        return null;
    }

    record Placement(BlockPlaceContext context, BlockState state) {
        PlacementOffset toOffset() {
            BlockState ghostState = state;
            return PlacementOffset.success(context.getClickedPos(), ignored -> ghostState).withGhostState(ghostState);
        }
    }
}
