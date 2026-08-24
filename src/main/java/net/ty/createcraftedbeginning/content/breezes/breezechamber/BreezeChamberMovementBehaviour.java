package net.ty.createcraftedbeginning.content.breezes.breezechamber;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.registry.CCBParticleTypes;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BreezeChamberMovementBehaviour implements MovementBehaviour {
    private static final String COMPOUND_KEY_CONDUCTOR = "Conductor";
    private static CameraEntityProvider cameraEntityProvider = context -> null;

    public static void setCameraEntityProvider(CameraEntityProvider provider) {
        cameraEntityProvider = provider;
    }

    private static LerpedFloat getHeadAngle(MovementContext context) {
        if (context.temporaryData instanceof LerpedFloat) {
            return (LerpedFloat) context.temporaryData;
        }

        context.temporaryData = LerpedFloat.angular().startWithValue(getTargetAngle(context));
        return (LerpedFloat) context.temporaryData;
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private static float getTargetAngle(MovementContext context) {
        if (shouldRenderHat(context) && !Mth.equal(context.relativeMotion.length(), 0) && context.contraption.entity instanceof CarriageContraptionEntity carriage) {
            float movementAngle = AngleHelper.deg(-Mth.atan2(context.relativeMotion.x, context.relativeMotion.z));
            return carriage.getInitialOrientation().getAxis() == Axis.X ? movementAngle + 180 : movementAngle;
        }

        Entity cameraEntity = cameraEntityProvider.getCameraEntity(context);
        if (cameraEntity == null || cameraEntity.isInvisible() || context.position == null) {
            return 0;
        }

        Vec3 cameraRelativePosition = context.contraption.entity.reverseRotation(cameraEntity.position().subtract(context.position), 1);
        return AngleHelper.deg(-Mth.atan2(cameraRelativePosition.z, cameraRelativePosition.x)) - 90;
    }

    private static boolean shouldRenderHat(MovementContext context) {
        CompoundTag movementData = context.data;
        if (movementData.contains(COMPOUND_KEY_CONDUCTOR)) {
            return movementData.getBoolean(COMPOUND_KEY_CONDUCTOR) && context.contraption.entity instanceof CarriageContraptionEntity carriage && carriage.hasSchedule();
        }

        movementData.putBoolean(COMPOUND_KEY_CONDUCTOR, determineIfConducting(context));
        return movementData.getBoolean(COMPOUND_KEY_CONDUCTOR) && context.contraption.entity instanceof CarriageContraptionEntity carriage && carriage.hasSchedule();
    }

    private static boolean determineIfConducting(MovementContext context) {
        Contraption contraption = context.contraption;
        if (!(contraption instanceof CarriageContraption carriageContraption)) {
            return false;
        }

        Direction assemblyDirection = carriageContraption.getAssemblyDirection();
        for (Direction controlDirection : Iterate.directionsInAxis(assemblyDirection.getAxis())) {
            if (!carriageContraption.inControl(context.localPos, controlDirection)) {
                continue;
            }

            return true;
        }
        return false;
    }

    @Override
    public void tick(MovementContext context) {
        if (!context.world.isClientSide()) {
            return;
        }

        RandomSource random = context.world.getRandom();
        Vec3 particlePos = context.position.add(VecHelper.offsetRandomly(Vec3.ZERO, random, 0.125f).multiply(1, 0, 1));
        if (random.nextInt(3) == 0 && context.motion.length() < 0.015625f) {
            context.world.addParticle(CCBParticleTypes.BREEZE_CLOUD.getParticleOptions(), particlePos.x, particlePos.y, particlePos.z, 0, 0, 0);
        }
        LerpedFloat headAngle = getHeadAngle(context);
        boolean shouldTurnQuickly = shouldRenderHat(context) && !Mth.equal(context.relativeMotion.length(), 0);
        float currentAngle = headAngle.getValue();
        float targetAngle = getTargetAngle(context);
        headAngle.chase(currentAngle + AngleHelper.getShortestAngleDiff(currentAngle, targetAngle), 0.5f, shouldTurnQuickly ? Chaser.EXP : Chaser.exp(5));
        headAngle.tickChaser();
    }

    @Override
    public @Nullable ItemStack canBeDisabledVia(MovementContext context) {
        return null;
    }

    @Override
    public boolean disableBlockEntityRendering() {
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices, MultiBufferSource buffer) {
        BreezeChamberRenderer.renderInContraption(context, matrices, buffer, getHeadAngle(context), shouldRenderHat(context), renderWorld);
    }

    @FunctionalInterface
    public interface CameraEntityProvider {
        @Nullable Entity getCameraEntity(MovementContext context);
    }
}
