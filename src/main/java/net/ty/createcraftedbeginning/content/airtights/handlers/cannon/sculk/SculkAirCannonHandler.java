package net.ty.createcraftedbeginning.content.airtights.handlers.cannon.sculk;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
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
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SculkAirCannonHandler implements AirtightCannonHandler, AirtightCannonVisualHandler {
    @Override
    public ItemStack getRenderIcon(Level level) {
        return new ItemStack(CCBItems.SCULK_WIND_CHARGE.asItem());
    }

    @Override
    public void renderTrailParticles(Level level, Vec3 pos, Vec3 velocity) {
        level.addParticle(ParticleTypes.SCULK_CHARGE_POP, pos.x, pos.y, pos.z, 0, 0, 0);
        level.addParticle(ParticleTypes.OMINOUS_SPAWNING, pos.x, pos.y, pos.z, 0, 0, 0);
        level.addParticle(ParticleTypes.SONIC_BOOM, pos.x, pos.y, pos.z, 0, 0, 0);
    }

    @Override
    public ResourceLocation getTextureLocation() {
        return CCBAPI.asResource("textures/entity/projectiles/sculk_wind_charge.png");
    }

    @Override
    public CannonModelType getModelType() {
        return CannonModelType.CORE_ONLY;
    }

    @Override
    public CannonAnimationType getAnimationType() {
        return CannonAnimationType.ONLY_CORE;
    }

    @Override
    public float getRotationSpeed() {
        return 16;
    }

    @Override
    public void explode(Level level, Vec3 pos, AirtightCannonShotContext context) {
        float multiplier = context.effectMultiplier() * 0.5F;
        DamageSource explosionDamageSource = CCBDamageTypes.source(DamageTypes.SONIC_BOOM, level, context.projectile(), context.owner());
        level.explode(context.projectile(), explosionDamageSource, AirtightCannonBlast.createDamageCalculator(context), pos.x(), pos.y(), pos.z(), multiplier, false, ExplosionInteraction.TRIGGER, ParticleTypes.GUST_EMITTER_SMALL, ParticleTypes.GUST_EMITTER_LARGE, SoundEvents.WIND_CHARGE_BURST);
    }

    @Override
    public float getGasConsumptionMultiplier() {
        return 1;
    }

    @Override
    public void appendHoverText(ItemStack cannon, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(CCBLang.translate("gui.airtight_cannon.sculk_air").style(ChatFormatting.DARK_GREEN).component());
    }

    @Override
    public float getDirectHitDamage() {
        return 6;
    }

    @Override
    public DamageSource createDirectHitDamageSource(Entity projectile, @Nullable LivingEntity owner) {
        return new DamageSource(projectile.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.SONIC_BOOM), projectile, owner);
    }
}
