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
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class EndSculkSilencerMovementBehaviour implements MovementBehaviour {
    private static final short MOVING_RANGE = 1;

    static float getAnimationAngle(MovementContext context, float partialTicks) {
        return getAnimationState(context).angle.getValue(partialTicks);
    }

    private static AnimationState getAnimationState(MovementContext context) {
        if (context.temporaryData instanceof AnimationState animationState) {
            return animationState;
        }

        AnimationState animationState = new AnimationState();
        context.temporaryData = animationState;
        return animationState;
    }

    private static void tickAnimation(MovementContext context) {
        AnimationState animationState = getAnimationState(context);
        float targetAnimationSpeed = context.disabled ? 0 : EndSculkSilencerBlockEntity.calculateAnimationTargetSpeed(SpeedLevel.FAST.getSpeedValue());
        animationState.speed.chase(targetAnimationSpeed, context.disabled ? 0.2 : 0.1, Chaser.EXP);
        animationState.speed.tickChaser();
        animationState.angle.setValue(animationState.angle.getValue() + animationState.speed.getValue());
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
        long registrationSeed = contraptionId.getMostSignificantBits();
        registrationSeed ^= Long.rotateLeft(contraptionId.getLeastSignificantBits(), 23);
        registrationSeed ^= Long.rotateLeft(context.localPos.asLong(), 41);
        long registrationHash = mix64(registrationSeed);
        return new BlockPos(unpackSignedCoordinate(registrationHash), -2048, unpackSignedCoordinate(registrationHash >>> 26));
    }

    private static int unpackSignedCoordinate(long packedCoordinate) {
        int coordinate = (int) (packedCoordinate & 0x3FFFFFF);
        if (coordinate < 0x2000000) {
            return coordinate;
        }
        return coordinate - 0x4000000;
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

        BlockPos effectCenter = SubLevelBridge.resolve(serverLevel, context.position).blockPos();
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
