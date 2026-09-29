package net.ty.createcraftedbeginning.content.airtights.airtighthandhelddrill;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.canister.GasConsumptionMath;
import net.ty.createcraftedbeginning.api.drillhandlers.AirtightDrillHandler;
import net.ty.createcraftedbeginning.api.drillhandlers.AirtightDrillHandlers;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerConsumers;
import net.ty.createcraftedbeginning.content.airtights.gascanister.container.CanisterContainerConsumers.AffordableFuel;
import net.ty.createcraftedbeginning.gas.interaction.GasInteractionFeedback;
import net.ty.createcraftedbeginning.registry.CCBDamageTypes;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightHandheldDrillAttack {
    private AirtightHandheldDrillAttack() {
    }

    static void doDrillAttack(Player player, Level level) {
        double attackRange = player.blockInteractionRange();
        Vec3 eyePosition = player.getEyePosition();
        Vec3 viewVector = player.calculateViewVector(player.getXRot(), player.getYRot());
        int perEntityHitConsumption = CCBConfig.server().equipment.airtightHandheldDrill.gasPerEntityHit.get();
        DamageSource damageSource = CCBDamageTypes.source(DamageTypes.THORNS, level, player);
        List<LivingEntity> vulnerableEntities = getVulnerableEntities(player, level, damageSource, attackRange, eyePosition, viewVector);
        if (vulnerableEntities.isEmpty()) {
            return;
        }

        vulnerableEntities.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(player)));
        Optional<AffordableFuel> affordableFuel = CanisterContainerConsumers.findAffordableFuel(player, context -> {
            AirtightDrillHandler drillHandler = AirtightDrillHandlers.resolveForEquipment(context.gasType());
            return (double) perEntityHitConsumption * drillHandler.getConsumptionMultiplier() * vulnerableEntities.size();
        });
        if (affordableFuel.isEmpty()) {
            AirtightHandheldDrillFuel.displayInsufficientGasWarning(player);
            return;
        }

        AffordableFuel selectedFuel = affordableFuel.get();
        AirtightDrillHandler drillHandler = AirtightDrillHandlers.resolveForEquipment(selectedFuel.gasType());
        int successfulHits = 0;
        for (LivingEntity entity : vulnerableEntities) {
            int damageAmount = AirtightDrillHandler.BASE_DAMAGE_AMOUNT + drillHandler.getDamageAddition();
            if (!entity.hurt(damageSource, damageAmount)) {
                continue;
            }

            successfulHits++;
            if (!(level instanceof ServerLevel serverLevel)) {
                continue;
            }

            drillHandler.extraBehaviour(entity, player, serverLevel);
        }

        if (successfulHits == 0) {
            return;
        }

        long gasConsumption = GasConsumptionMath.roundUp((double) perEntityHitConsumption * drillHandler.getConsumptionMultiplier() * successfulHits);
        if (CanisterContainerConsumers.interactContainer(player, new AffordableFuel(selectedFuel.gasContent(), selectedFuel.sourcePressurePa(), gasConsumption), () -> true, false)) {
            return;
        }

        GasInteractionFeedback.sendWarningFeedback(player, "gui.warnings.insufficient_gas", selectedFuel.gasContent().getHoverName());
    }

    private static List<LivingEntity> getVulnerableEntities(Player player, Level level, DamageSource damageSource, double range, Vec3 eyePosition, Vec3 viewVector) {
        List<LivingEntity> vulnerableEntities = new ArrayList<>();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range, range, range))) {
            if (entity.is(player)) {
                continue;
            }

            if (entity.isInvulnerableTo(damageSource) || entity.isInvulnerable()) {
                continue;
            }

            Vec3 directionToEntity = entity.position().subtract(eyePosition);
            if (directionToEntity.length() > range) {
                continue;
            }

            if (viewVector.dot(directionToEntity.normalize()) < 0.5) {
                continue;
            }

            if (!hasClearAttackLine(player, entity, level)) {
                continue;
            }

            vulnerableEntities.add(entity);
        }
        return vulnerableEntities;
    }

    private static boolean hasClearAttackLine(Player player, LivingEntity entity, Level level) {
        Vec3 eyePosition = player.getEyePosition();
        Vec3 targetCenter = entity.getBoundingBox().getCenter();
        BlockHitResult blockHit = level.clip(new ClipContext(eyePosition, targetCenter, Block.COLLIDER, Fluid.NONE, player));
        return blockHit.getType() == Type.MISS || blockHit.getLocation().distanceToSqr(eyePosition) >= targetCenter.distanceToSqr(eyePosition) - 0.25;
    }
}
