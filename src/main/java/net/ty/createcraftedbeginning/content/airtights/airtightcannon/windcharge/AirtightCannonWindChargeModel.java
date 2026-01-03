package net.ty.createcraftedbeginning.content.airtights.airtightcannon.windcharge;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.projectile.windcharge.AbstractWindCharge;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.gas.cannonhandlers.AirtightCannonHandler;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import org.jetbrains.annotations.NotNull;

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

    public AirtightCannonWindChargeModel(@NotNull ModelPart root) {
        super(RenderType::entityTranslucent);
        bone = root.getChild(NAME_BONE);
        windOuter = bone.getChild(NAME_WIND_OUTER);
        windInner = bone.getChild(NAME_WIND_INNER);
        core = bone.getChild(NAME_CORE);
    }

    @Override
    public void setupAnim(@NotNull AbstractWindCharge entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(entity instanceof AirtightCannonWindChargeProjectileEntity windCharge)) {
            return;
        }

        Gas gas = windCharge.getGasHolder().value();
        AirtightCannonHandler cannonHandler = AirtightCannonHandler.REGISTRY.get(gas);
        if (cannonHandler == null) {
            return;
        }

        float[] anim = cannonHandler.getSetupAnim(ageInTicks);
        if (anim.length != 9) {
            return;
        }

        core.xRot = anim[0];
        core.yRot = anim[1];
        core.zRot = anim[2];
        windOuter.xRot = anim[3];
        windOuter.yRot = anim[4];
        windOuter.zRot = anim[5];
        windInner.xRot = anim[6];
        windInner.yRot = anim[7];
        windInner.zRot = anim[8];
    }

    @Override
    public @NotNull ModelPart root() {
        return bone;
    }
}
