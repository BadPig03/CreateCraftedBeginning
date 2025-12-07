package net.ty.createcraftedbeginning.api.gas.cannonhandlers.natural;

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
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
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

public class NaturalAirCannonHandler implements AirtightCannonHandler {
    protected static final float DEFAULT_RADIUS = 1.2f;
    protected static final int DEFAULT_DAMAGE = 4;
    protected static final int DEFAULT_DURATION = 200;
    protected static final int ROTATION_SPEED = 16;
    protected static final String NAME_BONE = "bone";
    protected static final String NAME_WIND_OUTER = "wind_outer";
    protected static final String NAME_WIND_INNER = "wind_inner";
    protected static final String NAME_CORE = "core";

    @Override
    public ItemStack getRenderIcon(Level level) {
        return new ItemStack(CCBItems.NATURAL_WIND_CHARGE.asItem());
    }

    @Override
    public void renderTrailParticles(@NotNull Level level, @NotNull Vec3 pos) {
        level.addParticle(CCBParticleTypes.BREEZE_CLOUD.getParticleOptions(), pos.x, pos.y, pos.z, 0, 0, 0);
    }

    @Override
    public void explode(@NotNull Level level, @NotNull Vec3 pos, Entity source, float multiplier) {
        float radius = DEFAULT_RADIUS * multiplier;
        level.explode(source, CCBDamageTypes.source(DamageTypes.WIND_CHARGE, level, source), AirtightCannonUtils.createDamageCalculator(radius), pos.x(), pos.y(), pos.z(), radius, false, ExplosionInteraction.TRIGGER, ParticleTypes.GUST_EMITTER_SMALL, ParticleTypes.GUST_EMITTER_LARGE, SoundEvents.WIND_CHARGE_BURST);
    }

    @Override
    public ResourceLocation getTextureLocation() {
        return CreateCraftedBeginning.asResource("textures/entity/projectiles/natural_wind_charge.png");
    }

    @Override
    public LayerDefinition getLayerDefinition() {
        MeshDefinition definition = new MeshDefinition();
        PartDefinition root = definition.getRoot();
        PartDefinition bone = root.addOrReplaceChild(NAME_BONE, CubeListBuilder.create(), PartPose.offset(0.0f, 0.0f, 0.0f));
        bone.addOrReplaceChild(NAME_WIND_OUTER, CubeListBuilder.create().texOffs(15, 20).addBox(-4.0f, -1.0f, -4.0f, 8.0f, 2.0f, 8.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, -Mth.PI / 4, 0.0f));
        bone.addOrReplaceChild(NAME_WIND_INNER, CubeListBuilder.create().texOffs(0, 9).addBox(-3.0f, -2.0f, -3.0f, 6.0f, 4.0f, 6.0f, new CubeDeformation(0.0f)), PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, -Mth.PI / 4, 0.0f));
        bone.addOrReplaceChild(NAME_CORE, CubeListBuilder.create().texOffs(0, 0).addBox(-2.0f, -2.0f, -2.0f, 4.0f, 4.0f, 4.0f, new CubeDeformation(0.0f)), PartPose.offset(0.0f, 0.0f, 0.0f));
        return LayerDefinition.create(definition, 64, 32);
    }

    @Override
    public float[] getSetupAnim(float ageInTicks) {
        float speed = ageInTicks * ROTATION_SPEED * Mth.PI / 180;
        return new float[]{0, -speed, 0, 0, speed, 0, 0, speed, 0};
    }

    @Override
    public float getGasConsumptionMultiplier() {
        return 1;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack cannon, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(CCBLang.translate("gui.tooltips.airtight_cannon.natural_air").style(ChatFormatting.DARK_GREEN).component());
    }
}