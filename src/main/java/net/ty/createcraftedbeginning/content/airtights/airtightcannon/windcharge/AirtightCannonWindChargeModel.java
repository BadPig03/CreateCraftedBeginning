package net.ty.createcraftedbeginning.content.airtights.airtightcannon.windcharge;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.windcharge.AbstractWindCharge;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.cannonhandlers.visual.CannonAnimationType;
import net.ty.createcraftedbeginning.api.cannonhandlers.visual.CannonModelType;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class AirtightCannonWindChargeModel extends HierarchicalModel<AbstractWindCharge> {
    private static final String NAME_BONE = "bone";
    private static final String NAME_WIND_OUTER = "wind_outer";
    private static final String NAME_WIND_INNER = "wind_inner";
    private static final String NAME_CORE = "core";

    private final ModelPart bone;
    private final ModelPart core;
    private final ModelPart windOuter;
    private final ModelPart windInner;

    public AirtightCannonWindChargeModel(ModelPart root) {
        super(RenderType::entityTranslucent);
        bone = root.getChild(NAME_BONE);
        windOuter = bone.getChild(NAME_WIND_OUTER);
        windInner = bone.getChild(NAME_WIND_INNER);
        core = bone.getChild(NAME_CORE);
    }

    public static LayerDefinition createLayerDefinition(CannonModelType modelType) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition bone = root.addOrReplaceChild(NAME_BONE, CubeListBuilder.create(), PartPose.offset(0, 0, 0));
        switch (modelType) {
            case NATURAL -> {
                bone.addOrReplaceChild(NAME_WIND_OUTER, CubeListBuilder.create().texOffs(15, 20).addBox(-4, -1, -4, 8, 2, 8, new CubeDeformation(0)), PartPose.offsetAndRotation(0, 0, 0, 0, -0.7853982f, 0));
                bone.addOrReplaceChild(NAME_WIND_INNER, CubeListBuilder.create().texOffs(0, 9).addBox(-3, -2, -3, 6, 4, 6, new CubeDeformation(0)), PartPose.offsetAndRotation(0, 0, 0, 0, -0.7853982f, 0));
            }
            case ETHEREAL -> {
                bone.addOrReplaceChild(NAME_WIND_OUTER, CubeListBuilder.create().texOffs(15, 20).addBox(-4, -1, -4, 8, 2, 8, new CubeDeformation(0)), PartPose.offsetAndRotation(0, 0, 0, -0.7853982f, -0.7853982f, 0));
                bone.addOrReplaceChild(NAME_WIND_INNER, CubeListBuilder.create().texOffs(0, 9).addBox(-3, -2, -3, 6, 4, 6, new CubeDeformation(0)), PartPose.offsetAndRotation(0, 0, 0, Mth.PI / 4, -0.7853982f, 0));
            }
            case CORE_ONLY -> {
                bone.addOrReplaceChild(NAME_WIND_OUTER, CubeListBuilder.create(), PartPose.offsetAndRotation(0, 0, 0, 0, -0.7853982f, 0));
                bone.addOrReplaceChild(NAME_WIND_INNER, CubeListBuilder.create(), PartPose.offsetAndRotation(0, 0, 0, 0, -0.7853982f, 0));
            }
        }
        bone.addOrReplaceChild(NAME_CORE, CubeListBuilder.create().texOffs(0, 0).addBox(-2, -2, -2, 4, 4, 4, new CubeDeformation(0)), PartPose.offset(0, 0, 0));
        return LayerDefinition.create(mesh, 64, 32);
    }

    public void setupAnimation(CannonAnimationType animationType, float rotationSpeed, float ageInTicks) {
        resetRotations();
        float rotation = ageInTicks * rotationSpeed * Mth.DEG_TO_RAD;
        switch (animationType) {
            case CORE_Y -> core.yRot = -rotation;
            case NATURAL_Y -> {
                core.yRot = -rotation;
                windOuter.yRot = rotation;
                windInner.yRot = rotation;
            }
            case ETHEREAL_Z -> {
                core.yRot = -rotation;
                windOuter.zRot = rotation;
                windInner.zRot = rotation;
            }
        }
    }

    private void resetRotations() {
        core.xRot = 0;
        core.yRot = 0;
        core.zRot = 0;
        windOuter.xRot = 0;
        windOuter.yRot = 0;
        windOuter.zRot = 0;
        windInner.xRot = 0;
        windInner.yRot = 0;
        windInner.zRot = 0;
    }

    @Override
    public void setupAnim(AbstractWindCharge entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public ModelPart root() {
        return bone;
    }
}
