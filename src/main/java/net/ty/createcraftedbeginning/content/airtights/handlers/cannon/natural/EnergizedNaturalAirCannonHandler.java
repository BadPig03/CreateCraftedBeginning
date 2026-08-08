package net.ty.createcraftedbeginning.content.airtights.handlers.cannon.natural;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonShotContext;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.AirtightCannonUtils;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EnergizedNaturalAirCannonHandler extends NaturalAirCannonHandler {
    private static final int ENERGIZED_BONUS_DAMAGE = 4;

    /**
     * {@inheritDoc}
     */
    @Override
    public ItemStack getRenderIcon(Level level) {
        return new ItemStack(CCBItems.ENERGIZED_NATURAL_WIND_CHARGE.asItem());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void renderTrailParticles(Level level, Vec3 pos) {
        super.renderTrailParticles(level, pos);
        RandomSource random = level.getRandom();
        for (int i = 0; i < random.nextInt(2, 4); i++) {
            level.addParticle(ParticleTypes.WHITE_ASH, pos.x, pos.y, pos.z, 0, 0, 0);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceLocation getTextureLocation() {
        return CCBAPI.asResource("textures/entity/projectiles/energized_natural_wind_charge.png");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getGasConsumptionMultiplier() {
        return 0.8f;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void appendHoverText(ItemStack cannon, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(CCBLang.translate("gui.airtight_cannon.energized_natural_air").style(ChatFormatting.DARK_GREEN).component());
    }

    @Override
    protected boolean hasAdditionalEffects() {
        return true;
    }

    @Override
    protected void applyAdditionalEffects(Level level, List<LivingEntity> entities, DamageSource explosionDamageSource, AirtightCannonShotContext context) {
        float baseBonusDamage = ENERGIZED_BONUS_DAMAGE * context.effectMultiplier();
        AirtightCannonUtils.applyBonusDamage(entities, explosionDamageSource, entity -> entity.hasEffect(MobEffects.WIND_CHARGED) ? baseBonusDamage * 2 : baseBonusDamage);
    }
}
