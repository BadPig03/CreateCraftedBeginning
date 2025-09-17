package net.ty.createcraftedbeginning.content.airtights.portablegasinterface;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorVisual;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PortableGasInterfaceMovement implements MovementBehaviour {
    public static final String _workingPos_ = "WorkingPos";
    public static final String _clientPrevPos_ = "ClientPrevPos";

    public static LerpedFloat getAnimation(@NotNull MovementContext context) {
        if (!(context.temporaryData instanceof LerpedFloat lf)) {
            LerpedFloat nlf = LerpedFloat.linear();
            context.temporaryData = nlf;
            return nlf;
        }
        return lf;
    }

    @Override
    public void tick(@NotNull MovementContext context) {
        if (context.world.isClientSide) {
            getAnimation(context).tickChaser();
        }

        boolean onCarriage = context.contraption instanceof CarriageContraption;
        if (onCarriage && context.motion.length() > 1 / 4f) {
            return;
        }

        if (context.world.isClientSide) {
            BlockPos pos = BlockPos.containing(context.position);
            if (!findInterface(context, pos)) {
                reset(context);
            }
            return;
        }

        if (!context.data.contains(_workingPos_)) {
            if (context.stall) {
                cancelStall(context);
            }
            return;
        }

        BlockPos pos = NBTHelper.readBlockPos(context.data, _workingPos_);
        Vec3 target = VecHelper.getCenterOf(pos);

        if (!context.stall && !onCarriage && context.position.closerThan(target, target.distanceTo(context.position.add(context.motion)))) {
            context.stall = true;
        }

        Optional<Direction> currentFacingIfValid = getCurrentFacingIfValid(context);
        if (currentFacingIfValid.isEmpty()) {
            return;
        }

        PortableGasInterfaceBlockEntity stationaryInterface = getStationaryInterfaceAt(context.world, pos, context.state, currentFacingIfValid.get());
        if (stationaryInterface == null) {
            reset(context);
            return;
        }

        if (stationaryInterface.getConnectedEntity() == null) {
            stationaryInterface.startTransferringTo(context.contraption, stationaryInterface.getDistance());
        }

        boolean timerBelow = stationaryInterface.getTransferTimer() <= PortableGasInterfaceBlockEntity.ANIMATION;
        stationaryInterface.keepAlive = 2;
        if (context.stall && timerBelow) {
            context.stall = false;
        }
    }

    @Override
    public void visitNewPosition(@NotNull MovementContext context, BlockPos pos) {
        boolean onCarriage = context.contraption instanceof CarriageContraption;
        if (onCarriage && context.motion.length() > 1 / 4f) {
            return;
        }
        if (!findInterface(context, pos)) {
            context.data.remove(_workingPos_);
        }
    }

    @Override
    public Vec3 getActiveAreaOffset(@NotNull MovementContext context) {
        return Vec3.atLowerCornerOf(context.state.getValue(PortableGasInterfaceBlock.FACING).getNormal()).scale(1.85f);
    }

    @Override
    public void cancelStall(MovementContext context) {
        reset(context);
    }

    @Override
    public boolean disableBlockEntityRendering() {
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderInContraption(@NotNull MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices, MultiBufferSource buffer) {
        if (!VisualizationManager.supportsVisualization(context.world)) {
            PortableGasInterfaceRenderer.renderInContraption(context, renderWorld, matrices, buffer);
        }
    }

    @Nullable
    @Override
    public ActorVisual createVisual(VisualizationContext visualizationContext, VirtualRenderWorld simulationWorld, MovementContext movementContext) {
        return new PortableGasInterfaceActorVisual(visualizationContext, simulationWorld, movementContext);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean findInterface(@NotNull MovementContext context, BlockPos pos) {
        if (context.contraption instanceof CarriageContraption cc && !cc.notInPortal()) {
            return false;
        }
        Optional<Direction> currentFacingIfValid = getCurrentFacingIfValid(context);
        if (currentFacingIfValid.isEmpty()) {
            return false;
        }

        Direction currentFacing = currentFacingIfValid.get();
        PortableGasInterfaceBlockEntity psi = findStationaryInterface(context.world, pos, context.state, currentFacing);

        if (psi == null) {
            return false;
        }
        if (psi.isPowered()) {
            return false;
        }

        context.data.put(_workingPos_, NbtUtils.writeBlockPos(psi.getBlockPos()));
        if (!context.world.isClientSide) {
            Vec3 diff = VecHelper.getCenterOf(psi.getBlockPos()).subtract(context.position);
            diff = VecHelper.project(diff, Vec3.atLowerCornerOf(currentFacing.getNormal()));
            float distance = (float) (diff.length() + 1.85f - 1);
            psi.startTransferringTo(context.contraption, distance);
        } else {
            context.data.put(_clientPrevPos_, NbtUtils.writeBlockPos(pos));
            if (context.contraption instanceof CarriageContraption || context.contraption.entity.isStalled() || context.motion.lengthSqr() == 0) {
                getAnimation(context).chase(psi.getDistance() / 2, 0.25f, LerpedFloat.Chaser.LINEAR);
            }
        }

        return true;
    }

    public void reset(@NotNull MovementContext context) {
        context.data.remove(_clientPrevPos_);
        context.data.remove(_workingPos_);
        context.stall = false;
        getAnimation(context).chase(0, 0.25f, LerpedFloat.Chaser.LINEAR);
    }

    private @Nullable PortableGasInterfaceBlockEntity findStationaryInterface(Level world, BlockPos pos, BlockState state, Direction facing) {
        for (int i = 0; i < 2; i++) {
            PortableGasInterfaceBlockEntity interfaceAt = getStationaryInterfaceAt(world, pos.relative(facing, i), state, facing);
            if (interfaceAt == null) {
                continue;
            }
            return interfaceAt;
        }
        return null;
    }

    private @Nullable PortableGasInterfaceBlockEntity getStationaryInterfaceAt(@NotNull Level world, BlockPos pos, BlockState state, Direction facing) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof PortableGasInterfaceBlockEntity psi)) {
            return null;
        }
        BlockState blockState = world.getBlockState(pos);
        if (blockState.getBlock() != state.getBlock()) {
            return null;
        }
        if (blockState.getValue(PortableGasInterfaceBlock.FACING) != facing.getOpposite()) {
            return null;
        }
        if (psi.isPowered()) {
            return null;
        }
        return psi;
    }

    private Optional<Direction> getCurrentFacingIfValid(@NotNull MovementContext context) {
        Vec3 directionVec = Vec3.atLowerCornerOf(context.state.getValue(PortableGasInterfaceBlock.FACING).getNormal());
        directionVec = context.rotation.apply(directionVec);
        Direction facingFromVector = Direction.getNearest(directionVec.x, directionVec.y, directionVec.z);
        if (directionVec.distanceTo(Vec3.atLowerCornerOf(facingFromVector.getNormal())) > 1 / 2f) {
            return Optional.empty();
        }
        return Optional.of(facingFromVector);
    }
}
