package net.ty.createcraftedbeginning.content.end.endsculksilencer;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorVisual;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.compat.sable.SableSubLevelCompat;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class EndSculkSilencerMovementBehaviour implements MovementBehaviour {
    private static final short MOVING_RANGE = 1;

    public static float getAnimationAngle(MovementContext context, float partialTicks) {
        return getAnimationState(context).angle.getValue(partialTicks);
    }

    private static AnimationState getAnimationState(MovementContext context) {
        if (context.temporaryData instanceof AnimationState state) {
            return state;
        }

        AnimationState state = new AnimationState();
        context.temporaryData = state;
        return state;
    }

    private static void tickAnimation(MovementContext context) {
        AnimationState state = getAnimationState(context);
        float targetSpeed = context.disabled ? 0 : EndSculkSilencerBlockEntity.calculateAnimationTargetSpeed(SpeedLevel.FAST.getSpeedValue());
        state.speed.chase(targetSpeed, context.disabled ? 0.2 : 0.1, Chaser.EXP);
        state.speed.tickChaser();
        state.angle.setValue(state.angle.getValue() + state.speed.getValue());
    }

    private static void removeRegistration(MovementContext context) {
        if (!(context.world instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos registrationPos = getRegistrationPos(context);
        if (registrationPos == null) {
            return;
        }

        GlobalEndSculkSilencerManager.removeMoving(serverLevel, registrationPos);
    }

    private static @Nullable BlockPos getRegistrationPos(MovementContext context) {
        if (context.contraption.entity == null) {
            return null;
        }

        UUID contraptionId = context.contraption.entity.getUUID();
        long seed = contraptionId.getMostSignificantBits();
        seed ^= Long.rotateLeft(contraptionId.getLeastSignificantBits(), 23);
        seed ^= Long.rotateLeft(context.localPos.asLong(), 41);
        long hash = mix64(seed);
        int x = unpackSignedCoordinate(hash);
        int z = unpackSignedCoordinate(hash >>> 26);
        return new BlockPos(x, -2048, z);
    }

    private static int unpackSignedCoordinate(long value) {
        int coordinate = (int) (value & 0x3FFFFFFL);
        return coordinate >= 0x2000000 ? coordinate - 0x4000000 : coordinate;
    }

    private static long mix64(long value) {
        value = (value ^ value >>> 30) * 0xBF58476D1CE4E5B9L;
        value = (value ^ value >>> 27) * 0x94D049BB133111EBL;
        return value ^ value >>> 31;
    }

    @Override
    public void tick(MovementContext context) {
        if (context.world.isClientSide) {
            tickAnimation(context);
            return;
        }

        if (!(context.world instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos registrationPos = getRegistrationPos(context);
        if (registrationPos == null) {
            return;
        }

        if (context.disabled || context.position == null) {
            GlobalEndSculkSilencerManager.removeMoving(serverLevel, registrationPos);
            return;
        }

        BlockPos effectCenter = SableSubLevelCompat.resolve(serverLevel, context.position).blockPos();
        GlobalEndSculkSilencerManager.updateMoving(serverLevel, registrationPos, effectCenter, MOVING_RANGE);
    }

    @Override
    public void onDisabledByControls(MovementContext context) {
        MovementBehaviour.super.onDisabledByControls(context);
        removeRegistration(context);
    }

    @Override
    public boolean mustTickWhileDisabled() {
        return true;
    }

    @Override
    public void stopMoving(MovementContext context) {
        removeRegistration(context);
    }

    @Override
    public boolean disableBlockEntityRendering() {
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices, MultiBufferSource buffer) {
        if (VisualizationManager.supportsVisualization(context.world)) {
            return;
        }

        float angle = getAnimationAngle(context, AnimationTickHolder.getPartialTicks(context.world));
        EndSculkSilencerRenderer.renderInContraption(context, renderWorld, matrices, buffer, angle);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public ActorVisual createVisual(VisualizationContext visualizationContext, VirtualRenderWorld simulationWorld, MovementContext movementContext) {
        return new EndSculkSilencerActorVisual(visualizationContext, simulationWorld, movementContext);
    }

    private static final class AnimationState {
        private final LerpedFloat speed = LerpedFloat.linear().startWithValue(0);
        private final LerpedFloat angle = LerpedFloat.angular().startWithValue(0);
    }
}
