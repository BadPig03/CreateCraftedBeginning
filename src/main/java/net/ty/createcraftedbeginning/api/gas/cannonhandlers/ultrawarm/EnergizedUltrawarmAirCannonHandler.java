package net.ty.createcraftedbeginning.api.gas.cannonhandlers.ultrawarm;

import net.minecraft.ChatFormatting;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.AirtightCannonUtils;
import net.ty.createcraftedbeginning.registry.CCBDamageTypes;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EnergizedUltrawarmAirCannonHandler extends UltrawarmAirCannonHandler {
    @Override
    public ItemStack getRenderIcon(Level level) {
        return new ItemStack(CCBItems.ENERGIZED_ULTRAWARM_WIND_CHARGE.asItem());
    }

    @Override
    public void renderTrailParticles(@NotNull Level level, @NotNull Vec3 pos) {
        RandomSource random = level.getRandom();
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;
        for (int i = 0; i < random.nextInt(2, 4); i++) {
            double offsetX = (random.nextDouble() - 0.5) * 0.6;
            double offsetY = (random.nextDouble() - 0.5) * 0.6;
            double offsetZ = (random.nextDouble() - 0.5) * 0.6;
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x + offsetX, y + offsetY, z + offsetZ, (random.nextDouble() - 0.5) * 0.02, random.nextDouble() * 0.02 + 0.01, (random.nextDouble() - 0.5) * 0.02);
            level.addParticle(ParticleTypes.WARPED_SPORE, x, y, z, 0, 0, 0);

            if (random.nextFloat() < 0.25f) {
                level.addParticle(ParticleTypes.SMOKE, x, y + 0.2, z, 0, 0, 0);
            }
        }
    }

    @Override
    public void explode(@NotNull Level level, @NotNull Vec3 pos, Entity source, float multiplier) {
        super.explode(level, pos, source, multiplier);

        List<LivingEntity> entities = AirtightCannonUtils.getNearbyEntities(level, pos, DEFAULT_RADIUS * multiplier);
        if (entities.isEmpty()) {
            return;
        }

        DamageSource damageSource = CCBDamageTypes.source(DamageTypes.ON_FIRE, level, source);
        float baseDamageAmount = DEFAULT_DAMAGE * multiplier;
        for (LivingEntity entity : entities) {
            float damageAmount = baseDamageAmount;
            if (entity.getRemainingFireTicks() >= THRESHOLD) {
                damageAmount *= 2;
            }
            entity.hurt(damageSource, damageAmount);
        }
    }

    @Override
    public ResourceLocation getTextureLocation() {
        return CreateCraftedBeginning.asResource("textures/entity/projectiles/energized_ultrawarm_wind_charge.png");
    }

    @Override
    public LayerDefinition getLayerDefinition() {
        return super.getLayerDefinition();
    }

    @Override
    public float[] getSetupAnim(float ageInTicks) {
        return super.getSetupAnim(ageInTicks);
    }

    @Override
    public float getGasConsumptionMultiplier() {
        return 0.72f;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack cannon, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(CCBLang.translate("gui.tooltips.airtight_cannon.energized_ultrawarm_air").style(ChatFormatting.DARK_GREEN).component());
    }
}
