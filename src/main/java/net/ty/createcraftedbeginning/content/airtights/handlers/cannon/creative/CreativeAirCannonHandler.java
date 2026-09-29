package net.ty.createcraftedbeginning.content.airtights.handlers.cannon.creative;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonHandler;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonShotContext;
import net.ty.createcraftedbeginning.api.cannonhandlers.visual.AirtightCannonVisualHandler;
import net.ty.createcraftedbeginning.api.cannonhandlers.visual.CannonAnimationType;
import net.ty.createcraftedbeginning.api.cannonhandlers.visual.CannonModelType;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.AirtightCannonBlast;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBDamageTypes;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CreativeAirCannonHandler implements AirtightCannonHandler, AirtightCannonVisualHandler {
    @Override
    public ItemStack getRenderIcon(Level level) {
        return new ItemStack(CCBItems.CREATIVE_WIND_CHARGE.asItem());
    }

    @Override
    public void renderTrailParticles(Level level, Vec3 pos, Vec3 velocity) {
    }

    @Override
    public ResourceLocation getTextureLocation() {
        return CCBAPI.asResource("textures/entity/projectiles/creative_wind_charge.png");
    }

    @Override
    public CannonModelType getModelType() {
        return CannonModelType.WITH_WIND;
    }

    @Override
    public CannonAnimationType getAnimationType() {
        return CannonAnimationType.WITH_WIND_Y;
    }

    @Override
    public float getRotationSpeed() {
        return 24;
    }

    @Override
    public void explode(Level level, Vec3 pos, AirtightCannonShotContext context) {
        float multiplier = context.effectMultiplier();
        DamageSource explosionDamageSource = CCBDamageTypes.source(DamageTypes.WIND_CHARGE, level, context.projectile(), context.owner());
        level.explode(context.projectile(), explosionDamageSource, AirtightCannonBlast.createDamageCalculator(context), pos.x(), pos.y(), pos.z(), multiplier, false, ExplosionInteraction.TRIGGER, ParticleTypes.GUST_EMITTER_SMALL, ParticleTypes.GUST_EMITTER_LARGE, SoundEvents.WIND_CHARGE_BURST);
        List<LivingEntity> entities = AirtightCannonBlast.getNearbyEntities(level, pos, multiplier, context);
        applyAdditionalEffects(level, entities, explosionDamageSource, context);
    }

    @Override
    public float getGasConsumptionMultiplier() {
        return 0;
    }

    @Override
    public void appendHoverText(ItemStack cannon, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(CCBLang.translate("gui.airtight_cannon.creative_air").style(ChatFormatting.DARK_GREEN).component());
    }

    @Override
    public void applyAdditionalEffects(Level level, List<LivingEntity> entities, DamageSource explosionDamageSource, AirtightCannonShotContext context) {
        AirtightCannonBlast.applyBonusDamage(entities, explosionDamageSource, Integer.MAX_VALUE);
    }
}
