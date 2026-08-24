package net.ty.createcraftedbeginning.content.end.endincinerationblower;

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
public final class EndIncinerationBlowerEffectProcessor {
    private final EndIncinerationBlowerBlockEntity blower;
    private final EndIncinerationBlowerTargetCache targetCache;

    public EndIncinerationBlowerEffectProcessor(EndIncinerationBlowerBlockEntity blower) {
        this.blower = blower;
        targetCache = new EndIncinerationBlowerTargetCache(blower.getBlockPos());
    }

    private static boolean applyFanProcessing(ServerLevel level, FanProcessingType processingType, AABB effectArea, EntityArea entityArea, List<ItemEntity> affectedItems, List<TransportedItemStackHandlerBehaviour> transportedHandlers) {
        AtomicBoolean hasProcessedItem = new AtomicBoolean(false);
        for (Iterator<ItemEntity> itemIterator = affectedItems.iterator(); itemIterator.hasNext(); ) {
            ItemEntity itemEntity = itemIterator.next();
            if (!itemEntity.isAlive() || itemEntity.isRemoved()) {
                itemIterator.remove();
                continue;
            }

            if (!entityArea.intersects(itemEntity) || !FanProcessing.canProcess(itemEntity, processingType) || !FanProcessing.applyProcessing(itemEntity, processingType)) {
                continue;
            }

            hasProcessedItem.set(true);
        }

        for (Iterator<TransportedItemStackHandlerBehaviour> handlerIterator = transportedHandlers.iterator(); handlerIterator.hasNext(); ) {
            TransportedItemStackHandlerBehaviour handler = handlerIterator.next();
            if (handler.blockEntity.isRemoved() || handler.blockEntity.getLevel() != level) {
                handlerIterator.remove();
                continue;
            }

            handler.handleProcessingOnAllItems(transportedItem -> {
                if (!effectArea.contains(handler.getWorldPositionOf(transportedItem))) {
                    return TransportedResult.doNothing();
                }

                TransportedResult processingResult = FanProcessing.applyProcessing(transportedItem, level, processingType);
                if (!processingResult.doesNothing()) {
                    hasProcessedItem.set(true);
                }
                return processingResult;
            });
        }

        return hasProcessedItem.get();
    }

    public void tick(ServerLevel level) {
        float absSpeed = Mth.abs(blower.getSpeed());
        EndIncinerationBlowerStructuralBlockEntity structural = blower.getStructuralForEffect();
        if (EndIncinerationBlowerRange.calculateRange(absSpeed) <= 0 || structural == null) {
            return;
        }

        AABB effectArea = EndIncinerationBlowerRange.calculateArea(blower.getBlockPos(), absSpeed);
        boolean effectApplied = switch (structural.getBlowerWorkingMode().get()) {
            case SMOKING -> applyFanProcessing(level, AllFanProcessingTypes.SMOKING, effectArea);
            case BLASTING -> applyFanProcessing(level, AllFanProcessingTypes.BLASTING, effectArea);
            case IGNITION -> shouldApplyIgnition(level) && applyIgnition(level, effectArea);
        };
        if (!effectApplied) {
            return;
        }

        blower.awardPrimaryEffectAdvancement();
    }

    private boolean applyFanProcessing(ServerLevel level, FanProcessingType processingType, AABB effectArea) {
        EntityArea entityArea = CCBSubLevelBridge.createEntityArea(level, blower.getBlockPos(), effectArea);
        return applyFanProcessing(level, processingType, effectArea, entityArea, targetCache.getAffectedItems(level, effectArea, entityArea), targetCache.getTransportedHandlers(level, blower.getSpeed()));
    }

    private boolean applyIgnition(ServerLevel level, AABB effectArea) {
        EntityArea entityArea = CCBSubLevelBridge.createEntityArea(level, blower.getBlockPos(), effectArea);
        FakePlayer fakePlayer = blower.getFakePlayer(level);
        boolean effectApplied = false;
        boolean shouldAffectPlayers = CCBConfig.server().endDevices.ignitionAffectsPlayers.get();
        float configuredDamage = Math.max(0, CCBConfig.server().endDevices.ignitionDamage.getF());
        DamageSource damageSource = CCBDamageTypes.source(DamageTypes.IN_FIRE, level, fakePlayer);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, effectArea)) {
            if (!entityArea.intersects(target) || !target.isAlive() || target.fireImmune() || target instanceof Player && !shouldAffectPlayers) {
                continue;
            }

            int previousFireTicks = target.getRemainingFireTicks();
            target.igniteForSeconds(2);
            boolean targetAffected = target.getRemainingFireTicks() > previousFireTicks;

            boolean isSnowGolem = target instanceof SnowGolem;
            float damageAmount = isSnowGolem ? target.getHealth() + target.getAbsorptionAmount() + 1 : configuredDamage;
            if (damageAmount > 0 && target.hurt(damageSource, damageAmount)) {
                targetAffected = true;
                if (isSnowGolem && !target.isAlive()) {
                    blower.awardWarmHeartedAdvancement();
                }
            }
            effectApplied |= targetAffected;
        }
        return effectApplied;
    }

    private boolean shouldApplyIgnition(ServerLevel level) {
        return Math.floorMod(level.getGameTime(), 20) == Math.floorMod(blower.getBlockPos().hashCode(), 20);
    }
}
