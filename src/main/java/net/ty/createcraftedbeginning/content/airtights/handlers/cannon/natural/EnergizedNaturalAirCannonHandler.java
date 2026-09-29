package net.ty.createcraftedbeginning.content.airtights.handlers.cannon.natural;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonShotContext;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.AirtightCannonBlast;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBParticleTypes;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EnergizedNaturalAirCannonHandler extends NaturalAirCannonHandler {
    private static final int ENERGIZED_BONUS_DAMAGE = 4;

    @Override
    public ItemStack getRenderIcon(Level level) {
        return new ItemStack(CCBItems.ENERGIZED_NATURAL_WIND_CHARGE.asItem());
    }

    @Override
    public void renderTrailParticles(Level level, Vec3 pos, Vec3 velocity) {
        level.addParticle(CCBParticleTypes.BREEZE_CLOUD.getParticleOptions(), pos.x, pos.y, pos.z, 0, 0, 0);
        RandomSource random = level.getRandom();
        for (int i = 0; i < random.nextInt(3, 5); i++) {
            level.addParticle(ParticleTypes.WHITE_ASH, pos.x, pos.y, pos.z, 0, 0, 0);
        }
    }

    @Override
    public ResourceLocation getTextureLocation() {
        return CCBAPI.asResource("textures/entity/projectiles/energized_natural_wind_charge.png");
    }

    @Override
    public float getRotationSpeed() {
        return super.getRotationSpeed() * 2;
    }

    @Override
    public float getGasConsumptionMultiplier() {
        return super.getGasConsumptionMultiplier() * 0.75F;
    }

    @Override
    public void appendHoverText(ItemStack cannon, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(CCBLang.translate("gui.airtight_cannon.energized_natural_air").style(ChatFormatting.DARK_GREEN).component());
    }

    @Override
    public void applyAdditionalEffects(Level level, List<LivingEntity> entities, DamageSource explosionDamageSource, AirtightCannonShotContext context) {
        super.applyAdditionalEffects(level, entities, explosionDamageSource, context);
        float multiplier = context.effectMultiplier();
        int duration = Math.round(DEFAULT_DURATION * multiplier);
        float baseBonusDamage = ENERGIZED_BONUS_DAMAGE * multiplier;
        AirtightCannonBlast.applyEffects(entities, () -> new MobEffectInstance(MobEffects.WIND_CHARGED, duration, 0));
        AirtightCannonBlast.applyBonusDamage(entities, explosionDamageSource, entity -> baseBonusDamage);
    }
}
