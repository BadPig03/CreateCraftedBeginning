package net.ty.createcraftedbeginning.api.cannonhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.airtights.airtightcannon.AirtightCannonUtils;
import net.ty.createcraftedbeginning.registry.CCBDamageTypes;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DefaultCannonHandler implements AirtightCannonHandler {
    @Override
    public ItemStack getRenderIcon(Level level) {
        return new ItemStack(CCBItems.NATURAL_WIND_CHARGE.asItem());
    }

    @Override
    public void renderTrailParticles(Level level, Vec3 pos) {
    }

    @Override
    public void explode(Level level, Vec3 pos, Entity source, float multiplier) {
        float radius = 1.2f * multiplier;
        DamageSource damageSource = CCBDamageTypes.source(DamageTypes.WIND_CHARGE, level, source);
        level.explode(source, damageSource, AirtightCannonUtils.createDamageCalculator(radius), pos.x(), pos.y(), pos.z(), radius, false, ExplosionInteraction.TRIGGER, ParticleTypes.GUST_EMITTER_SMALL, ParticleTypes.GUST_EMITTER_LARGE, SoundEvents.WIND_CHARGE_BURST);
    }

    @Override
    public ResourceLocation getTextureLocation() {
        return CreateCraftedBeginning.asResource("textures/entity/projectiles/creative_wind_charge.png");
    }

    @Override
    public LayerDefinition getLayerDefinition() {
        MeshDefinition definition = new MeshDefinition();
        PartDefinition root = definition.getRoot();
        PartDefinition bone = root.addOrReplaceChild("bone", CubeListBuilder.create(), PartPose.offset(0.0f, 0.0f, 0.0f));
        bone.addOrReplaceChild("wind_outer", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, -0.7853982f, 0.0f));
        bone.addOrReplaceChild("wind_inner", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, -0.7853982f, 0.0f));
        bone.addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0f, -2.0f, -2.0f, 4.0f, 4.0f, 4.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, 0.0f, 0.0f));
        return LayerDefinition.create(definition, 64, 32);
    }

    @Override
    public float[] getSetupAnim(float ageInTicks) {
        return new float[]{0, -ageInTicks * 24 * Mth.PI / 180, 0, 0, 0, 0, 0, 0, 0};
    }

    @Override
    public float getGasConsumptionMultiplier() {
        return 1;
    }

    @Override
    public void appendHoverText(ItemStack cannon, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
    }
}
