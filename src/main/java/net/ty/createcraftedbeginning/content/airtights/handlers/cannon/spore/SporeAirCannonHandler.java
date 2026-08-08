package net.ty.createcraftedbeginning.content.airtights.handlers.cannon.spore;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageTypes;
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
public class SporeAirCannonHandler implements AirtightCannonHandler, AirtightCannonVisualHandler {
    private static final float DEFAULT_RADIUS = 1.2f;

    /**
     * {@inheritDoc}
     */
    @Override
    public ItemStack getRenderIcon(Level level) {
        return new ItemStack(CCBItems.SPORE_WIND_CHARGE.asItem());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void renderTrailParticles(Level level, Vec3 pos) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceLocation getTextureLocation() {
        return CCBAPI.asResource("textures/entity/projectiles/spore_wind_charge.png");
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
    public void explode(Level level, Vec3 pos, AirtightCannonShotContext context) {
        float radius = DEFAULT_RADIUS * context.effectMultiplier();
        level.explode(context.projectile(), CCBDamageTypes.source(DamageTypes.OUTSIDE_BORDER, level, context.projectile()), AirtightCannonUtils.createDamageCalculator(context.knockbackMultiplier()), pos.x(), pos.y(), pos.z(), radius, false, ExplosionInteraction.TRIGGER, ParticleTypes.GUST_EMITTER_SMALL, ParticleTypes.GUST_EMITTER_LARGE, SoundEvents.WIND_CHARGE_BURST);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getGasConsumptionMultiplier() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void appendHoverText(ItemStack cannon, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(CCBLang.translate("gui.airtight_cannon.spore_air").style(ChatFormatting.DARK_GREEN).component());
    }
}
