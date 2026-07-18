package net.ty.createcraftedbeginning.api.cannonhandlers.ultrawarm;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonShotContext;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.AirtightCannonUtils;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EnergizedUltrawarmAirCannonHandler extends UltrawarmAirCannonHandler {
    private static final int ENERGIZED_BONUS_DAMAGE = 3;

    /**
     * {@inheritDoc}
     */
    @Override
    public ItemStack getRenderIcon(Level level) {
        return new ItemStack(CCBItems.ENERGIZED_ULTRAWARM_WIND_CHARGE.asItem());
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
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ, (random.nextDouble() - 0.5) * 0.02, random.nextDouble() * 0.02 + 0.01, (random.nextDouble() - 0.5) * 0.02);
            level.addParticle(ParticleTypes.WARPED_SPORE, pos.x, pos.y, pos.z, 0, 0, 0);
            if (random.nextFloat() < 0.25f) {
                level.addParticle(ParticleTypes.SMOKE, pos.x, pos.y + 0.2, pos.z, 0, 0, 0);
            }
        }
    }

    @Override
    protected void applyAdditionalEffects(Level level, List<LivingEntity> entities, DamageSource explosionDamageSource, AirtightCannonShotContext context) {
        super.applyAdditionalEffects(level, entities, explosionDamageSource, context);
        float baseBonusDamage = ENERGIZED_BONUS_DAMAGE * context.effectMultiplier();
        AirtightCannonUtils.applyBonusDamage(entities, explosionDamageSource, entity -> entity.getRemainingFireTicks() >= THRESHOLD ? baseBonusDamage * 2 : baseBonusDamage);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceLocation getTextureLocation() {
        return CreateCraftedBeginning.asResource("textures/entity/projectiles/energized_ultrawarm_wind_charge.png");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getGasConsumptionMultiplier() {
        return 0.72f;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void appendHoverText(ItemStack cannon, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(CCBLang.translate("gui.airtight_cannon.energized_ultrawarm_air").style(ChatFormatting.DARK_GREEN).component());
    }
}
