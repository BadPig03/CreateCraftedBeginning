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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PortableGasInterfaceMovement extends PortableStorageInterfaceMovement {
    private static final String COMPOUND_KEY_WORKING_POSITION = "WorkingPosition";
    private static final String COMPOUND_KEY_CLIENT_PREVIOUS_POSITION = "ClientPreviousPosition";

    public static LerpedFloat getAnimation(@NotNull MovementContext context) {
        if (context.temporaryData instanceof LerpedFloat animation) {
            return animation;
        }

        LerpedFloat animation = LerpedFloat.linear();
        context.temporaryData = animation;
        return animation;
    }

    private static @Nullable PortableGasInterfaceBlockEntity findStationaryInterface(Level level, BlockPos pos, BlockState state, Direction facing) {
        for (int i = 0; i < 2; i++) {
            PortableGasInterfaceBlockEntity interfaceAt = getStationaryInterfaceAt(level, pos.relative(facing, i), state, facing);
            if (interfaceAt == null) {
                continue;
            }

            return interfaceAt;
        }
        return null;
    }

    private static @Nullable PortableGasInterfaceBlockEntity getStationaryInterfaceAt(@NotNull Level level, BlockPos pos, BlockState state, Direction facing) {
        if (!(level.getBlockEntity(pos) instanceof PortableGasInterfaceBlockEntity psi)) {
            return null;
        }

        BlockState blockState = level.getBlockState(pos);
        if (blockState.getBlock() != state.getBlock() || blockState.getValue(PortableGasInterfaceBlock.FACING) != facing.getOpposite()) {
            return null;
        }

        return psi.isPowered() ? null : psi;
    }

    private static Optional<Direction> getCurrentFacingIfValid(@NotNull MovementContext context) {
        Vec3 directionVec = Vec3.atLowerCornerOf(context.state.getValue(PortableGasInterfaceBlock.FACING).getNormal());
        directionVec = context.rotation.apply(directionVec);
        Direction facingFromVector = Direction.getNearest(directionVec.x, directionVec.y, directionVec.z);
        return directionVec.distanceTo(Vec3.atLowerCornerOf(facingFromVector.getNormal())) > 0.5f ? Optional.empty() : Optional.of(facingFromVector);
    }

    @Override
    public Vec3 getActiveAreaOffset(@NotNull MovementContext context) {
        return Vec3.atLowerCornerOf(context.state.getValue(PortableGasInterfaceBlock.FACING).getNormal()).scale(1.85f);
    }

    @Nullable
    @Override
    public ActorVisual createVisual(VisualizationContext visualizationContext, VirtualRenderWorld virtualLevel, MovementContext movementContext) {
        return new PortableGasInterfaceActorVisual(visualizationContext, virtualLevel, movementContext);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderInContraption(@NotNull MovementContext context, VirtualRenderWorld virtualLevel, ContraptionMatrices matrices, MultiBufferSource buffer) {
        if (VisualizationManager.supportsVisualization(context.world)) {
            return;
        }

        PortableGasInterfaceRenderer.renderInContraption(context, virtualLevel, matrices, buffer);
    }

    @Override
    public void visitNewPosition(@NotNull MovementContext context, BlockPos pos) {
        if (context.contraption instanceof CarriageContraption && context.motion.length() > 0.25f) {
            return;
        }
        if (findInterface(context, pos)) {
            return;
        }

        context.data.remove(COMPOUND_KEY_WORKING_POSITION);
    }

    @Override
    public void tick(@NotNull MovementContext context) {
        if (context.world.isClientSide) {
            getAnimation(context).tickChaser();
        }

        boolean onCarriage = context.contraption instanceof CarriageContraption;
        if (onCarriage && context.motion.length() > 0.25f) {
            return;
        }

        if (context.world.isClientSide) {
            if (findInterface(context, BlockPos.containing(context.position))) {
                return;
            }

            reset(context);
            return;
        }

        if (!context.data.contains(COMPOUND_KEY_WORKING_POSITION)) {
            if (context.stall) {
                cancelStall(context);
            }
            return;
        }

        BlockPos pos = NBTHelper.readBlockPos(context.data, COMPOUND_KEY_WORKING_POSITION);
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
        stationaryInterface.keepAlive = 2;
        if (!context.stall || stationaryInterface.getTransferTimer() > PortableGasInterfaceBlockEntity.ANIMATION) {
            return;
        }

        context.stall = false;
    }

    @Override
    protected boolean findInterface(@NotNull MovementContext context, BlockPos pos) {
        if (context.contraption instanceof CarriageContraption contraption && !contraption.notInPortal()) {
            return false;
        }

        Optional<Direction> currentFacingIfValid = getCurrentFacingIfValid(context);
        if (currentFacingIfValid.isEmpty()) {
            return false;
        }

        Direction currentFacing = currentFacingIfValid.get();
        PortableGasInterfaceBlockEntity psi = findStationaryInterface(context.world, pos, context.state, currentFacing);
        if (psi == null || psi.isPowered()) {
            return false;
        }

        context.data.put(COMPOUND_KEY_WORKING_POSITION, NbtUtils.writeBlockPos(psi.getBlockPos()));
        if (context.world.isClientSide) {
            context.data.put(COMPOUND_KEY_CLIENT_PREVIOUS_POSITION, NbtUtils.writeBlockPos(pos));
            if (context.contraption instanceof CarriageContraption || context.contraption.entity.isStalled() || context.motion.lengthSqr() == 0) {
                getAnimation(context).chase(psi.getDistance() / 2, 0.25f, Chaser.LINEAR);
            }
        }
        else {
            Vec3 subtracted = VecHelper.getCenterOf(psi.getBlockPos()).subtract(context.position);
            subtracted = VecHelper.project(subtracted, Vec3.atLowerCornerOf(currentFacing.getNormal()));
            psi.startTransferringTo(context.contraption, (float) (subtracted.length() + 1.85f - 1));
        }
        return true;
    }

    @Override
    public void reset(@NotNull MovementContext context) {
        context.data.remove(COMPOUND_KEY_CLIENT_PREVIOUS_POSITION);
        context.data.remove(COMPOUND_KEY_WORKING_POSITION);
        context.stall = false;
        getAnimation(context).chase(0, 0.25f, Chaser.LINEAR);
    }
}
