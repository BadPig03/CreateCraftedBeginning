package net.ty.createcraftedbeginning.content.end.endincinerationblower;

import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessing;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.platform.CCBSubLevelBridge;
import net.ty.createcraftedbeginning.platform.CCBSubLevelBridge.EntityArea;
import net.ty.createcraftedbeginning.registry.CCBDamageTypes;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class EndIncinerationBlowerEffectProcessor {
    private final EndIncinerationBlowerBlockEntity blockEntity;
    private final EndIncinerationBlowerTargetCache targetCache;

    EndIncinerationBlowerEffectProcessor(EndIncinerationBlowerBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
        targetCache = new EndIncinerationBlowerTargetCache(blockEntity.getBlockPos());
    }

    private static boolean applyFanProcessing(ServerLevel level, FanProcessingType processingType, EntityArea entityArea, List<ItemEntity> affectedItems, List<TransportedItemStackHandlerBehaviour> transportedHandlers) {
        AtomicBoolean applied = new AtomicBoolean(false);
        for (Iterator<ItemEntity> iterator = affectedItems.iterator(); iterator.hasNext(); ) {
            ItemEntity itemEntity = iterator.next();
            if (!itemEntity.isAlive() || itemEntity.isRemoved()) {
                iterator.remove();
                continue;
            }

            if (!entityArea.intersects(itemEntity) || !FanProcessing.canProcess(itemEntity, processingType)) {
                continue;
            }

            if (FanProcessing.applyProcessing(itemEntity, processingType)) {
                applied.set(true);
            }
        }

        for (Iterator<TransportedItemStackHandlerBehaviour> iterator = transportedHandlers.iterator(); iterator.hasNext(); ) {
            TransportedItemStackHandlerBehaviour behaviour = iterator.next();
            if (behaviour.blockEntity.isRemoved() || behaviour.blockEntity.getLevel() != level) {
                iterator.remove();
                continue;
            }

            behaviour.handleProcessingOnAllItems(transported -> {
                TransportedResult result = FanProcessing.applyProcessing(transported, level, processingType);
                if (!result.doesNothing()) {
                    applied.set(true);
                }
                return result;
            });
        }

        return applied.get();
    }

    void tick(ServerLevel level) {
        float absSpeed = Mth.abs(blockEntity.getSpeed());
        EndIncinerationBlowerStructuralBlockEntity structural = blockEntity.getStructuralForEffect();
        if (absSpeed < SpeedLevel.MEDIUM.getSpeedValue() || structural == null) {
            return;
        }

        AABB area = EndIncinerationBlowerRange.calculateArea(blockEntity.getBlockPos(), absSpeed);
        boolean applied = switch (structural.getBlowerWorkingMode().get()) {
            case SMOKING -> applyFanProcessing(level, AllFanProcessingTypes.SMOKING, area);
            case BLASTING -> applyFanProcessing(level, AllFanProcessingTypes.BLASTING, area);
            case IGNITION -> shouldApplyIgnition(level) && applyIgnition(level, area);
        };
        if (!applied) {
            return;
        }

        blockEntity.awardPrimaryEffectAdvancement();
    }

    private boolean applyFanProcessing(ServerLevel level, FanProcessingType processingType, AABB area) {
        EntityArea entityArea = CCBSubLevelBridge.createEntityArea(level, blockEntity.getBlockPos(), area);
        return applyFanProcessing(level, processingType, entityArea, targetCache.getAffectedItems(level, area, entityArea), targetCache.getTransportedHandlers(level, blockEntity.getSpeed()));
    }

    private boolean applyIgnition(ServerLevel level, AABB area) {
        EntityArea entityArea = CCBSubLevelBridge.createEntityArea(level, blockEntity.getBlockPos(), area);
        FakePlayer fakePlayer = blockEntity.getFakePlayer(level);
        boolean applied = false;
        boolean affectsPlayers = CCBConfig.server().endDevices.ignitionAffectsPlayers.get();
        float configuredDamage = Math.max(0, CCBConfig.server().endDevices.ignitionDamage.getF());
        DamageSource damageSource = CCBDamageTypes.source(DamageTypes.IN_FIRE, level, fakePlayer);
        for (LivingEntity livingEntity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (!entityArea.intersects(livingEntity) || !livingEntity.isAlive() || livingEntity.fireImmune() || livingEntity instanceof Player && !affectsPlayers) {
                continue;
            }

            boolean snowGolem = livingEntity instanceof SnowGolem;
            float damage = snowGolem ? livingEntity.getHealth() + livingEntity.getAbsorptionAmount() + 1 : configuredDamage;
            if (damage <= 0) {
                continue;
            }

            int previousFireTicks = livingEntity.getRemainingFireTicks();
            livingEntity.igniteForSeconds(2);
            if (!livingEntity.hurt(damageSource, damage)) {
                livingEntity.setRemainingFireTicks(previousFireTicks);
                continue;
            }

            applied = true;
            if (snowGolem && !livingEntity.isAlive()) {
                blockEntity.awardWarmHeartedAdvancement();
            }
        }
        return applied;
    }

    private boolean shouldApplyIgnition(ServerLevel level) {
        return Math.floorMod(level.getGameTime(), 20) == Math.floorMod(blockEntity.getBlockPos().hashCode(), 20);
    }
}
