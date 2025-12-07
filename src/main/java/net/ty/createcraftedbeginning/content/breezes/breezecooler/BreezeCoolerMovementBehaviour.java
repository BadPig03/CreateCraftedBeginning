package net.ty.createcraftedbeginning.content.breezes.breezecooler;

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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

public class BreezeCoolerMovementBehaviour implements MovementBehaviour {
    private static final String COMPOUND_KEY_CONDUCTOR = "Conductor";

    private static LerpedFloat getHeadAngle(@NotNull MovementContext context) {
        if (!(context.temporaryData instanceof LerpedFloat)) {
            context.temporaryData = LerpedFloat.angular().startWithValue(getTargetAngle(context));
        }
        return (LerpedFloat) context.temporaryData;
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private static float getTargetAngle(MovementContext context) {
        if (shouldRenderHat(context) && !Mth.equal(context.relativeMotion.length(), 0) && context.contraption.entity instanceof CarriageContraptionEntity cce) {
            float angle = AngleHelper.deg(-Mth.atan2(context.relativeMotion.x, context.relativeMotion.z));
            return cce.getInitialOrientation().getAxis() == Axis.X ? angle + 180 : angle;
        }

        Entity player = Minecraft.getInstance().cameraEntity;
        if (player == null || player.isInvisible() || context.position == null) {
            return 0;
        }

        Vec3 applyRotation = context.contraption.entity.reverseRotation(player.position().subtract(context.position), 1);
        return AngleHelper.deg(-Mth.atan2(applyRotation.z, applyRotation.x)) - 90;
    }

    private static boolean shouldRenderHat(@NotNull MovementContext context) {
        CompoundTag compoundTag = context.data;
        if (!compoundTag.contains(COMPOUND_KEY_CONDUCTOR)) {
            compoundTag.putBoolean(COMPOUND_KEY_CONDUCTOR, determineIfConducting(context));
        }
        return compoundTag.getBoolean(COMPOUND_KEY_CONDUCTOR) && context.contraption.entity instanceof CarriageContraptionEntity cce && cce.hasSchedule();
    }

    private static boolean determineIfConducting(@NotNull MovementContext context) {
        Contraption contraption = context.contraption;
        if (!(contraption instanceof CarriageContraption carriageContraption)) {
            return false;
        }

        Direction assemblyDirection = carriageContraption.getAssemblyDirection();
        for (Direction direction : Iterate.directionsInAxis(assemblyDirection.getAxis())) {
            if (!carriageContraption.inControl(context.localPos, direction)) {
                continue;
            }

            return true;
        }

        return false;
    }

    @Override
    public void tick(@NotNull MovementContext context) {
        if (!context.world.isClientSide()) {
            return;
        }

        RandomSource random = context.world.getRandom();
        Vec3 position = context.position;
        Vec3 added = position.add(VecHelper.offsetRandomly(Vec3.ZERO, random, 0.125f).multiply(1, 0, 1));
        if (random.nextInt(3) == 0 && context.motion.length() < 0.015625f) {
            context.world.addParticle(ParticleTypes.SNOWFLAKE, added.x, added.y, added.z, 0, 0, 0);
        }
        LerpedFloat headAngle = getHeadAngle(context);
        boolean quickTurn = shouldRenderHat(context) && !Mth.equal(context.relativeMotion.length(), 0);
        headAngle.chase(headAngle.getValue() + AngleHelper.getShortestAngleDiff(headAngle.getValue(), getTargetAngle(context)), 0.5f, quickTurn ? Chaser.EXP : Chaser.exp(5));
        headAngle.tickChaser();
    }

    @Override
    public ItemStack canBeDisabledVia(MovementContext context) {
        return null;
    }

    @Override
    public boolean disableBlockEntityRendering() {
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices, MultiBufferSource buffer) {
        BreezeCoolerRenderer.renderInContraption(context, matrices, buffer, getHeadAngle(context), shouldRenderHat(context));
    }
}
