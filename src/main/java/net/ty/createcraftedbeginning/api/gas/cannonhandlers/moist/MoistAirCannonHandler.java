package net.ty.createcraftedbeginning.api.gas.cannonhandlers.moist;

import net.minecraft.ChatFormatting;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.cannonhandlers.AirtightCannonHandler;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.AirtightCannonUtils;
import net.ty.createcraftedbeginning.registry.CCBDamageTypes;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.registry.CCBParticleTypes;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MoistAirCannonHandler implements AirtightCannonHandler {
    protected static final float DEFAULT_RADIUS = 1.2f;
    protected static final int DEFAULT_DAMAGE = 5;
    protected static final int ROTATION_SPEED = 16;
    protected static final String NAME_BONE = "bone";
    protected static final String NAME_WIND_OUTER = "wind_outer";
    protected static final String NAME_WIND_INNER = "wind_inner";
    protected static final String NAME_CORE = "core";

    @Override
    public ItemStack getRenderIcon(Level level) {
        return new ItemStack(CCBItems.MOIST_WIND_CHARGE.asItem());
    }

    @Override
    public void renderTrailParticles(@NotNull Level level, @NotNull Vec3 pos) {
        double x = pos.x;
        double y = pos.y;
        double z = pos.z;
        level.addParticle(ParticleTypes.DOLPHIN, x, y, z, 0, 0, 0);

        RandomSource random = level.getRandom();
        for (int i = 0; i < random.nextInt(2, 4); i++) {
            double offsetX = (random.nextDouble() - 0.5) * 0.6;
            double offsetY = (random.nextDouble() - 0.5) * 0.6;
            double offsetZ = (random.nextDouble() - 0.5) * 0.6;
            level.addParticle(CCBParticleTypes.BUBBLE_WITHOUT_WATER.getParticleOptions(), x + offsetX, y + offsetY, z + offsetZ, (random.nextDouble() - 0.5) * 0.02, random.nextDouble() * 0.02 + 0.01, (random.nextDouble() - 0.5) * 0.02);
        }
    }

    @Override
    public void explode(@NotNull Level level, @NotNull Vec3 pos, Entity source, float multiplier) {
        float radius = DEFAULT_RADIUS * multiplier;
        DamageSource damageSource = CCBDamageTypes.source(DamageTypes.DROWN, level, source);
        level.explode(source, damageSource, AirtightCannonUtils.createDamageCalculator(radius), pos.x(), pos.y(), pos.z(), radius, false, ExplosionInteraction.TRIGGER, ParticleTypes.GUST_EMITTER_SMALL, ParticleTypes.GUST_EMITTER_LARGE, SoundEvents.WIND_CHARGE_BURST);

        List<LivingEntity> entities = AirtightCannonUtils.getNearbyEntities(level, pos, radius);
        if (entities.isEmpty()) {
            return;
        }

        float damageAmount = DEFAULT_DAMAGE * multiplier;
        for (LivingEntity entity : entities) {
            if (entity.isSensitiveToWater()) {
                entity.hurt(damageSource, damageAmount);
            }
            if (entity.isOnFire()) {
                entity.extinguishFire();
            }
            if (entity instanceof WaterAnimal waterAnimal) {
                waterAnimal.setAirSupply(300);
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation() {
        return CreateCraftedBeginning.asResource("textures/entity/projectiles/moist_wind_charge.png");
    }

    @Override
    public LayerDefinition getLayerDefinition() {
        MeshDefinition definition = new MeshDefinition();
        PartDefinition root = definition.getRoot();
        PartDefinition bone = root.addOrReplaceChild(NAME_BONE, CubeListBuilder.create(), PartPose.offset(0.0f, 0.0f, 0.0f));
        bone.addOrReplaceChild(NAME_WIND_OUTER, CubeListBuilder.create(), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, -Mth.PI / 4, 0.0f));
        bone.addOrReplaceChild(NAME_WIND_INNER, CubeListBuilder.create(), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, -Mth.PI / 4, 0.0f));
        bone.addOrReplaceChild(NAME_CORE, CubeListBuilder.create().texOffs(0, 0).addBox(-2.0f, -2.0f, -2.0f, 4.0f, 4.0f, 4.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, 0.0f, 0.0f));
        return LayerDefinition.create(definition, 64, 32);
    }

    @Override
    public float[] getSetupAnim(float ageInTicks) {
        return new float[]{0, -ageInTicks * ROTATION_SPEED * Mth.PI / 180, 0, 0, 0, 0, 0, 0, 0};
    }

    @Override
    public float getGasConsumptionMultiplier() {
        return 1;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack cannon, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(CCBLang.translate("gui.tooltips.airtight_cannon.moist_air").style(ChatFormatting.DARK_GREEN).component());
    }
}