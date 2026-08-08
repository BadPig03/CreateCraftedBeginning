package net.ty.createcraftedbeginning.content.airtights.handlers.cannon.ultrawarm;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
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
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.AirtightCannonUtils;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBDamageTypes;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class UltrawarmAirCannonHandler implements AirtightCannonHandler, AirtightCannonVisualHandler {
    protected static final float DEFAULT_RADIUS = 0.8f;
    protected static final int DEFAULT_DURATION = 40;
    protected static final int THRESHOLD = 300;

    protected static void addIgnition(List<LivingEntity> entities, int duration, float multiplier) {
        int ignitionTime = Math.round(duration * multiplier);
        for (LivingEntity entity : entities) {
            entity.igniteForTicks(Math.clamp(entity.getRemainingFireTicks() + ignitionTime, 0, Short.MAX_VALUE));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ItemStack getRenderIcon(Level level) {
        return new ItemStack(CCBItems.ULTRAWARM_WIND_CHARGE.asItem());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void renderTrailParticles(Level level, Vec3 pos) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < random.nextInt(2, 4); i++) {
            double offsetX = (random.nextDouble() - 0.5) * 0.6;
            double offsetY = (random.nextDouble() - 0.5) * 0.6;
            double offsetZ = (random.nextDouble() - 0.5) * 0.6;
            level.addParticle(ParticleTypes.FLAME, pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ, (random.nextDouble() - 0.5) * 0.02, random.nextDouble() * 0.02 + 0.01, (random.nextDouble() - 0.5) * 0.02);
            if (random.nextFloat() < 0.25f) {
                level.addParticle(ParticleTypes.SMOKE, pos.x, pos.y + 0.2, pos.z, 0, 0, 0);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceLocation getTextureLocation() {
        return CCBAPI.asResource("textures/entity/projectiles/ultrawarm_wind_charge.png");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CannonModelType getModelType() {
        return CannonModelType.CORE_ONLY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CannonAnimationType getAnimationType() {
        return CannonAnimationType.CORE_Y;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getRotationSpeed() {
        return 16;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void explode(Level level, Vec3 pos, AirtightCannonShotContext context) {
        float radius = DEFAULT_RADIUS * context.effectMultiplier();
        DamageSource explosionDamageSource = CCBDamageTypes.source(DamageTypes.ON_FIRE, level, context.projectile());
        level.explode(context.projectile(), explosionDamageSource, AirtightCannonUtils.createDamageCalculator(context.knockbackMultiplier()), pos.x(), pos.y(), pos.z(), radius, false, ExplosionInteraction.TRIGGER, ParticleTypes.GUST_EMITTER_SMALL, ParticleTypes.GUST_EMITTER_LARGE, SoundEvents.WIND_CHARGE_BURST);
        List<LivingEntity> entities = AirtightCannonUtils.getNearbyEntities(level, pos, radius, context);
        applyAdditionalEffects(level, entities, explosionDamageSource, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getGasConsumptionMultiplier() {
        return 0.9f;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void appendHoverText(ItemStack cannon, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(CCBLang.translate("gui.airtight_cannon.ultrawarm_air").style(ChatFormatting.DARK_GREEN).component());
    }

    protected void applyAdditionalEffects(Level level, List<LivingEntity> entities, DamageSource explosionDamageSource, AirtightCannonShotContext context) {
        addIgnition(entities, DEFAULT_DURATION, context.effectMultiplier());
    }
}
