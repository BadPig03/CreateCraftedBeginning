package net.ty.createcraftedbeginning.content.airtights.portablegasinterface;

import com.simibubi.create.content.contraptions.actors.psi.PortableStorageInterfaceMovement;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorVisual;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PortableGasInterfaceMovement extends PortableStorageInterfaceMovement {
    private static final String COMPOUND_KEY_WORKING_POSITION = "WorkingPosition";
    private static final String COMPOUND_KEY_CLIENT_PREVIOUS_POSITION = "ClientPreviousPosition";

    public static LerpedFloat getAnimation(MovementContext context) {
        if (context.temporaryData instanceof LerpedFloat animation) {
            return animation;
        }

        LerpedFloat animation = LerpedFloat.linear();
        context.temporaryData = animation;
        return animation;
    }

    private static @Nullable PortableGasInterfaceBlockEntity findStationaryInterface(Level level, BlockPos pos, BlockState state, Direction facing) {
        for (int offset = 0; offset < 2; offset++) {
            PortableGasInterfaceBlockEntity stationary = getStationaryInterfaceAt(level, pos.relative(facing, offset), state, facing);
            if (stationary == null) {
                continue;
            }

            return stationary;
        }
        return null;
    }

    private static @Nullable PortableGasInterfaceBlockEntity getStationaryInterfaceAt(Level level, BlockPos pos, BlockState state, Direction facing) {
        if (!(level.getBlockEntity(pos) instanceof PortableGasInterfaceBlockEntity stationary)) {
            return null;
        }

        BlockState stationaryState = level.getBlockState(pos);
        if (stationaryState.getBlock() != state.getBlock() || stationaryState.getValue(PortableGasInterfaceBlock.FACING) != facing.getOpposite()) {
            return null;
        }

        if (stationary.isPowered()) {
            return null;
        }
        return stationary;
    }

    private static Optional<Direction> getValidFacing(MovementContext context) {
        Vec3 facingVector = Vec3.atLowerCornerOf(context.state.getValue(PortableGasInterfaceBlock.FACING).getNormal());
        Vec3 rotated = context.rotation.apply(facingVector);
        Direction facing = Direction.getNearest(rotated.x, rotated.y, rotated.z);
        Vec3 nearest = Vec3.atLowerCornerOf(facing.getNormal());
        if (rotated.distanceTo(nearest) > 0.5f) {
            return Optional.empty();
        }
        return Optional.of(facing);
    }

    private static boolean shouldStall(MovementContext context, Vec3 target, boolean isOnCarriage) {
        if (context.stall || isOnCarriage) {
            return false;
        }

        Vec3 nextPos = context.position.add(context.motion);
        return context.position.closerThan(target, target.distanceTo(nextPos));
    }

    private static void updateClientConnection(MovementContext context, BlockPos pos, PortableGasInterfaceBlockEntity stationary) {
        context.data.put(COMPOUND_KEY_CLIENT_PREVIOUS_POSITION, NbtUtils.writeBlockPos(pos));

        boolean shouldAnimate = context.contraption instanceof CarriageContraption || context.contraption.entity.isStalled() || context.motion.lengthSqr() == 0;
        if (!shouldAnimate) {
            return;
        }

        getAnimation(context).chase(stationary.getDistance() / 2, 0.25f, Chaser.LINEAR);
    }

    private static void startTransfer(MovementContext context, PortableGasInterfaceBlockEntity stationary, Direction facing) {
        Vec3 offset = VecHelper.getCenterOf(stationary.getBlockPos()).subtract(context.position);
        Vec3 projected = VecHelper.project(offset, Vec3.atLowerCornerOf(facing.getNormal()));
        stationary.startTransferringTo(context.contraption, (float) (projected.length() + 1.85f - 1));
    }

    @Override
    public Vec3 getActiveAreaOffset(MovementContext context) {
        return Vec3.atLowerCornerOf(context.state.getValue(PortableGasInterfaceBlock.FACING).getNormal()).scale(1.85);
    }

    @Nullable
    @Override
    public ActorVisual createVisual(VisualizationContext visualizationContext, VirtualRenderWorld virtualLevel, MovementContext movementContext) {
        return new PortableGasInterfaceActorVisual(visualizationContext, virtualLevel, movementContext);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderInContraption(MovementContext context, VirtualRenderWorld virtualLevel, ContraptionMatrices matrices, MultiBufferSource buffer) {
        if (VisualizationManager.supportsVisualization(context.world)) {
            return;
        }

        PortableGasInterfaceRenderer.renderInContraption(context, virtualLevel, matrices, buffer);
    }

    @Override
    public void visitNewPosition(MovementContext context, BlockPos pos) {
        if (context.contraption instanceof CarriageContraption && context.motion.length() > 0.25f) {
            return;
        }

        if (findInterface(context, pos)) {
            return;
        }

        context.data.remove(COMPOUND_KEY_WORKING_POSITION);
    }

    @Override
    public void tick(MovementContext context) {
        if (context.world.isClientSide) {
            getAnimation(context).tickChaser();
        }

        boolean isOnCarriage = context.contraption instanceof CarriageContraption;
        if (isOnCarriage && context.motion.length() > 0.25f) {
            return;
        }

        if (context.world.isClientSide) {
            tickClient(context);
            return;
        }

        tickServer(context, isOnCarriage);
    }

    @Override
    protected boolean findInterface(MovementContext context, BlockPos pos) {
        if (context.contraption instanceof CarriageContraption contraption && !contraption.notInPortal()) {
            return false;
        }

        Optional<Direction> facing = getValidFacing(context);
        if (facing.isEmpty()) {
            reset(context);
            return false;
        }

        Direction currentFacing = facing.get();
        PortableGasInterfaceBlockEntity stationary = findStationaryInterface(context.world, pos, context.state, currentFacing);
        if (stationary == null || stationary.isPowered()) {
            return false;
        }

        context.data.put(COMPOUND_KEY_WORKING_POSITION, NbtUtils.writeBlockPos(stationary.getBlockPos()));
        if (context.world.isClientSide) {
            updateClientConnection(context, pos, stationary);
            return true;
        }

        startTransfer(context, stationary, currentFacing);
        return true;
    }

    @Override
    public void reset(MovementContext context) {
        context.data.remove(COMPOUND_KEY_CLIENT_PREVIOUS_POSITION);
        context.data.remove(COMPOUND_KEY_WORKING_POSITION);
        context.stall = false;
        getAnimation(context).chase(0, 0.25f, Chaser.LINEAR);
    }

    protected void tickClient(MovementContext context) {
        if (findInterface(context, BlockPos.containing(context.position))) {
            return;
        }

        reset(context);
    }

    protected void tickServer(MovementContext context, boolean isOnCarriage) {
        if (!context.data.contains(COMPOUND_KEY_WORKING_POSITION)) {
            if (context.stall) {
                cancelStall(context);
            }
            return;
        }

        BlockPos pos = NBTHelper.readBlockPos(context.data, COMPOUND_KEY_WORKING_POSITION);
        Vec3 target = VecHelper.getCenterOf(pos);
        if (shouldStall(context, target, isOnCarriage)) {
            context.stall = true;
        }

        Optional<Direction> facing = getValidFacing(context);
        if (facing.isEmpty()) {
            reset(context);
            return;
        }

        PortableGasInterfaceBlockEntity stationary = getStationaryInterfaceAt(context.world, pos, context.state, facing.get());
        if (stationary == null) {
            reset(context);
            return;
        }

        if (stationary.getConnectedEntity() == null) {
            stationary.startTransferringTo(context.contraption, stationary.getDistance());
        }
        stationary.keepAlive = 2;
        if (!context.stall || stationary.getTransferTimer() > PortableGasInterfaceBlockEntity.ANIMATION) {
            return;
        }

        context.stall = false;
    }
}
